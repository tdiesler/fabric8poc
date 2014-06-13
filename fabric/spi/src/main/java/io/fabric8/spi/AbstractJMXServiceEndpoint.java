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

import static io.fabric8.api.ContainerAttributes.ATTRIBUTE_KEY_JMX_SERVER_URL;
import io.fabric8.api.AttributeKey;
import io.fabric8.spi.utils.ManagementUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.jboss.gravia.utils.IllegalArgumentAssertion;

/**
 * An abstract JMX service endpoint
 *
 * @author thomas.diesler@jboss.com
 * @since 06-Jun-2014
 */
public class AbstractJMXServiceEndpoint extends SimpleServiceEndpoint<JMXServiceEndpoint> implements JMXServiceEndpoint {

    public AbstractJMXServiceEndpoint(String identity, Map<AttributeKey<?>, Object> attributes) {
        super(identity, JMXServiceEndpoint.class.getCanonicalName(), attributes);
    }

    public AbstractJMXServiceEndpoint(String identity, String jmxServerUrl) {
        super(identity, JMXServiceEndpoint.class.getCanonicalName(), getJmxServerAttributes(jmxServerUrl));
    }

    private static Map<AttributeKey<?>, Object> getJmxServerAttributes(String jmxServerUrl) {
        IllegalArgumentAssertion.assertNotNull(jmxServerUrl, "jmxServerUrl");
        return Collections.<AttributeKey<?>, Object> singletonMap(ATTRIBUTE_KEY_JMX_SERVER_URL, jmxServerUrl);
    }

    @Override
    public JMXServiceURL getServiceURL() {
        JMXServiceURL serviceURL;
        try {
            String jmxServiceURL = getRequiredAttribute(ATTRIBUTE_KEY_JMX_SERVER_URL);
            serviceURL = new JMXServiceURL(jmxServiceURL);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
        return serviceURL;
    }

    @Override
    public Map<String, Object> getDefaultEnvironment() {
        String jmxServiceURL = getRequiredAttribute(ATTRIBUTE_KEY_JMX_SERVER_URL);
        return ManagementUtils.getDefaultEnvironment(jmxServiceURL);
    }

    @Override
    public JMXConnector getJMXConnector(Map<String, Object> env, String jmxUsername, String jmxPassword, long timeout, TimeUnit unit) {
        String jmxServiceURL = getRequiredAttribute(ATTRIBUTE_KEY_JMX_SERVER_URL);
        if (jmxUsername != null && jmxPassword != null) {
            String[] credentials = new String[] { jmxUsername, jmxPassword };
            env.put(JMXConnector.CREDENTIALS, credentials);
        }
        return ManagementUtils.getJMXConnector(jmxServiceURL, env, timeout, unit);
    }

    @Override
    public JMXConnector getJMXConnector(String jmxUsername, String jmxPassword, long timeout, TimeUnit unit) {
        String jmxServiceURL = getRequiredAttribute(ATTRIBUTE_KEY_JMX_SERVER_URL);
        return ManagementUtils.getJMXConnector(jmxServiceURL, jmxUsername, jmxPassword, timeout, unit);
    }

    @Override
    public <T> T getMBeanProxy(MBeanServerConnection server, ObjectName oname, Class<T> type) throws IOException {
        return ManagementUtils.getMBeanProxy(server, oname, type);
    }
}
