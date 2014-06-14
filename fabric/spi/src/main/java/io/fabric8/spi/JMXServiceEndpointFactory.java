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
import io.fabric8.api.ContainerAttributes;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.ServiceEndpointFactory;
import io.fabric8.api.ServiceEndpointIdentity;

import java.util.Map;

/**
 * A JMX service endpoint
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Apr-2014
 */
public final class JMXServiceEndpointFactory implements ServiceEndpointFactory<JMXServiceEndpoint> {

    @Override
    public boolean isSupported(ServiceEndpoint endpoint) {
        return endpoint.hasAttribute(ContainerAttributes.ATTRIBUTE_KEY_JMX_SERVER_URL);
    }

    @Override
    public JMXServiceEndpoint create(ServiceEndpoint endpoint) {
        return create(endpoint.getIdentity(), endpoint.getAttributes());
    }

    @Override
    public JMXServiceEndpoint create(ServiceEndpointIdentity identity, Map<AttributeKey<?>, Object> attributes) {
        return new ContainerJmxEndpoint(identity, attributes);
    }
}
