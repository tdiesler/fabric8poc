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
import io.fabric8.spi.utils.ManagementUtils;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.gravia.utils.ObjectNameFactory;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the Jolokia endpoint
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Jun-2014
 */
public abstract class JolokiaEndpointTestBase  {

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        PrePostConditions.assertPostConditions();
    }

    @Test
    public void testJolokiaEndpoint() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Container cnt = cntManager.getCurrentContainer();

        ServiceEndpoint sep = cnt.getServiceEndpoint(URLServiceEndpoint.JOLOKIA_SERVICE_ENDPOINT_IDENTITY);
        URLServiceEndpoint urlsep = sep.adapt(URLServiceEndpoint.class);
        String serviceURL = urlsep.getServiceURL();
        Assert.assertNotNull("Jolokia URL not null", serviceURL);

        // Get the local MBeanServer
        MBeanServer server = ServiceLocator.getRequiredService(MBeanServer.class);
        StandardMBean mbean = new StandardMBean(new SimpleMBean(), Simple.class);
        server.registerMBean(mbean, Simple.OBJECT_NAME);
        try {
            Simple proxy = ManagementUtils.getMBeanProxy(server, Simple.OBJECT_NAME, Simple.class);
            Assert.assertEquals("Hello: Kermit", proxy.echo("Kermit"));

            J4pClient client = new J4pClient(serviceURL);
            J4pExecRequest req = new J4pExecRequest(Simple.OBJECT_NAME, "echo", "Kermit");
            J4pResponse<J4pExecRequest> res = client.execute(req);
            Assert.assertEquals("Hello: Kermit", res.getValue());
        } finally {
            server.unregisterMBean(Simple.OBJECT_NAME);
        }
    }

    public interface Simple {
        ObjectName OBJECT_NAME = ObjectNameFactory.create("test:type=simple");
        String echo(String message);
    }

    static class SimpleMBean implements Simple {
        public String echo(String message) {
            return "Hello: " + message;
        }
    }
}
