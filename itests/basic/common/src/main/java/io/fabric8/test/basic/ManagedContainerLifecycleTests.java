/*
 * #%L
 * Gravia :: Runtime :: Embedded
 * %%
 * Copyright (C) 2013 - 2014 JBoss by Red Hat
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
import io.fabric8.api.ContainerBuilder;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.JMXServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.management.ContainerManagement;
import io.fabric8.api.management.ProfileManagement;
import io.fabric8.container.karaf.KarafContainerBuilder;
import io.fabric8.container.tomcat.TomcatContainerBuilder;
import io.fabric8.container.wildfly.WildFlyContainerBuilder;
import io.fabric8.spi.SystemProperties;
import io.fabric8.test.smoke.TestConditions;

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
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public class ManagedContainerLifecycleTests  {

    @Before
    public void preConditions() {
        TestConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        TestConditions.assertPostConditions();
    }

    @Test
    public void testManagedKaraf() throws Exception {

        // Build the {@link CreateOptions}
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        String dataDir = (String) runtime.getProperty(SystemProperties.KARAF_DATA);
        ContainerBuilder<?, ?> builder = new KarafContainerBuilder().setOutputToConsole(true).setTargetDirectory(dataDir);
        CreateOptions options = builder.getCreateOptions();

        ContainerManager cntManager = ServiceLocator.getRequiredService(ContainerManager.class);
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
        ContainerBuilder<?, ?> builder = new TomcatContainerBuilder().setOutputToConsole(true).setTargetDirectory(dataDir);
        CreateOptions options = builder.getCreateOptions();

        ContainerManager cntManager = ServiceLocator.getRequiredService(ContainerManager.class);
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
        ContainerBuilder<?, ?> builder = new WildFlyContainerBuilder().setOutputToConsole(true).setTargetDirectory(dataDir);
        CreateOptions options = builder.getCreateOptions();

        ContainerManager cntManager = ServiceLocator.getRequiredService(ContainerManager.class);
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

    private void verifyContainer(Container container, String username, String password) throws IOException {

        // Assert that there is one {@link JMXServiceEndpoint}
        Set<ServiceEndpointIdentity> endpointIds = container.getServiceEndpoints(JMXServiceEndpoint.class);
        Assert.assertEquals(1, endpointIds.size());

        // Get the JMX connector through the endpoint
        ServiceEndpointIdentity jmxEndpointId = endpointIds.iterator().next();
        JMXServiceEndpoint jmxEndpoint = container.getServiceEndpoint(jmxEndpointId, JMXServiceEndpoint.class);
        JMXConnector connector = jmxEndpoint.getJMXConnector(username, password, 20, TimeUnit.SECONDS);

        try {
            // Access containers through JMX
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ContainerManagement cntManagement = jmxEndpoint.getMBeanProxy(server, ContainerManagement.OBJECT_NAME, ContainerManagement.class);
            Assert.assertNotNull("ContainerManagement not null", cntManagement);
            Assert.assertTrue("No containers", cntManagement.getContainerIds().isEmpty());

            // Access profiles through JMX
            ProfileManagement prfManagement = jmxEndpoint.getMBeanProxy(server, ProfileManagement.OBJECT_NAME, ProfileManagement.class);
            Assert.assertNotNull("ProfileManagement not null", prfManagement);
            Assert.assertEquals(1, prfManagement.getProfileVersionIds().size());
            Assert.assertEquals("1.0.0", prfManagement.getProfileVersionIds().iterator().next());
            Assert.assertEquals(1, prfManagement.getProfileIds("1.0").size());
            Assert.assertEquals("default", prfManagement.getProfileIds("1.0").iterator().next());
        } finally {
            connector.close();
        }
    }
}
