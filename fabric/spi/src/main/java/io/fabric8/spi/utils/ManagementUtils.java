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
package io.fabric8.spi.utils;

import io.fabric8.api.Attributable;
import io.fabric8.api.Constants;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.gravia.utils.MBeanProxy;
import org.jboss.gravia.utils.NotNullException;

/**
 * A set of management utils
 *
 * @author Thomas.Diesler@jboss.com
 * @since 16-Apr-2014
 */
public final class ManagementUtils {

    // Hide ctor
    private ManagementUtils() {
    }

    public static <T> T getMBeanProxy(MBeanServerConnection server, ObjectName oname, Class<T> type) throws IOException {
        long end = System.currentTimeMillis() + 10000L;
        while (System.currentTimeMillis() < end) {
            if (server.isRegistered(oname)) {
                return MBeanProxy.get(server, oname, type);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                break;
            }
        }
        throw new IllegalStateException("Cannot obtain MBean proxy for: " + oname);
    }

    public static JMXConnector getJMXConnector(Attributable attributes, String username, String password, long timeout, TimeUnit unit) {
        String jmxServiceURL = attributes.getAttribute(Constants.ATTRIBUTE_KEY_JMX_SERVER_URL);
        if (jmxServiceURL == null)
            throw new IllegalStateException("Cannot obtain container attribute: JMX_SERVER_URL");
        return getJMXConnector(jmxServiceURL, username, password, timeout, unit);
    }

    public static JMXConnector getJMXConnector(String jmxServiceURL, String jmxUsername, String jmxPassword, long timeout, TimeUnit unit) {
        NotNullException.assertValue(jmxServiceURL, "jmxServiceURL");
        Map<String, Object> env = new HashMap<String, Object>();
        if (jmxUsername != null && jmxPassword != null) {
            String[] credentials = new String[] { jmxUsername, jmxPassword };
            env.put(JMXConnector.CREDENTIALS, credentials);
        }
        return getJMXConnector(jmxServiceURL, env, timeout, unit);
    }

    public static JMXConnector getJMXConnector(String jmxServiceURL, Map<String, Object> env, long timeout, TimeUnit unit) {

        JMXServiceURL serviceURL;
        try {
            serviceURL = new JMXServiceURL(jmxServiceURL);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }

        Exception lastException = null;
        JMXConnector connector = null;
        long end = System.currentTimeMillis() + unit.toMillis(timeout);
        while (connector == null && System.currentTimeMillis() < end) {
            try {
                connector = JMXConnectorFactory.connect(serviceURL, env);
            } catch (Exception ex) {
                lastException = ex;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }
        if (connector == null) {
            throw new IllegalStateException("Cannot obtain JMXConnector for: " + jmxServiceURL, lastException);
        }
        return connector;
    }
}