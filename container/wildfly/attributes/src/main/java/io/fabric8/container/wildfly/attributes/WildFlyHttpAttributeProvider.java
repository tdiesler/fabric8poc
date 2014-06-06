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

package io.fabric8.container.wildfly.attributes;


import io.fabric8.api.ContainerAttributes;
import io.fabric8.spi.AttributeProvider;
import io.fabric8.spi.HttpAttributeProvider;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AbstractAttributeProvider;
import io.fabric8.spi.scr.ValidatingReference;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service({AttributeProvider.class, HttpAttributeProvider.class})
@Properties(
        @Property(name = "type", value = ContainerAttributes.TYPE)
)
public class WildFlyHttpAttributeProvider extends AbstractAttributeProvider implements HttpAttributeProvider  {

    private static final String HTTP_PORT = "jboss.management.http.port";
    private static final String HTTPS_PORT = "jboss.management.https.port";
    private static final int DEFAULT_HTTP_PORT = 8080;
    private static final int DEFAULT_HTTPS_PORT = 8443;

    private static final String HTTP_URL_FORMAT = "http://${container:%s/fabric8.ip}:%d";
    private static final String HTTPS_URL_FORMAT = "https://${container:%s/fabric8.ip}:%d";

    @Reference(referenceInterface = RuntimeService.class)
    private final ValidatingReference<RuntimeService> runtimeService = new ValidatingReference<>();

    private int httpPort;
    private int httpsPort;
    private String httpUrl;
    private String httpsUrl;
    private String runtimeId;

    @Activate
    void activate() throws Exception {
        runtimeId = runtimeService.get().getRuntimeIdentity();;
        httpPort = Integer.parseInt(runtimeService.get().getProperty(HTTP_PORT, "" + DEFAULT_HTTP_PORT));
        httpsPort = Integer.parseInt(runtimeService.get().getProperty(HTTPS_PORT, "" + DEFAULT_HTTPS_PORT));
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

    private void updateAttributes() {
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_HTTP_PORT, httpPort);
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_HTTPS_PORT, httpsPort);
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_HTTP_URL, getHttpUrl(runtimeId, httpPort));
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_HTTPS_URL, getHttpsUrl(runtimeId, httpsPort));
    }

    private String getHttpUrl(String name, int port)  {
        return httpUrl = String.format(HTTP_URL_FORMAT, name, port);
    }

    private String getHttpsUrl(String name, int port)  {
        return httpsUrl = String.format(HTTPS_URL_FORMAT, name, port);
    }

    void bindRuntimeService(RuntimeService service) {
        runtimeService.bind(service);
    }
    void unbindRuntimeService(RuntimeService service) {
        runtimeService.unbind(service);
    }
}
