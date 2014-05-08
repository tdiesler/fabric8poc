/*
 * #%L
 * Fabric8 :: Container :: WildFly :: Extension
 * %%
 * Copyright (C) 2014 Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package org.wildfly.extension.fabric.service;

import io.fabric8.api.ServiceLocator;
import io.fabric8.container.karaf.KarafContainerCreateHandler;
import io.fabric8.container.tomcat.TomcatContainerCreateHandler;
import io.fabric8.container.wildfly.WildFlyContainerCreateHandler;
import io.fabric8.spi.BootstrapComplete;
import io.fabric8.spi.ContainerCreateHandler;
import io.fabric8.spi.SystemProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.gravia.provision.Provisioner;
import org.jboss.gravia.repository.Repository;
import org.jboss.gravia.resolver.Resolver;
import org.jboss.gravia.resource.Attachable;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.ServiceReference;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.jboss.gravia.runtime.ServiceTracker;
import org.jboss.gravia.runtime.spi.AbstractModule;
import org.jboss.gravia.runtime.spi.ClassLoaderEntriesProvider;
import org.jboss.gravia.runtime.spi.ManifestHeadersProvider;
import org.jboss.gravia.runtime.spi.ModuleEntriesProvider;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.value.InjectedValue;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.extension.fabric.FabricConstants;
import org.wildfly.extension.gravia.GraviaConstants;
import org.wildfly.extension.gravia.service.ProvisionerService;
import org.wildfly.extension.gravia.service.RepositoryService;
import org.wildfly.extension.gravia.service.ResolverService;

/**
 * Service responsible for creating and managing the life-cycle of the gravia subsystem.
 *
 * @since 19-Apr-2013
 */
public class FabricBootstrapService extends AbstractService<Void> {

    static final Logger LOGGER = LoggerFactory.getLogger(FabricConstants.class.getPackage().getName());

    private final InjectedValue<ModuleContext> injectedModuleContext = new InjectedValue<ModuleContext>();
    private final InjectedValue<Runtime> injectedRuntime = new InjectedValue<Runtime>();

    private final Set<ServiceRegistration<?>> registrations = new HashSet<ServiceRegistration<?>>();
    private Set<ServiceTracker<?, ?>> trackers;
    private Module module;

    public ServiceController<Void> install(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        ServiceBuilder<Void> builder = serviceTarget.addService(FabricConstants.FABRIC_SUBSYSTEM_SERVICE_NAME, this);
        builder.addDependency(GraviaConstants.MODULE_CONTEXT_SERVICE_NAME, ModuleContext.class, injectedModuleContext);
        builder.addDependency(GraviaConstants.RUNTIME_SERVICE_NAME, Runtime.class, injectedRuntime);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        LOGGER.info("Activating Fabric Subsystem");

        // Initialize ConfigurationAdmin content
        Runtime runtime = injectedRuntime.getValue();
        ModuleContext syscontext = runtime.getModuleContext();
        initConfigurationAdmin(runtime);

        // Install and start this as a {@link Module}
        ModuleClassLoader classLoader = (ModuleClassLoader) getClass().getClassLoader();
        try {
            URL extensionURL = null;
            Enumeration<URL> resources = classLoader.getResources(JarFile.MANIFEST_NAME);
            while (resources.hasMoreElements()) {
                URL nextURL = resources.nextElement();
                if (nextURL.getPath().contains("wildfly-extension")) {
                    extensionURL = nextURL;
                    break;
                }
            }
            Manifest manifest = new Manifest(extensionURL.openStream());
            Dictionary<String, String> headers = new ManifestHeadersProvider(manifest).getHeaders();
            module = runtime.installModule(classLoader, headers);

            // Attach the {@link ModuleEntriesProvider} so
            ModuleEntriesProvider entriesProvider = new ClassLoaderEntriesProvider(module);
            Attachable attachable = AbstractModule.assertAbstractModule(module);
            attachable.putAttachment(AbstractModule.MODULE_ENTRIES_PROVIDER_KEY, entriesProvider);

            // Start the module
            module.start();

        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new StartException(ex);
        }

        // Register {@link ContainerCreateHandler} for Karaf, Tomcat, Wildfly
        Set<ContainerCreateHandler> handlers = new HashSet<ContainerCreateHandler>();
        handlers.add(new KarafContainerCreateHandler());
        handlers.add(new TomcatContainerCreateHandler());
        handlers.add(new WildFlyContainerCreateHandler());
        registerContainerCreateHandlers(syscontext, handlers);

        // Open service trackers for {@link Resolver}, {@link Repository}, {@link Provisioner}
        trackers = new HashSet<ServiceTracker<?, ?>>();
        trackers.add(resolverTracker(syscontext, startContext.getChildTarget()));
        trackers.add(repositoryTracker(syscontext, startContext.getChildTarget()));
        trackers.add(provisionerTracker(syscontext, startContext.getChildTarget()));

        // Wait for the {@link BootstrapComplete} to come up
        ServiceLocator.awaitService(BootstrapComplete.class, 10, TimeUnit.SECONDS);

        // FuseFabric banner message
        Properties brandingProperties = new Properties();
        String resname = "/META-INF/branding.properties";
        try {
            URL brandingURL = getClass().getResource(resname);
            brandingProperties.load(brandingURL.openStream());
        } catch (IOException e) {
            throw new StartException("Cannot read branding properties from: " + resname);
        }
        System.out.println(brandingProperties.getProperty("welcome"));
    }

