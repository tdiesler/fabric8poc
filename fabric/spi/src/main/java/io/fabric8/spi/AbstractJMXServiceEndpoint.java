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

import io.fabric8.api.Attributable;
import io.fabric8.api.ContainerAttributes;
import io.fabric8.api.JMXServiceEndpoint;
import io.fabric8.spi.utils.ManagementUtils;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

public abstract class AbstractJMXServiceEndpoint extends AttributeSupport implements JMXServiceEndpoint {

    public AbstractJMXServiceEndpoint(Attributable attributes) {
        String jmxServiceURL = attributes.getAttribute(ContainerAttributes.ATTRIBUTE_KEY_JMX_SERVER_URL);
        putAttribute(ContainerAttributes.ATTRIBUTE_KEY_JMX_SERVER_URL, jmxServiceURL);
    }

    @Override
    public <T> T getMBeanProxy(MBeanServerConnection server, ObjectName oname, Class<T> type) throws IOException {
        return ManagementUtils.getMBeanProxy(server, oname, type);
    }

    public String toString() {
        return "JMXServiceEndpoint" + getAttributes();
    }
}
