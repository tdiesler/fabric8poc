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

package io.fabric8.wildfly.attributes;


import io.fabric8.spi.AttributeProvider;
import io.fabric8.api.ContainerAttributes;
import io.fabric8.spi.Configurer;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AttributeProviderComponent;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component(immediate = true)
@Service(AttributeProvider.class)
@Properties(
        @Property(name = "type", value = ContainerAttributes.TYPE)
)
public class HttpAttributeProvider extends AttributeProviderComponent  {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpAttributeProvider.class);
    private static final String HTTP_PORT = "jboss.management.http.port";
    private static final String HTTPS_PORT = "jboss.management.https.port";
    private static final int DEFAULT_HTTP_PORT = 8080;
    private static final int DEFAULT_HTTPS_PORT = 8443;

    private static final String HTTP_URL_FORMAT = "http://${container:%s/fabric8.ip}:%d";
    private static final String HTTPS_URL_FORMAT = "https://${container:%s/fabric8.ip}:%d";

    @Property(name = HTTP_PORT, value = "${" + HTTP_PORT + "}")
    int httpPort = DEFAULT_HTTP_PORT;
    @Property(name = HTTPS_PORT, value = "${" + HTTPS_PORT + "}")
    int httpsPort = DEFAULT_HTTPS_PORT;
    @Property(name ="runtimeId", value = "${"+RuntimeService.RUNTIME_IDENTITY+"}")
    private String runtimeId;
    @Reference
    private Configurer configurer;

    @Activate
    void activate(Map<String, Object> configuration) throws Exception {
        configurer.configure(configuration, this);
        httpPort = httpPort > 0 ? httpPort : DEFAULT_HTTP_PORT;
        httpsPort = httpsPort > 0 ? httpsPort : DEFAULT_HTTPS_PORT;
        updateAttributes();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    private void updateAttributes() {
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_HTTP_PORT, httpPort);
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_HTTPS_PORT, httpsPort);
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_HTTP_URL, getHttpUrl(runtimeId, httpPort));
    }

    private String getHttpUrl(String name, int port)  {
        return String.format(HTTP_URL_FORMAT, name, port);
    }
}
