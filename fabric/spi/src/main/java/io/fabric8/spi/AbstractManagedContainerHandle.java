/*
 * #%L
 * Gravia :: Integration Tests :: Common
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 * %%
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
 * limitations under the License.
 * #L%
 */
package io.fabric8.spi;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.JMXServiceEndpoint;
import io.fabric8.api.LifecycleException;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.remote.JMXConnector;

/**
 * A handle to a container instance
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Apr-2014
 */
public abstract class AbstractManagedContainerHandle implements ContainerHandle {

    private final Set<ServiceEndpoint> endpoints = new HashSet<>();
    private final ManagedContainer<?> container;

    protected AbstractManagedContainerHandle(ManagedContainer<?> container) {
        this.container = container;
    }

    @Override
    public Set<AttributeKey<?>> getAttributeKeys() {
        return container.getAttributeKeys();
    }

    @Override
    public <T> T getAttribute(AttributeKey<T> key) {
        return container.getAttribute(key);
    }

    @Override
    public <T> boolean hasAttribute(AttributeKey<T> key) {
        return container.hasAttribute(key);
    }

    @Override
    public Map<AttributeKey<?>, Object> getAttributes() {
        return container.getAttributes();
    }

    @Override
    public CreateOptions getCreateOptions() {
        return container.getCreateOptions();
    }

    @Override
    public void start() throws LifecycleException {
        container.start();
        ServiceEndpoint endpoint = new AbstractJMXServiceEndpoint(container) {

            private final ServiceEndpointIdentity<JMXServiceEndpoint> identity;
            {
                String idspec = container.getIdentity().getSymbolicName() + "-" + JMXServiceEndpoint.class.getSimpleName();
                identity = ServiceEndpointIdentity.create(idspec, JMXServiceEndpoint.class);
            }

            @Override
            public ServiceEndpointIdentity<JMXServiceEndpoint> getIdentity() {
                return identity;
            }

            @Override
            public JMXConnector getJMXConnector(String jmxUsername, String jmxPassword, long timeout, TimeUnit unit) {
                return container.getJMXConnector(jmxUsername, jmxPassword, timeout, unit);
            }
        };
        endpoints.add(endpoint);
    }

    @Override
    public void stop() throws LifecycleException {
        container.stop();
    }

    @Override
    public void destroy() throws LifecycleException {
        container.destroy();
    }

    @Override
    public Set<ServiceEndpoint> getServiceEndpoints() {
        return Collections.unmodifiableSet(endpoints);
    }
}