    @Override
    public void stop(StopContext context) {
        // Close the service trackers
        for (ServiceTracker<?, ?> tracker : trackers) {
            tracker.close();
        }
        // Unregister system services
        for (ServiceRegistration<?> sreg : registrations) {
            sreg.unregister();
        }
        // Uninstall the bootstrap module
        if (module != null) {
            module.uninstall();
        }
    }

    private void registerContainerCreateHandlers(ModuleContext context, Set<ContainerCreateHandler> handlers) {
        for (ContainerCreateHandler handler : handlers) {
            String[] classes = new String[] { handler.getClass().getName(), ContainerCreateHandler.class.getName() };
            registrations.add(context.registerService(classes, handler, null));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void initConfigurationAdmin(Runtime runtime) {
        ModuleContext syscontext = runtime.getModuleContext();
        ConfigurationAdmin configAdmin = syscontext.getService(syscontext.getServiceReference(ConfigurationAdmin.class));
        File karafEtc = new File((String) runtime.getProperty(SystemProperties.KARAF_ETC));
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".cfg");
            }
        };
        for (String name : karafEtc.list(filter)) {
            String pid = name.substring(0, name.length() - 4);
            try {
                FileInputStream fis = new FileInputStream(new File(karafEtc, name));
                Properties props = new Properties();
                props.load(fis);
                fis.close();

                Configuration config = configAdmin.getConfiguration(pid, null);
                config.update((Hashtable) props);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private ServiceTracker<?, ?> resolverTracker(final ModuleContext syscontext, final ServiceTarget serviceTarget) {
        ServiceTracker<?, ?> tracker = new ServiceTracker<Resolver, Resolver>(syscontext, Resolver.class, null) {

            ServiceController<Resolver> controller;

            @Override
            public Resolver addingService(ServiceReference<Resolver> reference) {
                Resolver resolver = super.addingService(reference);
                controller = new ResolverService(resolver).install(serviceTarget);
                return resolver;
            }

            @Override
            public void remove(ServiceReference<Resolver> reference) {
                controller.setMode(Mode.REMOVE);
                super.remove(reference);
            }
        };
        tracker.open();
        return tracker;
    }

    private ServiceTracker<?, ?> repositoryTracker(final ModuleContext syscontext, final ServiceTarget serviceTarget) {
        ServiceTracker<?, ?> tracker = new ServiceTracker<Repository, Repository>(syscontext, Repository.class, null) {

            ServiceController<Repository> controller;

            @Override
            public Repository addingService(ServiceReference<Repository> reference) {
                Repository repository = super.addingService(reference);
                controller = new RepositoryService(repository).install(serviceTarget);
                return repository;
            }

            @Override
            public void remove(ServiceReference<Repository> reference) {
                controller.setMode(Mode.REMOVE);
                super.remove(reference);
            }
        };
        tracker.open();
        return tracker;
    }

    private ServiceTracker<?, ?> provisionerTracker(final ModuleContext syscontext, final ServiceTarget serviceTarget) {
        ServiceTracker<?, ?> tracker = new ServiceTracker<Provisioner, Provisioner>(syscontext, Provisioner.class, null) {

            ServiceController<Provisioner> controller;

            @Override
            public Provisioner addingService(ServiceReference<Provisioner> reference) {
                Provisioner provisioner = super.addingService(reference);
                controller = new ProvisionerService(provisioner).install(serviceTarget);
                return provisioner;
            }

            @Override
            public void remove(ServiceReference<Provisioner> reference) {
                controller.setMode(Mode.REMOVE);
                super.remove(reference);
            }
        };
        tracker.open();
        return tracker;
    }
}
