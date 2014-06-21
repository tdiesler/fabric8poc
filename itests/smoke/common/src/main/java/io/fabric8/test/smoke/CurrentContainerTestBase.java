/*
 * #%L
 * Fabric8 :: Testsuite :: Smoke :: Embedded
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

import static io.fabric8.api.ContainerAttributes.JMX_SERVICE_ENDPOINT_IDENTITY;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerAttributes;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.api.management.ContainerManagement;
import io.fabric8.spi.JMXServiceEndpoint;
import io.fabric8.spi.RuntimeService;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test current container functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public abstract class CurrentContainerTestBase {

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
    public void testCurrentContainer() throws Exception {

        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        String runtimeId = (String) runtime.getProperty(RuntimeService.RUNTIME_IDENTITY);
        ContainerIdentity currentId = ContainerIdentity.create(runtimeId);

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Container cnt = cntManager.getCurrentContainer();
        Assert.assertEquals(currentId, cnt.getIdentity());

        Set<ServiceEndpoint> endpoints = cnt.getServiceEndpoints();
        Assert.assertEquals(1, endpoints.size());
        Assert.assertEquals(ServiceEndpointIdentity.create("jmx"), endpoints.iterator().next().getIdentity());

        String jmxServerUrl = cnt.getAttribute(ContainerAttributes.ATTRIBUTE_KEY_JMX_SERVER_URL);
        Assert.assertNotNull("JMX server URL not null", jmxServerUrl);

        String[] userpass = RuntimeType.KARAF == RuntimeType.getRuntimeType() ? karafJmx : otherJmx;
        ServiceEndpoint sep = cntManager.getServiceEndpoint(currentId, JMX_SERVICE_ENDPOINT_IDENTITY);
        JMXServiceEndpoint jmxEndpoint = sep.adapt(JMXServiceEndpoint.class);
        JMXConnector connector = jmxEndpoint.getJMXConnector(userpass[0], userpass[1], 200, TimeUnit.MILLISECONDS);
        try {
            // Access containers through JMX
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ContainerManagement cntManagement = jmxEndpoint.getMBeanProxy(server, ContainerManagement.OBJECT_NAME, ContainerManagement.class);
            Assert.assertNotNull("ContainerManagement not null", cntManagement);
            Set<String> containerIds = cntManagement.getContainerIds();
            Assert.assertEquals("One container", 1, containerIds.size());
            ContainerIdentity cntId = ContainerIdentity.create(containerIds.iterator().next());
            Assert.assertEquals(cnt.getIdentity(), cntId);
        } finally {
            connector.close();
        }
    }
}
