/*
 * #%L
 * Fabric8 :: Container :: Tomcat :: WebApp
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
package io.fabric8.container.tomcat.webapp;

import io.fabric8.spi.BootstrapComplete;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jboss.gravia.container.tomcat.support.TomcatResourceInstaller;
import org.jboss.gravia.container.tomcat.support.TomcatRuntimeFactory;
import org.jboss.gravia.provision.ResourceInstaller;
import org.jboss.gravia.provision.spi.RuntimeEnvironment;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.ModuleException;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceEvent;
import org.jboss.gravia.runtime.ServiceListener;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.jboss.gravia.runtime.WebAppContextListener;
import org.jboss.gravia.runtime.spi.PropertiesProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Activates the {@link Runtime} as part of the web app lifecycle.
 */
public class FabricTomcatActivator implements ServletContextListener {

    private final Set<ServiceRegistration<?>> registrations = new HashSet<ServiceRegistration<?>>();

    @Override
    public void contextInitialized(ServletContextEvent event) {

        // Create the runtime
        ServletContext servletContext = event.getServletContext();
        PropertiesProvider propsProvider = new FabricPropertiesProvider(servletContext);
        Runtime runtime = RuntimeLocator.createRuntime(new TomcatRuntimeFactory(servletContext), propsProvider);
        runtime.init();

        // Start listening on the {@link BootstrapComplete}
        final ModuleContext syscontext = runtime.getModuleContext();
        final BoostrapLatch latch = new BoostrapLatch(1);
        ServiceListener listener = new ServiceListener() {
            @Override
            public void serviceChanged(ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    syscontext.removeServiceListener(this);
                    latch.countDown();
                }
            }
        };
        servletContext.setAttribute(BoostrapLatch.class.getName(), latch);
        syscontext.addServiceListener(listener, "(objectClass=" + BootstrapComplete.class.getName() + ")");

        // Register the {@link RuntimeEnvironment}, {@link ResourceInstaller} services
        registerServices(servletContext, runtime);

        // Install and start this webapp as a module
        WebAppContextListener webappInstaller = new WebAppContextListener();
        Module module = webappInstaller.installWebappModule(servletContext);
        servletContext.setAttribute(Module.class.getName(), module);
        try {
            module.start();
        } catch (ModuleException ex) {
            throw new IllegalStateException(ex);
        }

        // HttpService integration
        Module sysmodule = runtime.getModuleContext().getModule();
        BundleContext bundleContext = sysmodule.adapt(Bundle.class).getBundleContext();
        servletContext.setAttribute(BundleContext.class.getName(), bundleContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        for (ServiceRegistration<?> sreg : registrations) {
            sreg.unregister();
        }
    }

    private void registerServices(ServletContext servletContext, Runtime runtime) {
        RuntimeEnvironment environment = new RuntimeEnvironment(runtime).initDefaultContent();
        TomcatResourceInstaller installer = new TomcatResourceInstaller(environment);
        ModuleContext syscontext = runtime.getModuleContext();
        registrations.add(syscontext.registerService(RuntimeEnvironment.class, environment, null));
        registrations.add(syscontext.registerService(ResourceInstaller.class, installer, null));
    }

    static class BoostrapLatch extends CountDownLatch {
        BoostrapLatch(int count) {
            super(count);
        }
    }
}
