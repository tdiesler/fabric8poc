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
import io.fabric8.spi.HttpAttributeProvider;
import io.fabric8.spi.NetworkAttributeProvider;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AbstractAttributeProvider;
import io.fabric8.spi.scr.ValidatingReference;

import java.io.IOException;
import java.util.Dictionary;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Cannot use configuration pid=org.apache.felix.http for bundle
// mvn:io.fabric8.poc/fabric8-container-karaf-attributes/2.0.0-SNAPSHOT because it belongs to bundle mvn:org.apache.felix/org.apache.felix.http.bundle/2.2.1

@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service({AttributeProvider.class, HttpAttributeProvider.class})
@Properties({
                @Property(name = "type", value = ContainerAttributes.TYPE),
                @Property(name = "classifier", value = "http")
})
public class KarafHttpAttributeProvider extends AbstractAttributeProvider implements HttpAttributeProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(KarafHttpAttributeProvider.class);
    static final String APACHE_FELIX_HTTP_PID = "org.apache.felix.http";

    private static final String HTTP_CONNECTION_PORT_KEY = "io.fabric8.http.connection.port";
    private static final String HTTPS_CONNECTION_PORT_KEY = "io.fabric8.http.connection.port.secure";

    private static final String HTTP_ENABLED = "org.osgi.service.http.enabled";
    private static final String HTTPS_ENABLED = "org.osgi.service.http.secure.enabled";
    private static final String HTTP_URL_FORMAT = "http://%s:%d";
    private static final String HTTPS_URL_FORMAT = "https://%s:%d";

    @Property(name = HTTP_BINDING_PORT, value = "${" + HTTP_BINDING_PORT + "}")
    private int httpPort = 8080;
    @Property(name = HTTPS_BINDING_PORT, value = "${" + HTTPS_BINDING_PORT + "}")
    private int httpPortSecure = 8443;
    @Property(name = HTTP_CONNECTION_PORT_KEY, value = "${" + HTTP_CONNECTION_PORT_KEY + "}")
    private int httpConnectionPort = 0;
    @Property(name = HTTPS_CONNECTION_PORT_KEY, value = "${" + HTTPS_CONNECTION_PORT_KEY + "}")
    private int httpConnectionPortSecure = 0;
    @Property(name = HTTP_ENABLED, value = "${" + HTTP_ENABLED + "}")
    private boolean httpEnabled = true;
    @Property(name = HTTPS_ENABLED, value = "${" + HTTPS_ENABLED + "}")
    private boolean httpSecureEnabled = false;
    @Property(name = "runtimeId", value = "${" + RuntimeService.RUNTIME_IDENTITY + "}")
    private String runtimeId;

    @Reference
    private Configurer configurer;

    @Reference(referenceInterface = ConfigurationAdmin.class)
    private ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<>();
    @Reference(referenceInterface = NetworkAttributeProvider.class)
    private final ValidatingReference<NetworkAttributeProvider> networkProvider = new ValidatingReference<>();

    private String ip;
    private String httpUrl;
    private String httpsUrl;

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
    public String getHttpsUrl() {
        return httpsUrl;
    }

    @Override
    public String getHttpUrl() {
        return httpUrl;
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

    // Configuration pid belongs to another bundle
    private void processConfiguration() {
        try {
            Configuration configuration = configAdmin.get().getConfiguration(APACHE_FELIX_HTTP_PID);
            Dictionary<String, Object> properties = configuration.getProperties();
            configurer.configure(properties, this, "org.osgi.service", "io,fabric8");
            httpConnectionPort = httpConnectionPort != 0 ? httpConnectionPort : httpPort;
            httpConnectionPortSecure = httpConnectionPortSecure != 0 ? httpConnectionPortSecure : httpPortSecure;
            updateAttributes();
        } catch (IOException e) {
            LOGGER.warn("Failed to read configuration update on pid {}.", APACHE_FELIX_HTTP_PID, e);
        } catch (Exception e) {
            LOGGER.warn("Failed to apply configuration of pid {}.", APACHE_FELIX_HTTP_PID, e);
        }
    }

    private void updateAttributes() throws Exception {
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_HTTP_PORT, httpConnectionPort != 0 ? httpConnectionPort : httpPort);
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_HTTPS_PORT, httpConnectionPortSecure != 0 ? httpConnectionPortSecure : httpPortSecure);
        int httpPort = httpSecureEnabled && !httpEnabled ? httpConnectionPortSecure : httpConnectionPort;
        int httpsPort = httpSecureEnabled ? httpConnectionPortSecure : 0;
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_HTTP_URL, getHttpUrl(ip, httpPort));
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_HTTPS_URL, getHttpsUrl(ip, httpsPort));
    }

    private String getHttpUrl(String host, int port) {
        return httpUrl = String.format(HTTP_URL_FORMAT, host, port);
    }

    private String getHttpsUrl(String host, int port) {
        return httpsUrl = String.format(HTTPS_URL_FORMAT, host, port);
    }
}
