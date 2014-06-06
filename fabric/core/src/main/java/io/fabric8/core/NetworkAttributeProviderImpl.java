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

import io.fabric8.api.ContainerAttributes;
import io.fabric8.spi.AttributeProvider;
import io.fabric8.spi.NetworkAttributeProvider;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AbstractAttributeProvider;
import io.fabric8.spi.scr.ValidatingReference;
import io.fabric8.spi.utils.HostUtils;

import java.net.UnknownHostException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service({AttributeProvider.class, NetworkAttributeProvider.class})
@Properties(@Property(name = "type", value = ContainerAttributes.TYPE))
public class NetworkAttributeProviderImpl extends AbstractAttributeProvider implements NetworkAttributeProvider {

    private static final String ATTRIBUTE_POINTER_FORMAT = "${container:%s/%s}";

    @Reference(referenceInterface = RuntimeService.class)
    private final ValidatingReference<RuntimeService> runtimeService = new ValidatingReference<>();

    private String runtimeId;
    private String ip;
    private String localIp;
    private String localHostName;

    @Activate
    void activate() throws Exception {
        runtimeId = runtimeService.get().getProperty(RuntimeService.RUNTIME_IDENTITY);
        updateAttributes();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getIp() {
        // [TODO] compute ip
        // return ip;
        return localIp;
    }

    @Override
    public String getLocalIp() {
        return localIp;
    }

    @Override
    public String getLocalHostName() {
        return localHostName;
    }

    private void updateAttributes() throws UnknownHostException {
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_BIND_ADDRESS, "0.0.0.0");
        putAttribute(ContainerAttributes.ATTRIBUTE_ADDRESS_RESOLVER, ContainerAttributes.ATTRIBUTE_KEY_LOCAL_IP.getName());
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_IP, getIp(runtimeId, ContainerAttributes.ATTRIBUTE_ADDRESS_RESOLVER.getName()));
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_LOCAL_IP, localIp = HostUtils.getLocalIp());
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_HOSTNAME, localHostName = HostUtils.getLocalHostName());
    }

    private String getIp(String name, String resolver) {
        return ip = String.format(ATTRIBUTE_POINTER_FORMAT, name, resolver);
    }

    void bindRuntimeService(RuntimeService service) {
        runtimeService.bind(service);
    }
    void unbindRuntimeService(RuntimeService service) {
        runtimeService.unbind(service);
    }
}
