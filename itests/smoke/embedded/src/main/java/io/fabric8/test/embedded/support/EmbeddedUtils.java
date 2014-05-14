/*
 * #%L
 * Fabric8 :: Testsuite :: Smoke :: Embedded
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
package io.fabric8.test.embedded.support;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.jboss.gravia.provision.ResourceHandle;
import org.jboss.gravia.provision.ResourceInstaller;
import org.jboss.gravia.provision.spi.AbstractResourceInstaller;
import org.jboss.gravia.provision.spi.RuntimeEnvironment;
import org.jboss.gravia.resource.Attachable;
import org.jboss.gravia.resource.DefaultResourceBuilder;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceContent;
import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.ModuleException;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.embedded.internal.EmbeddedRuntime;
import org.jboss.gravia.runtime.spi.ClassLoaderEntriesProvider;
import org.jboss.gravia.runtime.spi.DefaultPropertiesProvider;
import org.jboss.gravia.runtime.spi.ManifestHeadersProvider;
import org.jboss.gravia.runtime.spi.ModuleEntriesProvider;
import org.jboss.gravia.runtime.spi.PropertiesProvider;
import org.jboss.gravia.runtime.spi.RuntimeFactory;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.gravia.utils.ManifestUtils;

/**
 * Utility for embedded runtime tests
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Oct-2013
 */
public class EmbeddedUtils {

    public static Runtime getEmbeddedRuntime() {
        Runtime runtime;
        synchronized (RuntimeLocator.class) {
            runtime = RuntimeLocator.getRuntime();
            if (runtime == null) {
                RuntimeFactory factory = new RuntimeFactory() {
                    @Override
                    public Runtime createRuntime(PropertiesProvider propertiesProvider) {
                        return new EmbeddedRuntime(propertiesProvider, null) {
                            @Override
                            protected ModuleEntriesProvider getDefaultEntriesProvider(Module module, Attachable context) {
                                return new ClassLoaderEntriesProvider(module);
                            }
                        };
                    }
                };

                runtime = RuntimeLocator.createRuntime(factory, new DefaultPropertiesProvider());
                runtime.init();

                ModuleContext syscontext = runtime.getModuleContext();
                RuntimeEnvironment environment = new RuntimeEnvironment(runtime);
                EmbeddedResourceInstaller resourceInstaller = new EmbeddedResourceInstaller(environment);
                syscontext.registerService(RuntimeEnvironment.class, environment, null);
                syscontext.registerService(ResourceInstaller.class, resourceInstaller, null);
            }
        }
        return runtime;
    }

    public static Module installAndStartModule(ClassLoader classLoader, String symbolicName) throws ModuleException, IOException {
        File modfile = getModuleFile(symbolicName);
        if (modfile.isFile()) {
            return installAndStartModule(classLoader, modfile);
        } else {
            return installAndStartModule(classLoader, symbolicName, null);
        }
    }

    public static Module installAndStartModule(ClassLoader classLoader, File location) throws ModuleException, IOException {
        return installAndStartModule(classLoader, location.toURI().toURL());
    }

    public static Module installAndStartModule(ClassLoader classLoader, URL location) throws ModuleException, IOException {
        JarInputStream input = new JarInputStream(location.openStream());
        try {
            Manifest manifest = input.getManifest();
            Dictionary<String, String> headers = new ManifestHeadersProvider(manifest).getHeaders();
            return installAndStartModule(classLoader, null, headers);
        } finally {
            input.close();
        }
    }

    public static Module installAndStartModule(ClassLoader classLoader, String symbolicName, String version) throws ModuleException {
        ResourceIdentity.create(symbolicName, version);
        Resource resource = new DefaultResourceBuilder().addIdentityCapability(symbolicName, version).getResource();
        return installAndStartModule(classLoader, resource);
    }

    public static Module installAndStartModule(ClassLoader classLoader, ResourceIdentity identity) throws ModuleException {
        Resource resource = new DefaultResourceBuilder().addIdentityCapability(identity).getResource();
        return installAndStartModule(classLoader, resource);
    }

    public static Module installAndStartModule(ClassLoader classLoader, Resource resource) throws ModuleException {
        return installAndStartModule(classLoader, resource, null);
    }

    public static Module installAndStartModule(ClassLoader classLoader, Resource resource, Dictionary<String, String> headers) throws ModuleException {
        Module module = getEmbeddedRuntime().installModule(classLoader, resource, headers);
        module.start();
        return module;
    }

    private static File getModuleFile(String modname) {
        return new File("target/modules/" + modname + ".jar").getAbsoluteFile();
    }

    static final class EmbeddedResourceInstaller extends AbstractResourceInstaller {

        private final RuntimeEnvironment environment;

        EmbeddedResourceInstaller(RuntimeEnvironment environment) {
            this.environment = environment;
        }

        @Override
        public RuntimeEnvironment getEnvironment() {
            return environment;
        }

        @Override
        public ResourceHandle installResourceProtected(Context context, final Resource resource, boolean shared, String runtimeName) throws Exception {

            ResourceContent content = resource.adapt(ResourceContent.class);
            IllegalStateAssertion.assertNotNull(content, "Resource has no content: " + resource);

            Manifest manifest = ManifestUtils.getManifest(content.getContent());
            IllegalStateAssertion.assertNotNull(manifest, "Resource has no manifest: " + resource);
            Dictionary<String, String> headers = ManifestUtils.getManifestHeaders(manifest);

            // Install the module
            Runtime runtime = environment.getRuntime();
            ClassLoader classLoader = EmbeddedRuntime.class.getClassLoader();
            final Module module = runtime.installModule(classLoader, resource, headers);

            // Autostart the module
            module.start();

            return new ResourceHandle() {

                @Override
                public Resource getResource() {
                    return resource;
                }

                @Override
                public Module getModule() {
                    return module;
                }

                @Override
                public void uninstall() {
                    module.uninstall();
                }
            };
        }
    }
}
