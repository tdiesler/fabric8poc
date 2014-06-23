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

import static io.fabric8.spi.RuntimeService.RUNTIME_CONF_DIR;
import static io.fabric8.spi.RuntimeService.RUNTIME_DATA_DIR;
import static io.fabric8.spi.RuntimeService.RUNTIME_HOME_DIR;
import static io.fabric8.spi.RuntimeService.RUNTIME_IDENTITY;
import static org.jboss.gravia.runtime.spi.RuntimeLogger.LOGGER;
import io.fabric8.api.ContainerAttributes;
import io.fabric8.api.FabricException;
import io.fabric8.spi.AbstractJMXAttributeProvider;
import io.fabric8.spi.AttributeProvider;
import io.fabric8.spi.AttributeSupport;
import io.fabric8.spi.BootstrapComplete;
import io.fabric8.spi.HttpAttributeProvider;
import io.fabric8.spi.JMXAttributeProvider;
import io.fabric8.spi.NetworkAttributeProvider;
import io.fabric8.spi.utils.FileUtils;
import io.fabric8.spi.utils.HostUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.arquillian.core.spi.context.ObjectStore;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.jboss.gravia.arquillian.container.embedded.EmbeddedRuntimeSetup;
import org.jboss.gravia.arquillian.container.embedded.EmbeddedSetupObserver;
import org.jboss.gravia.arquillian.container.embedded.EmbeddedUtils;
import org.jboss.gravia.provision.spi.RuntimeEnvironment;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.gravia.utils.IOUtils;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * A plugin point that is called from the {@link EmbeddedSetupObserver}
 * as part of the {@link BeforeSuite} processing
 *
 * @author thomas.diesler@jboss.com
 * @since 19-Jun-2014
 */
public class EmbeddedContainerSetup implements EmbeddedRuntimeSetup {

    private static String[] moduleNames = new String[] { "fabric8-api", "fabric8-spi", "fabric8-core", "fabric8-domain-agent", "fabric8-container-karaf-managed",
            "fabric8-container-tomcat-managed", "fabric8-container-wildfly-managed" };

