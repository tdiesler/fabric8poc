/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.container.tomcat;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.JMXServiceEndpoint;
import io.fabric8.api.LifecycleException;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.spi.AbstractJMXServiceEndpoint;
import io.fabric8.spi.ContainerCreateHandler;
import io.fabric8.spi.ContainerHandle;
import io.fabric8.spi.ManagedContainer;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.remote.JMXConnector;

/**
 * The Tomcat container create handler
 *
 * @author thomas.diesler@jboss.com
 * @since 16-Apr-2014
 */
public final class TomcatContainerCreateHandler implements ContainerCreateHandler {

    @Override
    public boolean accept(CreateOptions options) {
        return options instanceof TomcatCreateOptions;
    }

    @Override
    public ContainerHandle create(final CreateOptions options) {
        final TomcatManagedContainer container = new TomcatManagedContainer((TomcatCreateOptions) options);
        container.create();
        return new TomcatContainerHandle(container, options);
    }

    static class TomcatContainerHandle implements ContainerHandle {

        private final ManagedContainer<?> container;
        private final CreateOptions options;

        TomcatContainerHandle(ManagedContainer<?> container, CreateOptions options) {
            this.container = container;
            this.options = options;
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
            return options;
        }

        @Override
        public void start() throws LifecycleException {
            container.start();
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
            ServiceEndpoint endpoint = new AbstractJMXServiceEndpoint() {

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
            return Collections.singleton(endpoint);
        }
    }
}
