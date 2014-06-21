/*
 * #%L
 * Fabric8 :: SPI
 * %%
 * Copyright (C) 2014 Red Hat
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
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.api.URLServiceEndpoint;

import java.util.Map;

/**
 * An URL service endpoint
 *
 * @author thomas.diesler@jboss.com
 * @since 06-Jun-2014
 */
public class AbstractURLServiceEndpoint extends AbstractServiceEndpoint implements URLServiceEndpoint {

    public AbstractURLServiceEndpoint(ServiceEndpointIdentity identity, Map<AttributeKey<?>, Object> attributes) {
        super(identity, attributes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ServiceEndpoint> T adapt(Class<T> type) {
        T result = super.adapt(type);
        if (result == null) {
            if (type.isAssignableFrom(JMXServiceEndpoint.class)) {
                result = (T) new AbstractJMXServiceEndpoint(this);
            } else if (type.isAssignableFrom(URLServiceEndpoint.class)) {
                result = (T) this;
            }
        }
        return result;
    }

    @Override
    public String getServiceURL() {
        return getRequiredAttribute(URLServiceEndpoint.ATTRIBUTE_KEY_SERVICE_URL);
    }
}