    @Override
    public void setupEmbeddedRuntime(ObjectStore suiteStore) throws Exception {

        Path basedir = Paths.get("").toAbsolutePath();
        Path homePath = basedir.resolve(Paths.get("target", "home"));

        System.setProperty("basedir", basedir.toString());
        System.setProperty(RUNTIME_IDENTITY, "embedded");
        System.setProperty(RUNTIME_HOME_DIR, homePath.toString());
        System.setProperty(RUNTIME_DATA_DIR, homePath.resolve("data").toString());
        System.setProperty(RUNTIME_CONF_DIR, homePath.resolve("conf").toString());

        // Delete the container's home directory - every test case starts fresh
        FileUtils.deleteRecursively(homePath);

        // Install and start additional modules
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();
        for (URL url : getInitialModuleLocations()) {
            ClassLoader classLoader = EmbeddedUtils.class.getClassLoader();
            EmbeddedUtils.installAndStartModule(classLoader, url);
        }

        // Create the JMXConnectorServer
        String jmxServerUrl;
        try {
            JMXConnectorServer connectorServer = createJMXConnectorServer();
            connectorServer.start();
            jmxServerUrl = connectorServer.getAddress().toString();
            LOGGER.info("JMX server URL: " + jmxServerUrl);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        // Add initial runtime resources
        String resname = "environment.xml";
        URL resurl = EmbeddedUtils.class.getClassLoader().getResource(resname);
        if (resurl != null) {
            RuntimeEnvironment environment = ServiceLocator.getRequiredService(RuntimeEnvironment.class);
            InputStream input = null;
            try {
                input = resurl.openStream();
                environment.initDefaultContent(input);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            } finally {
                IOUtils.safeClose(input);
            }
        }

        // Register the JMXAttributeProvider
        String[] services = new String[] { AttributeProvider.class.getName(), JMXAttributeProvider.class.getName() };
        syscontext.registerService(services, new EmbeddedJmxAttributeProvider(jmxServerUrl), null);

        // Register the NetworkAttributeProvider
        services = new String[] { AttributeProvider.class.getName(), NetworkAttributeProvider.class.getName() };
        syscontext.registerService(services, new EmbeddedNetworkAttributeProvider(), null);

        // Register the HttpAttributeProvider
        services = new String[] { AttributeProvider.class.getName(), HttpAttributeProvider.class.getName() };
        syscontext.registerService(services, new EmbeddedHttpAttributeProvider(), null);

        // Wait for the {@link BootstrapComplete} service
        ServiceLocator.awaitService(BootstrapComplete.class, 20, TimeUnit.SECONDS);
    }

    private List<URL> getInitialModuleLocations() throws IOException {
        List<URL> urls = new ArrayList<>();
        for (String modname : moduleNames) {
            File modfile = Paths.get("target", "modules", modname + ".jar").toFile();
            urls.add(modfile.toURI().toURL());
        }
        return urls;
    }

    private JMXConnectorServer createJMXConnectorServer() throws IOException {
        ServerSocket server = new ServerSocket(0);
        int port = server.getLocalPort();
        server.close();
        LocateRegistry.createRegistry(port);
        JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:rmi://localhost:" + port + "/jndi/rmi://localhost:" + port + "/jmxrmi");
        MBeanServer mbeanServer = ServiceLocator.getRequiredService(MBeanServer.class);
        return JMXConnectorServerFactory.newJMXConnectorServer(serviceURL, null, mbeanServer);
    }

    static final class EmbeddedJmxAttributeProvider extends AbstractJMXAttributeProvider {
        EmbeddedJmxAttributeProvider(String jmxServerUrl) {
            super(jmxServerUrl, null, null);
        }
    }

    static final class EmbeddedNetworkAttributeProvider extends AttributeSupport implements NetworkAttributeProvider {
        EmbeddedNetworkAttributeProvider() {
            addAttribute(ContainerAttributes.ATTRIBUTE_KEY_HOSTNAME, getLocalHostName());
            addAttribute(ContainerAttributes.ATTRIBUTE_KEY_LOCAL_IP, getLocalIp());
            addAttribute(ContainerAttributes.ATTRIBUTE_KEY_BIND_ADDRESS, "0.0.0.0");
            addAttribute(ContainerAttributes.ATTRIBUTE_ADDRESS_RESOLVER, ContainerAttributes.ATTRIBUTE_KEY_LOCAL_IP.getName());
        }

        @Override
        public String getIp() {
            return getLocalIp();
        }

        @Override
        public String getLocalIp() {
            try {
                return HostUtils.getLocalIp();
            } catch (UnknownHostException e) {
                throw FabricException.launderThrowable(e);
            }
        }

        @Override
        public String getLocalHostName() {
            try {
                return HostUtils.getLocalHostName();
            } catch (UnknownHostException e) {
                throw FabricException.launderThrowable(e);
            }
        }
    }

    static final class EmbeddedHttpAttributeProvider extends AttributeSupport implements HttpAttributeProvider {

        private int httpPort;
        private String httpUrl;

        EmbeddedHttpAttributeProvider() {
            initAttributes();
            addAttribute(ContainerAttributes.ATTRIBUTE_KEY_HTTP_PORT, httpPort);
            addAttribute(ContainerAttributes.ATTRIBUTE_KEY_HTTP_URL, httpUrl);
        }

        @Override
        public String getHttpUrl() {
            return httpUrl;
        }

        @Override
        public String getHttpsUrl() {
            throw new UnsupportedOperationException();
        }

        private void initAttributes() {
            NetworkAttributeProvider networkProvider = ServiceLocator.getRequiredService(NetworkAttributeProvider.class);
            ConfigurationAdmin configAdmin = ServiceLocator.getRequiredService(ConfigurationAdmin.class);
            try {
                Configuration configuration = configAdmin.getConfiguration("org.apache.felix.http", null);
                Dictionary<String, Object> props = configuration.getProperties();
                httpPort = Integer.parseInt((String) props.get(HttpAttributeProvider.HTTP_BINDING_PORT));
                httpUrl = "http://" + networkProvider.getLocalIp() + ":" + httpPort;
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}