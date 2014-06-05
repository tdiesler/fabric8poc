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

package io.fabric8.core;

import io.fabric8.spi.AttributeProvider;
import io.fabric8.api.ContainerAttributes;
import io.fabric8.spi.Configurer;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AbstractAttributeProvider;
import io.fabric8.spi.utils.HostUtils;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import java.net.UnknownHostException;
import java.util.Map;

@Component(policy = ConfigurationPolicy.REQUIRE, immediate = true)
@Service(AttributeProvider.class)
@Properties(@Property(name = "type", value = ContainerAttributes.TYPE))
public class NetworkAttributeProvider extends AbstractAttributeProvider {

    private static final String ATTRIBUTE_POINTER_FORMAT = "${container:%s/%s}";

    @Property(name = "runtimeId", value = "${" + RuntimeService.RUNTIME_IDENTITY + "}")
    private String runtimeId;

    @Reference
    private Configurer configurer;

    @Activate
    void activate(Map<String, Object> configuration) throws Exception {
        configurer.configure(configuration, this);
        updateAttributes();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    void updateAttributes() throws UnknownHostException {
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_BIND_ADDRESS, "0.0.0.0");
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_IP, String.format(ATTRIBUTE_POINTER_FORMAT, runtimeId, ContainerAttributes.ATTRIBUTE_ADDRESS_RESOLVER.getName()));
        putAttribute(ContainerAttributes.ATTRIBUTE_ADDRESS_RESOLVER, ContainerAttributes.ATTRIBUTE_KEY_LOCAL_IP.getName());
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_LOCAL_IP, HostUtils.getLocalIp());
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_HOSTNAME, HostUtils.getLocalHostName());
    }
}
