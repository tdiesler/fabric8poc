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
package io.fabric8.test.basic.karaf;


import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerBuilder;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.management.ContainerManagement;
import io.fabric8.api.management.ManagementUtils;
import io.fabric8.api.management.ProfileManagement;
import io.fabric8.container.karaf.KarafContainerBuilder;
import io.fabric8.spi.BootstrapComplete;
import io.fabric8.spi.SystemProperties;
import io.fabric8.test.smoke.PortableTestConditionsTests;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.test.gravia.itests.support.AnnotatedContextListener;
import org.jboss.test.gravia.itests.support.ArchiveBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test basic container functionality.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
@RunWith(Arquillian.class)
public class ManagedContainerLifecycleTest extends PortableTestConditionsTests {

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final ArchiveBuilder archive = new ArchiveBuilder("managed-container-test");
        archive.addClasses(RuntimeType.TOMCAT, AnnotatedContextListener.class);
        archive.addClasses(PortableTestConditionsTests.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                if (ArchiveBuilder.getTargetContainer() == RuntimeType.KARAF) {
                    OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                    builder.addBundleManifestVersion(2);
                    builder.addBundleSymbolicName(archive.getName());
                    builder.addBundleVersion("1.0.0");
                    builder.addImportPackages(RuntimeLocator.class, Resource.class, Container.class);
                    builder.addImportPackages(KarafContainerBuilder.class, ContainerManagement.class, JMXConnector.class);
                    builder.addImportPackages(BootstrapComplete.class);
                    return builder.openStream();
                } else {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addIdentityCapability(archive.getName(), "1.0.0");
                    builder.addManifestHeader("Dependencies", "org.jboss.gravia,io.fabric8.api,io.fabric8.spi");
                    return builder.openStream();
                }
            }
        });
        return archive.getArchive();
    }

    @Test
    public void testManagedKaraf() throws Exception {

        // Build the {@link CreateOptions}
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        String dataDir = (String) runtime.getProperty(SystemProperties.KARAF_DATA);
        ContainerBuilder<?, ?> builder = new KarafContainerBuilder().setTargetDirectory(dataDir);
        CreateOptions options = builder.addIdentity("cntKaraf").getCreateOptions();

        ContainerManager cntManager = ServiceLocator.getRequiredService(ContainerManager.class);
        Container cnt = cntManager.createContainer(options);
        ContainerIdentity cntId = cnt.getIdentity();
        try {
            // Start the container
            cnt = cntManager.startContainer(cntId, null);
            Assert.assertEquals(State.STARTED, cnt.getState());

            JMXConnector connector = ManagementUtils.getJMXConnector(cnt, "karaf", "karaf", 10, TimeUnit.SECONDS);
            try {
                // Access containers through JMX
                MBeanServerConnection server = connector.getMBeanServerConnection();
                ContainerManagement cntManagement = ManagementUtils.getMBeanProxy(server, ContainerManagement.OBJECT_NAME, ContainerManagement.class);
                Assert.assertNotNull("ContainerManagement not null", cntManagement);
                Assert.assertTrue("No containers", cntManagement.getContainerIds().isEmpty());

                // Access profiles through JMX
                ProfileManagement prfManagement = ManagementUtils.getMBeanProxy(server, ProfileManagement.OBJECT_NAME, ProfileManagement.class);
                Assert.assertNotNull("ProfileManagement not null", prfManagement);
                Assert.assertEquals(1, prfManagement.getProfileVersionIds().size());
                Assert.assertEquals("1.0.0", prfManagement.getProfileVersionIds().iterator().next());
                Assert.assertEquals(1, prfManagement.getProfileIds("1.0").size());
                Assert.assertEquals("default", prfManagement.getProfileIds("1.0").iterator().next());
            } finally {
                connector.close();
            }
        } finally {
            cntManager.destroyContainer(cntId);
        }
    }
}
