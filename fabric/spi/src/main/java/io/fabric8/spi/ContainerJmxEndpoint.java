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

import io.fabric8.api.AttributeKey;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.JMXServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;

import java.util.Map;

/**
 * An abstract JMX service endpoint
 *
 * @author thomas.diesler@jboss.com
 * @since 06-Jun-2014
 */
public class ContainerJmxEndpoint extends AbstractJMXServiceEndpoint {

    public ContainerJmxEndpoint(ContainerIdentity identity, Map<AttributeKey<?>, Object> attributes) {
        super(getEndpointIdentity(identity), attributes);
    }

    public ContainerJmxEndpoint(ContainerIdentity identity, String jmxServerUrl) {
        super(getEndpointIdentity(identity), jmxServerUrl);
    }

    private static ServiceEndpointIdentity<JMXServiceEndpoint> getEndpointIdentity(ContainerIdentity identity) {
        String idspec = identity.getSymbolicName() + "-" + JMXServiceEndpoint.class.getSimpleName();
        return ServiceEndpointIdentity.create(idspec , JMXServiceEndpoint.class);
    }
}
