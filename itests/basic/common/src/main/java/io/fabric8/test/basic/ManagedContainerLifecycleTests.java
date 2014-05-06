/*
 * #%L
 * Fabric8 :: Testsuite :: Basic :: Common
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
package io.fabric8.test.basic;


import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.Constants;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.JMXServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.api.management.ContainerManagement;
import io.fabric8.api.management.ProfileManagement;
import io.fabric8.api.management.ProfileVersionManagement;
import io.fabric8.container.karaf.KarafContainerBuilder;
import io.fabric8.container.tomcat.TomcatContainerBuilder;
import io.fabric8.container.wildfly.WildFlyContainerBuilder;
import io.fabric8.container.wildfly.WildFlyCreateOptions;
import io.fabric8.spi.SystemProperties;
import io.fabric8.test.smoke.PrePostConditions;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test basic container functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public class ManagedContainerLifecycleTests  {

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        PrePostConditions.assertPostConditions();
    }

    @Test
    public void testManagedKaraf() throws Exception {

        // Build the {@link CreateOptions}
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        String dataDir = (String) runtime.getProperty(SystemProperties.KARAF_DATA);
        KarafContainerBuilder cntBuilder = KarafContainerBuilder.create().outputToConsole(true).targetDirectory(dataDir);
        CreateOptions options = cntBuilder.build();

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Container cnt = cntManager.createContainer(options);
        ContainerIdentity cntId = cnt.getIdentity();
        try {
            // Start the container
            cnt = cntManager.startContainer(cntId, null);
            Assert.assertEquals(State.STARTED, cnt.getState());

            verifyContainer(cnt, "karaf", "karaf");
        } finally {
            cntManager.destroyContainer(cntId);
        }
    }

    @Test
    public void testManagedTomcat() throws Exception {

        // Build the {@link CreateOptions}
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        String dataDir = (String) runtime.getProperty(SystemProperties.KARAF_DATA);
        TomcatContainerBuilder cntBuilder = TomcatContainerBuilder.create().outputToConsole(true).targetDirectory(dataDir);
        CreateOptions options = cntBuilder.build();

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Container cnt = cntManager.createContainer(options);
        ContainerIdentity cntId = cnt.getIdentity();
        try {
            // Start the container
            cnt = cntManager.startContainer(cntId, null);
            Assert.assertEquals(State.STARTED, cnt.getState());

            verifyContainer(cnt, null, null);
        } finally {
            cntManager.destroyContainer(cntId);
        }
    }

    @Test
    public void testManagedWildFly() throws Exception {

        // Build the {@link CreateOptions}
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        String dataDir = (String) runtime.getProperty(SystemProperties.KARAF_DATA);
        WildFlyContainerBuilder cntBuilder = WildFlyContainerBuilder.create().outputToConsole(true).targetDirectory(dataDir);
        // [TODO] The default port of the running server is available, why?
        cntBuilder.managementNativePort(WildFlyCreateOptions.DEFAULT_MANAGEMENT_NATIVE_PORT + 1);
        cntBuilder.managementHttpPort(WildFlyCreateOptions.DEFAULT_MANAGEMENT_HTTP_PORT + 1);
        CreateOptions options = cntBuilder.build();

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Container cnt = cntManager.createContainer(options);
        ContainerIdentity cntId = cnt.getIdentity();
        try {
            // Start the container
            cnt = cntManager.startContainer(cntId, null);
            Assert.assertEquals(State.STARTED, cnt.getState());

            verifyContainer(cnt, null, null);
        } finally {
            cntManager.destroyContainer(cntId);
        }
    }

    private void verifyContainer(Container cnt, String username, String password) throws IOException {

        // Assert that there is one {@link JMXServiceEndpoint}
        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Assert.assertEquals(1, cnt.getEndpointIdentities(JMXServiceEndpoint.class).size());
        Assert.assertEquals(1, cnt.getEndpointIdentities(null).size());

        // Get the JMX connector through the endpoint
        ServiceEndpointIdentity<?> endpointId = cnt.getEndpointIdentities(null).iterator().next();
        JMXServiceEndpoint jmxEndpoint = cntManager.getServiceEndpoint(cnt.getIdentity(), JMXServiceEndpoint.class);
        Assert.assertNotNull("JMXServiceEndpoint not null", jmxEndpoint);
        Assert.assertSame(jmxEndpoint, cntManager.getServiceEndpoint(cnt.getIdentity(), endpointId));

        System.out.println(jmxEndpoint);
        JMXConnector connector = jmxEndpoint.getJMXConnector(username, password, 20, TimeUnit.SECONDS);
        try {
            // Access containers through JMX
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ContainerManagement cntManagement = jmxEndpoint.getMBeanProxy(server, ContainerManagement.OBJECT_NAME, ContainerManagement.class);
            Assert.assertNotNull("ContainerManagement not null", cntManagement);
            Set<String> containerIds = cntManagement.getContainerIds();
            Assert.assertEquals("One container", 1, containerIds.size());
            ContainerIdentity cntId = ContainerIdentity.create(containerIds.iterator().next());
            Assert.assertEquals(Constants.CURRENT_CONTAINER_IDENTITY, cntId);

            // Access profiles through JMX
            ProfileVersionManagement prvManagement = jmxEndpoint.getMBeanProxy(server, ProfileVersionManagement.OBJECT_NAME, ProfileVersionManagement.class);
            ProfileManagement prfManagement = jmxEndpoint.getMBeanProxy(server, ProfileManagement.OBJECT_NAME, ProfileManagement.class);
            Assert.assertNotNull("ProfileManagement not null", prfManagement);
            Assert.assertEquals(1, prvManagement.getProfileVersionIds().size());
            Assert.assertEquals("1.0.0", prvManagement.getProfileVersionIds().iterator().next());
            Assert.assertEquals(1, prfManagement.getProfileIds("1.0").size());
            Assert.assertEquals("default", prfManagement.getProfileIds("1.0").iterator().next());
        } finally {
            connector.close();
        }
    }
}
