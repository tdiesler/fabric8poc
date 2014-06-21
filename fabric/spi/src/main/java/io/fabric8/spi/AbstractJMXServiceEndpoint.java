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

import io.fabric8.api.URLServiceEndpoint;
import io.fabric8.spi.utils.ManagementUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

/**
 * An abstract JMX service endpoint
 *
 * @author thomas.diesler@jboss.com
 * @since 06-Jun-2014
 */
public class AbstractJMXServiceEndpoint extends AbstractServiceEndpoint implements JMXServiceEndpoint {

    private final URLServiceEndpoint delegate;

    public AbstractJMXServiceEndpoint(URLServiceEndpoint delegate) {
        super(delegate.getIdentity(), delegate.getAttributes());
        this.delegate = delegate;
    }

    public String getServiceURL() {
        return delegate.getServiceURL();
    }

    @Override
    public JMXServiceURL getJMXServiceURL() {
        JMXServiceURL serviceURL;
        try {
            String urlspec = getRequiredAttribute(URLServiceEndpoint.ATTRIBUTE_KEY_SERVICE_URL);
            serviceURL = new JMXServiceURL(urlspec);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
        return serviceURL;
    }

    @Override
    public Map<String, Object> getDefaultEnvironment() {
        String jmxServiceURL = getRequiredAttribute(URLServiceEndpoint.ATTRIBUTE_KEY_SERVICE_URL);
        return ManagementUtils.getDefaultEnvironment(jmxServiceURL);
    }

    @Override
    public JMXConnector getJMXConnector(Map<String, Object> env, String jmxUsername, String jmxPassword, long timeout, TimeUnit unit) {
        String jmxServiceURL = getRequiredAttribute(URLServiceEndpoint.ATTRIBUTE_KEY_SERVICE_URL);
        if (jmxUsername != null && jmxPassword != null) {
            String[] credentials = new String[] { jmxUsername, jmxPassword };
            env.put(JMXConnector.CREDENTIALS, credentials);
        }
        return ManagementUtils.getJMXConnector(jmxServiceURL, env, timeout, unit);
    }

    @Override
    public JMXConnector getJMXConnector(String jmxUsername, String jmxPassword, long timeout, TimeUnit unit) {
        String jmxServiceURL = getRequiredAttribute(URLServiceEndpoint.ATTRIBUTE_KEY_SERVICE_URL);
        return ManagementUtils.getJMXConnector(jmxServiceURL, jmxUsername, jmxPassword, timeout, unit);
    }

    @Override
    public <T> T getMBeanProxy(MBeanServerConnection server, ObjectName oname, Class<T> type) throws IOException {
        return ManagementUtils.getMBeanProxy(server, oname, type);
    }
}
