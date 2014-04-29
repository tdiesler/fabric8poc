/*
 * #%L
 * Fabric8 :: Testsuite :: Basic :: Embedded
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

package io.fabric8.test.basic.embedded;

import io.fabric8.api.management.ContainerManagement;
import io.fabric8.api.management.ProfileManagement;
import io.fabric8.api.management.ProfileVersionManagement;
import io.fabric8.container.karaf.KarafContainerBuilder;
import io.fabric8.container.tomcat.TomcatContainerBuilder;
import io.fabric8.container.wildfly.WildFlyContainerBuilder;
import io.fabric8.spi.ManagedContainer;
import io.fabric8.spi.ManagedContainerBuilder;
import io.fabric8.spi.utils.ManagementUtils;
import io.fabric8.test.embedded.support.EmbeddedTestSupport;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test basic {@link ManagedContainer} functionality
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2014
 */
public class StandaloneManagedContainerTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        EmbeddedTestSupport.beforeClass();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        EmbeddedTestSupport.afterClass();
    }

    @Test
    @SuppressWarnings({ "rawtypes" })
    public void testManagedKaraf() throws Exception {
        KarafContainerBuilder builder = buildCreateOptions(KarafContainerBuilder.create());
        ManagedContainer container = builder.getManagedContainer();
        try {
            container.create();
            verifyContainer(container, "karaf", "karaf");
        } finally {
            container.destroy();
        }
    }

    @Test
    @SuppressWarnings({ "rawtypes" })
    public void testManagedTomcat() throws Exception {
        TomcatContainerBuilder builder = buildCreateOptions(TomcatContainerBuilder.create());
        ManagedContainer container = builder.getManagedContainer();
        try {
            container.create();
            verifyContainer(container, null, null);
        } finally {
            container.destroy();
        }
    }

    @Test
    @SuppressWarnings({ "rawtypes" })
    public void testManagedWildFly() throws Exception {
        WildFlyContainerBuilder builder = buildCreateOptions(WildFlyContainerBuilder.create());
        ManagedContainer container = builder.getManagedContainer();
        try {
            container.create();
            verifyContainer(container, null, null);
        } finally {
            container.destroy();
        }
    }

    private <B extends ManagedContainerBuilder<?, ?>> B buildCreateOptions(B builder) {
        builder.setTargetDirectory("target/managed-container").setOutputToConsole(true);
        return builder;
    }

    @SuppressWarnings({ "rawtypes" })
    private ManagedContainer verifyContainer(ManagedContainer container, String jmxUsername, String jmxPassword) throws Exception {
        Assert.assertNotNull("ManagedContainer not null", container);
        File containerHome = container.getContainerHome();
        Assert.assertNotNull("Container home not null", containerHome);
        Assert.assertTrue("Container home is dir", containerHome.isDirectory());

        // Start the container
        container.start();

        JMXConnector connector = container.getJMXConnector(jmxUsername, jmxPassword, 20, TimeUnit.SECONDS);
        try {
            // Access containers through JMX
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ContainerManagement cntManagement = ManagementUtils.getMBeanProxy(server, ContainerManagement.OBJECT_NAME, ContainerManagement.class);
            Assert.assertNotNull("ContainerManagement not null", cntManagement);
            Assert.assertTrue("No containers", cntManagement.getContainerIds().isEmpty());

            // Access profiles through JMX
            ProfileVersionManagement prvManagement = ManagementUtils.getMBeanProxy(server, ProfileVersionManagement.OBJECT_NAME, ProfileVersionManagement.class);
            ProfileManagement prfManagement = ManagementUtils.getMBeanProxy(server, ProfileManagement.OBJECT_NAME, ProfileManagement.class);
            Assert.assertNotNull("ProfileManagement not null", prfManagement);
            Assert.assertEquals(1, prvManagement.getProfileVersionIds().size());
            Assert.assertEquals("1.0.0", prvManagement.getProfileVersionIds().iterator().next());
            Assert.assertEquals(1, prfManagement.getProfileIds("1.0").size());
            Assert.assertEquals("default", prfManagement.getProfileIds("1.0").iterator().next());
        } finally {
            connector.close();
        }
        return container;
    }
}
