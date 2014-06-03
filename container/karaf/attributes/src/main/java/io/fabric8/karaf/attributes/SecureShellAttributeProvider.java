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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import java.util.Map;

@Component(configurationPid = SecureShellAttributeProvider.SSH_PID, policy = ConfigurationPolicy.REQUIRE, immediate = true)
@Service(AttributeProvider.class)
@Properties(
        @Property(name = "type", value = ContainerAttributes.TYPE)
)
public class SecureShellAttributeProvider extends AttributeProviderComponent {

    static final String SSH_PID = "org.apache.karaf.shell";

    private static final String SSH_URL_FORMAT = "ssh://${container:%s/fabric8.ip}:%d";
    private static final String SSH_BINDING_PORT_KEY = "sshPort";
    private static final String SSH_CONNECTION_PORT_KEY = "sshConnectionPort";

    @Property(name = SSH_BINDING_PORT_KEY, value = "${"+SSH_BINDING_PORT_KEY+"}")
    private int sshPort=8101;
    @Property(name = SSH_CONNECTION_PORT_KEY, value = "${"+SSH_CONNECTION_PORT_KEY+"}")
    private int sshConnectionPort=8101;

    @Property(name ="runtimeId", value = "${"+RuntimeService.RUNTIME_IDENTITY+"}")
    private String runtimeId;

    @Reference
    private Configurer configurer;

    @Activate
    void activate(Map<String, Object> configuration) throws Exception {
        configurer.configure(configuration, this);
        updateAttributes();
        activateComponent();
    }

    @Modified
    void modified(Map<String, Object> configuration) throws Exception {
        configurer.configure(configuration, this);
        updateAttributes();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    private void updateAttributes() {
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_SSH_SERVER_URL, getSshUrl(runtimeId, sshConnectionPort));
    }

    private String getSshUrl(String name, int port)  {
        return String.format(SSH_URL_FORMAT, name, port);
    }
}
