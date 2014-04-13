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
package io.fabric8.test.container;


import io.fabric8.core.api.container.ContainerConfiguration;
import io.fabric8.core.api.container.ManagedContainer;
import io.fabric8.core.api.container.ManagedContainerBuilder;
import io.fabric8.core.api.management.ContainerManagement;
import io.fabric8.core.api.management.ProfileManagement;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.jboss.gravia.utils.MBeanProxy;
import org.junit.Assert;

/**
 * Test basic {@link ManagedContainer} functionality
 *
 * @since 26-Feb-2014
 */
public abstract class ManagedContainerTests {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void verifyContainer(ManagedContainerBuilder<?, ?> builder, String jmxUsername, String jmxPassword) throws Exception {
        ContainerConfiguration configuration = builder.setTargetDirectory("target/managed-container").getConfiguration();
        ManagedContainer container = builder.getManagedContainer();
        Assert.assertNotNull("ManagedContainer not null", container);
        try {
            // Create the container
            container.create(configuration);
            File containerHome = container.getContainerHome();
            Assert.assertNotNull("Container home not null", containerHome);
            Assert.assertTrue("Container home is dir", containerHome.isDirectory());

            // Start the container
            container.start();

            JMXConnector connector = container.getJMXConnector(jmxUsername, jmxPassword, 20, TimeUnit.SECONDS);
            try {
                // Access containers through JMX
                MBeanServerConnection server = connector.getMBeanServerConnection();
                ContainerManagement cntManager = getMBeanProxy(server, ContainerManagement.OBJECT_NAME, ContainerManagement.class);
                Assert.assertNotNull("ContainerManagement not null", cntManager);

//                Assert.assertTrue("No containers", cntManager.getContainerIds().isEmpty());

                // Access profiles through JMX
                ProfileManagement prfManager = getMBeanProxy(server, ProfileManagement.OBJECT_NAME, ProfileManagement.class);
                Assert.assertNotNull("ProfileManagement not null", prfManager);

//                Version version = Version.parseVersion("1.0");
//                ProfileVersion defaultVersion = prfManager.getProfileVersion(version);
//                Assert.assertNotNull("Default profile version", defaultVersion);
//                Assert.assertEquals(1, defaultVersion.getProfiles().size());
//                ProfileIdentity profileIdentity = defaultVersion.getProfiles().iterator().next();
//                Profile defaultProfile = prfManager.getProfile(version, profileIdentity);
//                Assert.assertNotNull("Default profile", defaultProfile);
            } finally {
                connector.close();
            }
        } finally {
            container.destroy();
        }
    }

    private <T> T getMBeanProxy(MBeanServerConnection server, ObjectName oname, Class<T> type) throws IOException {
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

}