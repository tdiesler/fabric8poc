/*
 * #%L
 * Gravia :: Container :: Tomcat :: Webapp
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
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
package io.fabric8.container.wildfly.webapp;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Activates the {@link Runtime} as part of the web app lifecycle.
 *
 * @author thomas.diesler@jboss.com
 * @since 27-Nov-2013
 */
public class FabricRuntimeActivator implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {

        // [TODO] #41 Invalid class load of embedded interface despite defined dependency
        // https://issues.jboss.org/browse/WFLY-3511
        try {
            ModuleClassLoader classLoader = (ModuleClassLoader) getClass().getClassLoader();
            org.jboss.modules.Module graviaModule = classLoader.getModule().getModuleLoader().loadModule(ModuleIdentifier.create("org.jboss.gravia"));
            org.jboss.modules.Module osgiModule = classLoader.getModule().getModuleLoader().loadModule(ModuleIdentifier.create("org.osgi.enterprise"));
            Class<?> interfClass = loadClass(null, osgiModule.getClassLoader(), "org.osgi.service.http.HttpService");
            Class<?> implClass = loadClass(null, graviaModule.getClassLoader(), "org.apache.felix.http.base.internal.service.HttpServiceImpl");
            if (!interfClass.isAssignableFrom(implClass)) {
                System.out.println("NOT ASSIGNABLE: " + interfClass + " <= " + implClass);
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        ServletContext servletContext = event.getServletContext();

        // HttpService integration
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        Module sysmodule = runtime.getModuleContext().getModule();
        BundleContext bundleContext = sysmodule.adapt(Bundle.class).getBundleContext();
        servletContext.setAttribute(BundleContext.class.getName(), bundleContext);
    }

    private Class<?> loadClass(Class<?> loaderClass, ClassLoader classLoader, String className) throws ClassNotFoundException {
        if (classLoader == null) {
            classLoader = loaderClass.getClassLoader();
        }
        Class<?> clazz = classLoader.loadClass(className);
        System.out.println("LOADED: " + clazz + "\n   using " + loaderClass + " from " + classLoader + "\n   loaded from => " + clazz.getClassLoader());
        return clazz;
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}
