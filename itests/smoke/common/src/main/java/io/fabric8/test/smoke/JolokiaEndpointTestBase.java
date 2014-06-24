/*
 * #%L
 * Fabric8 :: Testsuite :: Smoke :: Common
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
package io.fabric8.test.smoke;

import io.fabric8.api.Container;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.URLServiceEndpoint;
import io.fabric8.spi.JMXServiceEndpoint;
import io.fabric8.spi.utils.ManagementUtils;
import io.fabric8.test.smoke.sub.c.Bean;
import io.fabric8.test.smoke.sub.c.BeanOpenType;

import java.util.concurrent.TimeUnit;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;

import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.gravia.utils.ObjectNameFactory;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;
import org.jolokia.client.request.J4pWriteRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the Jolokia endpoint
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Jun-2014
 */
public abstract class JolokiaEndpointTestBase {

    static String[] karafJmx = new String[] { "karaf", "karaf" };
    static String[] otherJmx = new String[] { null, null };

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        PrePostConditions.assertPostConditions();
    }

    @Test
    public void testMXBeanEndpoint() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Container cnt = cntManager.getCurrentContainer();

        ServiceEndpoint sep = cnt.getServiceEndpoint(URLServiceEndpoint.JMX_SERVICE_ENDPOINT_IDENTITY);
        JMXServiceEndpoint jmxEndpoint = sep.adapt(JMXServiceEndpoint.class);
        String serviceURL = jmxEndpoint.getServiceURL();
        Assert.assertNotNull("JMX URL not null", serviceURL);

        // Get the local MBeanServer
        MBeanServer server = ServiceLocator.getRequiredService(MBeanServer.class);
        server.registerMBean(new Simple(), SimpleMXBean.OBJECT_NAME);
        try {
            String[] userpass = RuntimeType.KARAF == RuntimeType.getRuntimeType() ? karafJmx : otherJmx;
            JMXConnector jmxConnector = jmxEndpoint.getJMXConnector(userpass[0], userpass[1], 200, TimeUnit.MILLISECONDS);
            MBeanServerConnection con = jmxConnector.getMBeanServerConnection();
            try {
                // Simple string echo
                Object[] params = new Object[]{ "Kermit" };
                String[] signature = new String[]{ String.class.getName() };
                Object result = con.invoke(SimpleMXBean.OBJECT_NAME, "echo", params, signature);
                Assert.assertEquals("Hello: Kermit", result);

                // Set Bean attribute using CompositeData
                Bean bean = new Bean("Hello", "Foo");
                CompositeData cdata = BeanOpenType.toCompositeData(bean);
                con.setAttribute(SimpleMXBean.OBJECT_NAME, new Attribute("Bean", cdata));

                // Get Bean attribute using CompositeData
                cdata = (CompositeData) con.getAttribute(SimpleMXBean.OBJECT_NAME, "Bean");
                Assert.assertEquals(bean, BeanOpenType.fromCompositeData(cdata));

                // Simple Bean echo using CompositeData
                params = new Object[]{ cdata };
                signature = new String[]{ CompositeData.class.getName() };
                cdata = (CompositeData) con.invoke(SimpleMXBean.OBJECT_NAME, "echoBean", params, signature);
                Assert.assertEquals(bean, BeanOpenType.fromCompositeData(cdata));
            } finally {
                jmxConnector.close();
            }
        } finally {
            server.unregisterMBean(SimpleMXBean.OBJECT_NAME);
        }
    }

    @Test
    public void testMXBeanProxy() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Container cnt = cntManager.getCurrentContainer();

        ServiceEndpoint sep = cnt.getServiceEndpoint(URLServiceEndpoint.JMX_SERVICE_ENDPOINT_IDENTITY);
        JMXServiceEndpoint jmxEndpoint = sep.adapt(JMXServiceEndpoint.class);
        String serviceURL = jmxEndpoint.getServiceURL();
        Assert.assertNotNull("JMX URL not null", serviceURL);

        // Get the local MBeanServer
        MBeanServer server = ServiceLocator.getRequiredService(MBeanServer.class);
        server.registerMBean(new Simple(), SimpleMXBean.OBJECT_NAME);
        try {
            String[] userpass = RuntimeType.KARAF == RuntimeType.getRuntimeType() ? karafJmx : otherJmx;
            JMXConnector jmxConnector = jmxEndpoint.getJMXConnector(userpass[0], userpass[1], 200, TimeUnit.MILLISECONDS);
            MBeanServerConnection con = jmxConnector.getMBeanServerConnection();
            try {
                SimpleMXBean proxy = ManagementUtils.getMXBeanProxy(con, SimpleMXBean.OBJECT_NAME, SimpleMXBean.class);

                // Simple string echo
                Assert.assertEquals("Hello: Kermit", proxy.echo("Kermit"));

                // Set Bean attribute using CompositeData
                Bean bean = new Bean("Hello", "Foo");
                proxy.setBean(bean);

                // Get Bean attribute using CompositeData
                Assert.assertEquals(bean, proxy.getBean());

                // Simple Bean echo using CompositeData
                Assert.assertEquals(bean, proxy.echoBean(bean));
            } finally {
                jmxConnector.close();
            }
        } finally {
            server.unregisterMBean(SimpleMXBean.OBJECT_NAME);
        }
    }

    @Test
    @Ignore
    public void testJolokiaEndpoint() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Container cnt = cntManager.getCurrentContainer();

        ServiceEndpoint sep = cnt.getServiceEndpoint(URLServiceEndpoint.JOLOKIA_SERVICE_ENDPOINT_IDENTITY);
        URLServiceEndpoint urlsep = sep.adapt(URLServiceEndpoint.class);
        String serviceURL = urlsep.getServiceURL();
        Assert.assertNotNull("Jolokia URL not null", serviceURL);

        // Get the local MBeanServer
        MBeanServer server = ServiceLocator.getRequiredService(MBeanServer.class);
        server.registerMBean(new Simple(), SimpleMXBean.OBJECT_NAME);
        try {
            // Simple string echo
            J4pClient client = new J4pClient(serviceURL);
            J4pExecRequest execReq = new J4pExecRequest(SimpleMXBean.OBJECT_NAME, "echo", "Kermit");
            J4pResponse<?> res = client.execute(execReq);
            Assert.assertEquals("Hello: Kermit", res.getValue());

            // Set Bean attribute using CompositeData
            Bean bean = new Bean("Hello", "Foo");
            CompositeData cdata = BeanOpenType.toCompositeData(bean);
            J4pWriteRequest writeReq = new J4pWriteRequest(SimpleMXBean.OBJECT_NAME, "Bean", cdata);
            writeReq.setPreferredHttpMethod("POST");
            res = client.execute(writeReq);

            // Get Bean attribute using CompositeData
            J4pReadRequest readReq = new J4pReadRequest(SimpleMXBean.OBJECT_NAME, "Bean");
            res = client.execute(readReq);
            Object value = res.getValue();
        } finally {
            server.unregisterMBean(SimpleMXBean.OBJECT_NAME);
        }
    }

    public interface SimpleMXBean {

        ObjectName OBJECT_NAME = ObjectNameFactory.create("test:type=simple");

        String echo(String message);

        Bean getBean();

        void setBean(Bean bean);

        Bean echoBean(Bean bean);
    }

    static class Simple implements SimpleMXBean {

        private Bean bean;

        public String echo(String message) {
            return "Hello: " + message;
        }

        public Bean getBean() {
            return bean;
        }

        @Override
        public void setBean(Bean bean) {
            this.bean = bean;
        }

        @Override
        public Bean echoBean(Bean bean) {
            return new Bean(bean.getName(), bean.getValue());
        }
    }
}
