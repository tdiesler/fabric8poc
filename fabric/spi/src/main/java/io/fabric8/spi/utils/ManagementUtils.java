/*
 * #%L
 * Fabric8 :: SPI
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

package io.fabric8.spi.utils;

import io.fabric8.api.Attributable;
import io.fabric8.api.ContainerAttributes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.gravia.utils.MBeanProxy;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * A set of management utils
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Apr-2014
 */
public final class ManagementUtils {

    // Hide ctor
    private ManagementUtils() {
    }

    public static <T> T getMBeanProxy(MBeanServerConnection server, ObjectName oname, Class<T> type) throws IOException {
        T mbeanProxy = null;
        long end = System.currentTimeMillis() + 10000L;
        while (mbeanProxy == null && System.currentTimeMillis() < end) {
            if (server.isRegistered(oname)) {
                mbeanProxy = MBeanProxy.get(server, oname, type);
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
        IllegalStateAssertion.assertNotNull(mbeanProxy, "Cannot obtain MBean proxy for: " + oname);
        return mbeanProxy;
    }

    public static JMXConnector getJMXConnector(Attributable attributes, String username, String password, long timeout, TimeUnit unit) {
        String jmxServiceURL = attributes.getAttribute(ContainerAttributes.ATTRIBUTE_KEY_JMX_SERVER_URL);
        IllegalStateAssertion.assertNotNull(jmxServiceURL, "Cannot obtain container attribute: JMX_SERVER_URL");
        return getJMXConnector(jmxServiceURL, username, password, timeout, unit);
    }

    public static JMXConnector getJMXConnector(String jmxServiceURL, String jmxUsername, String jmxPassword, long timeout, TimeUnit unit) {
        IllegalArgumentAssertion.assertNotNull(jmxServiceURL, "jmxServiceURL");
        Map<String, Object> env = new HashMap<String, Object>();
        if (jmxUsername != null && jmxPassword != null) {
            String[] credentials = new String[] { jmxUsername, jmxPassword };
            env.put(JMXConnector.CREDENTIALS, credentials);
        }
        return getJMXConnector(jmxServiceURL, env, timeout, unit);
    }

    public static JMXConnector getJMXConnector(String jmxServiceURL, Map<String, Object> env, long timeout, TimeUnit unit) {

        JMXServiceURL serviceURL;
        try {
            serviceURL = new JMXServiceURL(jmxServiceURL);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }

        // Find the fabric8-container-wildfly-connector module and use its classloader
        if (RuntimeType.KARAF == RuntimeType.getRuntimeType() && jmxServiceURL.startsWith("service:jmx:http-remoting-jmx")) {
            String classLoaderKey = "jmx.remote.protocol.provider.class.loader";
            if (env.get(classLoaderKey) == null) {
                Runtime runtime = RuntimeLocator.getRequiredRuntime();
                String symbolicName = "fabric8-container-wildfly-connector";
                Set<Module> modules = runtime.getModules(symbolicName, null);
                IllegalStateAssertion.assertFalse(modules.isEmpty(), "Cannot find module: " + symbolicName);
                ClassLoader classLoader = modules.iterator().next().adapt(ClassLoader.class);
                env.put(classLoaderKey, classLoader);
            }
        }

        // Find the org.jboss.remoting-jmx module and use its classloader
        if (RuntimeType.WILDFLY == RuntimeType.getRuntimeType()) {
            String classLoaderKey = "jmx.remote.protocol.provider.class.loader";
            if (env.get(classLoaderKey) == null) {
                ClassLoader classLoader = JmxEnvironmentEnhancer.getClassLoader(env);
                env.put(classLoaderKey, classLoader);
            }
        }

        Exception lastException = null;
        JMXConnector connector = null;
        long end = System.currentTimeMillis() + unit.toMillis(timeout);
        while (connector == null && System.currentTimeMillis() < end) {
            try {
                connector = JMXConnectorFactory.connect(serviceURL, env);
            } catch (Exception ex) {
                lastException = ex;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }
        if (connector == null) {
            throw new IllegalStateException("Cannot obtain JMXConnector for: " + jmxServiceURL, lastException);
        }
        return connector;
    }

    // Confine the usage of jboss-modules to an additional class
    // This prevents NoClassDefFoundError: org/jboss/modules/ModuleLoadException
    static class JmxEnvironmentEnhancer {
        static ClassLoader getClassLoader(Map<String, Object> env) {
            try {
                ModuleIdentifier moduleid = ModuleIdentifier.fromString("org.jboss.remoting-jmx");
                return org.jboss.modules.Module.getBootModuleLoader().loadModule(moduleid).getClassLoader();
            } catch (ModuleLoadException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
