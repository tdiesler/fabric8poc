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
import io.fabric8.api.ResourceItem;
import io.fabric8.api.ServiceLocator;
import io.fabric8.test.smoke.suba.SimpleModuleActivator;

import java.io.InputStream;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.gravia.Constants;
import org.jboss.gravia.provision.Provisioner;
import org.jboss.gravia.provision.ResourceHandle;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.Module.State;
import org.jboss.gravia.runtime.ModuleActivatorBridge;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.runtime.WebAppContextListener;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.test.gravia.itests.support.AnnotatedContextListener;
import org.jboss.test.gravia.itests.support.ArchiveBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test {@link ResourceItem} functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 08-May-2014
 */
public abstract class ResourceInstallTestBase {

    public static final String DEPLOYMENT_A = "resourceA";

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        PrePostConditions.assertPostConditions();
    }

    protected abstract InputStream getDeployment(String name);

    /**
     * Install a shared resource from input stream
     */
    @Test
    @Ignore
    public void testSharedStreamResource() throws Exception {
    }

    /**
     * Install a shared resource from maven coordinates
     */
    @Test
    @Ignore
    public void testSharedMavenResource() throws Exception {
    }

    /**
     * Install a unshared resource from input stream
     */
    @Test
    public void testUnsharedStreamResource() throws Exception {
        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();

        // Get current container
        Container cnt = cntManager.getCurrentContainer();
        Provisioner provisioner = cntManager.getProvisioner(cnt.getIdentity());

        InputStream inputStream = getDeployment(DEPLOYMENT_A);
        ResourceHandle handle = provisioner.installResource(DEPLOYMENT_A, inputStream, null);
        try {
            // Verify that the module got installed
            Runtime runtime = RuntimeLocator.getRequiredRuntime();
            Module module = runtime.getModule(ResourceIdentity.create(DEPLOYMENT_A, Version.emptyVersion));
            Assert.assertNotNull("Module not null", module);
            Assert.assertEquals(State.ACTIVE, module.getState());

            // Verify that the module activator was called
            MBeanServer server = ServiceLocator.getRequiredService(MBeanServer.class);
            Assert.assertTrue("MBean registered", server.isRegistered(getObjectName(module)));
            Assert.assertEquals(State.ACTIVE, server.getAttribute(getObjectName(module), "ModuleState"));
        } finally {
            handle.uninstall();
        }
    }

    /**
     * Install a unshared resource from maven coordinates
     */
    @Test
    @Ignore
    public void testUnsharedMavenResource() throws Exception {
    }

    private static ObjectName getObjectName(Module module) throws MalformedObjectNameException {
        ResourceIdentity identity = module.getIdentity();
        return new ObjectName("test:name=" + identity.getSymbolicName() + ",version=" + identity.getVersion());
    }

    @Deployment(name = DEPLOYMENT_A, managed = false, testable = false)
    public static Archive<?> getDeploymentA() {
        final ArchiveBuilder archive = new ArchiveBuilder(DEPLOYMENT_A);
        archive.addClasses(RuntimeType.TOMCAT, AnnotatedContextListener.class, WebAppContextListener.class);
        archive.addClasses(ModuleActivatorBridge.class, SimpleModuleActivator.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                if (ArchiveBuilder.getTargetContainer() == RuntimeType.KARAF) {
                    OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                    builder.addBundleManifestVersion(2);
                    builder.addBundleSymbolicName(DEPLOYMENT_A);
                    builder.addBundleActivator(ModuleActivatorBridge.class);
                    builder.addManifestHeader(Constants.MODULE_ACTIVATOR, SimpleModuleActivator.class.getName());
                    builder.addImportPackages(Runtime.class, Resource.class, ServiceLocator.class, MBeanServer.class);
                    return builder.openStream();
                } else {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addIdentityCapability(DEPLOYMENT_A, Version.emptyVersion);
                    builder.addManifestHeader(Constants.MODULE_ACTIVATOR, SimpleModuleActivator.class.getName());
                    builder.addManifestHeader("Dependencies", "org.jboss.gravia,io.fabric8.api");
                    return builder.openStream();
                }
            }
        });
        return archive.getArchive();
    }
}
