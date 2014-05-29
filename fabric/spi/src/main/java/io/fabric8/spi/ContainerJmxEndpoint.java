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

package io.fabric8.spi;

import io.fabric8.api.Container;
import io.fabric8.api.JMXServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.spi.utils.ManagementUtils;

import javax.management.remote.JMXConnector;
import java.util.concurrent.TimeUnit;

public class ContainerJmxEndpoint extends AbstractJMXServiceEndpoint {

    private final Container container;
    private final ServiceEndpointIdentity<JMXServiceEndpoint> endpointIdentity;

    public ContainerJmxEndpoint(Container container, ServiceEndpointIdentity<JMXServiceEndpoint> endpointIdentity) {
        super(container);
        this.container = container;
        this.endpointIdentity = endpointIdentity;
    }

    @Override
    public JMXConnector getJMXConnector(String jmxUsername, String jmxPassword, long timeout, TimeUnit unit) {
        return ManagementUtils.getJMXConnector(container, jmxUsername, jmxPassword, timeout, unit);
    }

    @Override
    public ServiceEndpointIdentity<JMXServiceEndpoint> getIdentity() {
        return endpointIdentity;
    }
}
