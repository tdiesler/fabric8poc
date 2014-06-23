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

package io.fabric8.container.tomcat.attributes;

import io.fabric8.api.ContainerAttributes;
import io.fabric8.spi.AttributeProvider;
import io.fabric8.spi.HttpAttributeProvider;
import io.fabric8.spi.NetworkAttributeProvider;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AbstractAttributeProvider;
import io.fabric8.spi.scr.ValidatingReference;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.catalina.Server;
import org.apache.catalina.connector.Connector;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service({ AttributeProvider.class, HttpAttributeProvider.class })
public class TomcatHttpAttributeProvider extends AbstractAttributeProvider implements HttpAttributeProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatHttpAttributeProvider.class);

    private static final int DEFAULT_HTTP_REMOTE_PORT = 8080;
    private static final int DEFAULT_HTTPS_REMOTE_PORT = 8443;

    private static final String HTTP_URL_FORMAT = "http://%s:%d";
    private static final String HTTPS_URL_FORMAT = "https://%s:%d";

    @Reference(referenceInterface = MBeanServer.class)
    private final ValidatingReference<MBeanServer> mbeanServer = new ValidatingReference<MBeanServer>();
    @Reference(referenceInterface = NetworkAttributeProvider.class)
    private final ValidatingReference<NetworkAttributeProvider> networkProvider = new ValidatingReference<>();
    @Reference(referenceInterface = RuntimeService.class)
    private final ValidatingReference<RuntimeService> runtimeService = new ValidatingReference<>();

    private final Set<Connector> httpConnectors = new LinkedHashSet<Connector>();
    private final Set<Connector> httpsConnectors = new LinkedHashSet<Connector>();
    private String ip;
    private String httpUrl;
    private String httpsUrl;

    @Activate
    void activate() throws Exception {
        ip = networkProvider.get().getIp();
        activateInternal();
        updateAttributes();
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

    private void activateInternal() throws Exception {
        Server server = getServer();
        org.apache.catalina.Service[] services = server.findServices();
        for (org.apache.catalina.Service service : services) {
            for (Connector connector : service.findConnectors()) {
                if (connector.getScheme().equals("http")) {
                    httpConnectors.add(connector);
                } else if (connector.getScheme().equals("https")) {
                    httpsConnectors.add(connector);
                }
            }
        }
    }

    private void updateAttributes() {
        try {
            boolean httpEnabled = isHttpEnabled();
            boolean httpsEnabled = isHttpsEnabled();
            int httpPort = httpsEnabled && !httpEnabled ? getHttpsPort() : getHttpPort();
            int httpsPort = httpsEnabled ? getHttpsPort() : 0;
            putAttribute(ContainerAttributes.ATTRIBUTE_KEY_HTTP_URL, getHttpUrl(ip, httpPort));
            putAttribute(ContainerAttributes.ATTRIBUTE_KEY_HTTPS_URL, getHttpsUrl(ip, httpsPort));
        } catch (Exception e) {
            LOGGER.warn("Failed to get http attributes.", e);
        }
    }

    private Server getServer() throws Exception {
        ObjectName name = new ObjectName("Catalina", "type", "Server");
        return (Server) mbeanServer.get().getAttribute(name, "managedResource");
    }

    private boolean isHttpEnabled() throws IOException {
        return !httpConnectors.isEmpty();
    }

    private boolean isHttpsEnabled() throws IOException {
        return !httpsConnectors.isEmpty();
    }

    private int getHttpPort() {
        int port = DEFAULT_HTTP_REMOTE_PORT;
        for (Connector connector : httpConnectors) {
            return connector.getPort();
        }
        return port;
    }

    private int getHttpsPort() throws InterruptedException, IOException {
        int port = DEFAULT_HTTPS_REMOTE_PORT;
        for (Connector connector : httpsConnectors) {
            return connector.getPort();
        }
        return port;
    }

    private String getHttpUrl(String host, int port) {
        return httpUrl = String.format(HTTP_URL_FORMAT, host, port);
    }

    private String getHttpsUrl(String host, int port) {
        return httpsUrl = String.format(HTTPS_URL_FORMAT, host, port);
    }

    void bindMbeanServer(MBeanServer service) {
        this.mbeanServer.bind(service);
    }

    void unbindMbeanServer(MBeanServer service) {
        this.mbeanServer.unbind(service);
    }

    void bindNetworkProvider(NetworkAttributeProvider service) {
        networkProvider.bind(service);
    }

    void unbindNetworkProvider(NetworkAttributeProvider service) {
        networkProvider.unbind(service);
    }

    void bindRuntimeService(RuntimeService service) {
        runtimeService.bind(service);
    }

    void unbindRuntimeService(RuntimeService service) {
        runtimeService.unbind(service);
    }
}
