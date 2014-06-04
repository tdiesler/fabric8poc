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

package io.fabric8.karaf.attributes;


import io.fabric8.spi.AttributeProvider;
import io.fabric8.api.ContainerAttributes;
import io.fabric8.spi.Configurer;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AttributeProviderComponent;
import io.fabric8.spi.scr.ValidatingReference;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component(immediate = true)
@Service({AttributeProvider.class, ConfigurationListener.class})
@Properties(
        @Property(name = "type", value = ContainerAttributes.TYPE)
)
public class JmxAttributeProvider extends AttributeProviderComponent implements ConfigurationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmxAttributeProvider.class);
    static final String MANAGEMENT_PID = "org.apache.karaf.management";

    private static final String JMX_URL_FORMAT = "service:jmx:rmi://${container:%s/fabric8.ip}:%d/jndi/rmi://${container:%s/fabric8.ip}:%d/karaf-%s";

    private static final String JMX_SERVICE_URL = "serviceUrl";
    private static final String RMI_REGISTRY_BINDING_PORT_KEY = "rmiRegistryPort";
    private static final String RMI_SERVER_BINDING_PORT_KEY = "rmiServerPort";
    private static final String RMI_REGISTRY_CONNECTION_PORT_KEY = "rmiRegistryConnectionPort";
    private static final String RMI_SERVER_CONNECTION_PORT_KEY = "rmiServerConnectionPort";

    @Property(name = RMI_REGISTRY_BINDING_PORT_KEY, value = "${"+RMI_REGISTRY_BINDING_PORT_KEY+"}")
    private int rmiRegistryPort = 1099;
    @Property(name = RMI_SERVER_BINDING_PORT_KEY, value = "${"+RMI_SERVER_BINDING_PORT_KEY+"}")
    private int rmiServerPort = 44444;
    @Property(name = RMI_REGISTRY_CONNECTION_PORT_KEY, value = "${"+RMI_REGISTRY_CONNECTION_PORT_KEY+"}")
    private int rmiRegistryConnectionPort = 1099;
    @Property(name = RMI_SERVER_CONNECTION_PORT_KEY, value = "${"+RMI_SERVER_CONNECTION_PORT_KEY+"}")
    private int rmiServerConnectionPort = 44444;
    @Property(name ="runtimeId", value = "${"+RuntimeService.RUNTIME_IDENTITY+"}")
    private String runtimeId;

    @Reference(referenceInterface = Configurer.class)
    private ValidatingReference<Configurer> configurer = new ValidatingReference<>();

    @Reference(referenceInterface = ConfigurationAdmin.class)
    private ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<>();


    @Activate
    void activate() throws Exception {
        processConfiguration();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
        String pid = event.getPid();
        if (pid.equals(MANAGEMENT_PID) && event.getType() == ConfigurationEvent.CM_UPDATED) {
            processConfiguration();
        }
    }

    /**
     * Reads configuration and updates attrivutes
     */
    private void processConfiguration() {
        try {
            Configuration configuration = configAdmin.get().getConfiguration(MANAGEMENT_PID);
            Map<String, Object> map = new HashMap<>();
            configurer.get().configure(configuration.getProperties(), this);
            updateAttributes();
        } catch (IOException e) {
            LOGGER.warn("Failed to read configuration update on pid {}.", MANAGEMENT_PID, e);
        } catch (Exception e) {
            LOGGER.warn("Failed to apply configuration of pid {}.", MANAGEMENT_PID, e);
        }
    }

    private void updateAttributes() {
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_JMX_SERVER_URL, getJmxUrl(runtimeId, rmiServerConnectionPort, rmiRegistryPort));
    }

    private String getJmxUrl(String name, int serverConnectionPort, int registryConnectionPort)  {
        return String.format(JMX_URL_FORMAT, name, serverConnectionPort, name, registryConnectionPort, name);
    }

    void bindConfigurer(Configurer service) {
        this.configurer.bind(service);
    }
    void unbindConfigurer(Configurer service) {
        this.configurer.unbind(service);
    }

    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }
    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }
}
