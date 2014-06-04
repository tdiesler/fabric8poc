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

import io.fabric8.api.process.MutableManagedProcess;
import io.fabric8.api.process.ProcessIdentity;
import io.fabric8.container.karaf.KarafContainerBuilder;
import io.fabric8.container.karaf.KarafCreateOptions;
import io.fabric8.container.karaf.KarafProcessHandler;
import io.fabric8.container.tomcat.TomcatContainerBuilder;
import io.fabric8.container.tomcat.TomcatCreateOptions;
import io.fabric8.container.tomcat.TomcatProcessHandler;
import io.fabric8.container.wildfly.WildFlyContainerBuilder;
import io.fabric8.container.wildfly.WildFlyCreateOptions;
import io.fabric8.container.wildfly.WildFlyProcessHandler;
import io.fabric8.test.embedded.support.EmbeddedTestSupport;

import java.nio.file.Path;
import java.nio.file.Paths;

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
    public void testManagedKaraf() throws Exception {

        KarafCreateOptions options = KarafContainerBuilder.create()
                .identity("ManagedKaraf")
                .targetPath(Paths.get("target", "managed-container"))
                .outputToConsole(true)
                .getCreateOptions();

        KarafProcessHandler handler = new KarafProcessHandler();
        Assert.assertTrue("Handler accepts options", handler.accept(options));

        ProcessIdentity procid = ProcessIdentity.create(options.getIdentity().getSymbolicName());
        MutableManagedProcess process = handler.create(options, procid);
        try {
            verifyContainer(process, "karaf", "karaf");
        } finally {
            handler.destroy(process);
        }
    }

    @Test
    public void testManagedTomcat() throws Exception {

        TomcatCreateOptions options = TomcatContainerBuilder.create()
                .identity("ManagedTomcat")
                .targetPath(Paths.get("target", "managed-container"))
                .outputToConsole(true)
                .getCreateOptions();

        TomcatProcessHandler handler = new TomcatProcessHandler();
        Assert.assertTrue("Handler accepts options", handler.accept(options));

        ProcessIdentity procid = ProcessIdentity.create(options.getIdentity().getSymbolicName());
        MutableManagedProcess process = handler.create(options, procid);
        try {
            verifyContainer(process, "karaf", "karaf");
        } finally {
            handler.destroy(process);
        }
    }

    @Test
    public void testManagedWildFly() throws Exception {

        WildFlyCreateOptions options = WildFlyContainerBuilder.create()
                .identity("ManagedWildFly")
                .targetPath(Paths.get("target", "managed-container"))
                .outputToConsole(true)
                .getCreateOptions();

        WildFlyProcessHandler handler = new WildFlyProcessHandler();
        Assert.assertTrue("Handler accepts options", handler.accept(options));

        ProcessIdentity procid = ProcessIdentity.create(options.getIdentity().getSymbolicName());
        MutableManagedProcess process = handler.create(options, procid);
        try {
            verifyContainer(process, "karaf", "karaf");
        } finally {
            handler.destroy(process);
        }
    }

    @SuppressWarnings({ "rawtypes" })
    private void verifyContainer(MutableManagedProcess process, String jmxUsername, String jmxPassword) throws Exception {
        Assert.assertNotNull("ManagedProcess not null", process);
        Path containerHome = process.getHomePath();
        Assert.assertNotNull("Container home not null", containerHome);
        Assert.assertTrue("Container home is dir", containerHome.toFile().isDirectory());

        /*
        // Start the container
        cnt.start();

        JMXConnector connector = cnt.getJMXConnector(jmxUsername, jmxPassword, 20, TimeUnit.SECONDS);
        try {
            // Access containers through JMX
            MBeanServerConnection server = connector.getMBeanServerConnection();
            ContainerManagement cntManagement = ManagementUtils.getMBeanProxy(server, ContainerManagement.OBJECT_NAME, ContainerManagement.class);
            Assert.assertNotNull("ContainerManagement not null", cntManagement);
            Set<String> containerIds = cntManagement.getContainerIds();
            Assert.assertEquals("One container", 1, containerIds.size());
            ContainerIdentity cntId = ContainerIdentity.create(containerIds.iterator().next());
            Assert.assertEquals(cnt.getIdentity(), cntId);

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
        */
    }
}
