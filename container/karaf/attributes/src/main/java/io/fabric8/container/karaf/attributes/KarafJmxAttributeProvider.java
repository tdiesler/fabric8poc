/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
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
 */

package io.fabric8.container.karaf.attributes;

import io.fabric8.api.ContainerAttributes;
import io.fabric8.spi.AttributeProvider;
import io.fabric8.spi.Configurer;
import io.fabric8.spi.JmxAttributeProvider;
import io.fabric8.spi.NetworkAttributeProvider;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AbstractAttributeProvider;
import io.fabric8.spi.scr.ValidatingReference;

import java.io.IOException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Cannot use configuration pid=org.apache.karaf.management because it belongs to bundle
// mvn:org.apache.karaf.management/org.apache.karaf.management.server/2.3.3

@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service({ AttributeProvider.class, JmxAttributeProvider.class, ConfigurationListener.class })
@Properties({
        @Property(name = "type", value = ContainerAttributes.TYPE),
        @Property(name = "classifier", value = "jmx")
})
public class KarafJmxAttributeProvider extends AbstractAttributeProvider implements ConfigurationListener, JmxAttributeProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(KarafJmxAttributeProvider.class);
    static final String MANAGEMENT_PID = "org.apache.karaf.management";

    //private static final String JMX_URL_FORMAT = "service:jmx:rmi://${container:%s/fabric8.ip}:%d/jndi/rmi://${container:%s/fabric8.ip}:%d/karaf-%s";
    private static final String JMX_URL_FORMAT = "service:jmx:rmi://%s:%d/jndi/rmi://%s:%d/karaf-root";

    //private static final String JMX_SERVICE_URL = "serviceUrl";
    private static final String RMI_REGISTRY_BINDING_PORT_KEY = "rmiRegistryPort";
    private static final String RMI_SERVER_BINDING_PORT_KEY = "rmiServerPort";
    private static final String RMI_REGISTRY_CONNECTION_PORT_KEY = "rmiRegistryConnectionPort";
    private static final String RMI_SERVER_CONNECTION_PORT_KEY = "rmiServerConnectionPort";

    @Property(name = RMI_REGISTRY_BINDING_PORT_KEY, value = "${" + RMI_REGISTRY_BINDING_PORT_KEY + "}")
    private int rmiRegistryPort = 1099;
    @Property(name = RMI_SERVER_BINDING_PORT_KEY, value = "${" + RMI_SERVER_BINDING_PORT_KEY + "}")
    private int rmiServerPort = 44444;
    @Property(name = RMI_REGISTRY_CONNECTION_PORT_KEY, value = "${" + RMI_REGISTRY_CONNECTION_PORT_KEY + "}")
    private int rmiRegistryConnectionPort = 1099;
    @Property(name = RMI_SERVER_CONNECTION_PORT_KEY, value = "${" + RMI_SERVER_CONNECTION_PORT_KEY + "}")
    private int rmiServerConnectionPort = 44444;
    @Property(name = "runtimeId", value = "${" + RuntimeService.RUNTIME_IDENTITY + "}")
    private String runtimeId;

    @Reference
    private Configurer configurer;

    @Reference(referenceInterface = ConfigurationAdmin.class)
    private ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<>();
    @Reference(referenceInterface = NetworkAttributeProvider.class)
    private final ValidatingReference<NetworkAttributeProvider> networkProvider = new ValidatingReference<>();

    private String ip;
    private String jmxServerUrl;
    private String jmxUsername = "karaf";
    private String jmxPassword = "karaf";

    @Activate
    void activate() throws Exception {
        ip = networkProvider.get().getIp();
        processConfiguration();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    // [TODO] #47 Revisit configurationEvent on JmxAttributeProvider
    // This is an asynchronous callback. How is data integrity preserved?
    // Should this depend the the lifecycle of dependent components?
    public void configurationEvent(ConfigurationEvent event) {
        String pid = event.getPid();
        if (pid.equals(MANAGEMENT_PID) && event.getType() == ConfigurationEvent.CM_UPDATED) {
            processConfiguration();
        }
    }

    @Override
    public String getJmxServerUrl() {
        return jmxServerUrl;
    }

    @Override
    public String getJmxUsername() {
        return jmxUsername;
    }

    @Override
    public String getJmxPassword() {
        return jmxPassword;
    }

    // Configuration pid belongs to another bundle
    private void processConfiguration() {
        try {
            Configuration configuration = configAdmin.get().getConfiguration(MANAGEMENT_PID, null);
            configurer.configure(configuration.getProperties(), this);
            updateAttributes();
        } catch (IOException e) {
            LOGGER.warn("Failed to read configuration update on pid {}.", MANAGEMENT_PID, e);
        } catch (Exception e) {
            LOGGER.warn("Failed to apply configuration of pid {}.", MANAGEMENT_PID, e);
        }
    }

    private void updateAttributes() {
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_JMX_SERVER_URL, getJmxUrl(ip, rmiServerConnectionPort, rmiRegistryPort));
    }

    private String getJmxUrl(String ip, int serverConnectionPort, int registryConnectionPort) {
        return jmxServerUrl = String.format(JMX_URL_FORMAT, ip, serverConnectionPort, ip, registryConnectionPort);
    }

    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }
    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }

    void bindNetworkProvider(NetworkAttributeProvider service) {
        networkProvider.bind(service);
    }
    void unbindNetworkProvider(NetworkAttributeProvider service) {
        networkProvider.unbind(service);
    }
}
