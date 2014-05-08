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

import static io.fabric8.api.Constants.DEFAULT_PROFILE_VERSION;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.ResourceItem;
import io.fabric8.test.smoke.suba.SimpleEndpointActivator;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.gravia.Constants;
import org.jboss.gravia.resource.ManifestBuilder;
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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.test.gravia.itests.support.AnnotatedContextListener;
import org.jboss.test.gravia.itests.support.AnnotatedProxyListener;
import org.jboss.test.gravia.itests.support.AnnotatedProxyServlet;
import org.jboss.test.gravia.itests.support.ArchiveBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.http.HttpService;

/**
 * Test profile items functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 08-May-2014
 */
public abstract class ResourceItemsTestBase {

    public static final String DEPLOYMENT_A = "resource-itemA";

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        PrePostConditions.assertPostConditions();
    }

    protected abstract InputStream getDeployment(String name);

    @Test
    public void testImportableResourceItem() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();

        // Get current container
        Container cnt = cntManager.getCurrentContainer();
        Assert.assertEquals(DEFAULT_PROFILE_VERSION, cnt.getProfileVersion());

        // Build a profile
        InputStream inputStream = getDeployment(DEPLOYMENT_A);
        Profile profile = ProfileBuilder.Factory.create("foo")
                .profileVersion(DEFAULT_PROFILE_VERSION)
                .addResourceItem(DEPLOYMENT_A, inputStream)
                .build();

        // Add the profile and verify the item URL
        profile = prfManager.addProfile(DEFAULT_PROFILE_VERSION, profile);
        Assert.assertEquals(1, profile.getProfileItems(ResourceItem.class).size());
        ResourceItem resItem = profile.getProfileItem(DEPLOYMENT_A, ResourceItem.class);
        Assert.assertEquals("profile://1.0.0/foo/resource-itemA", resItem.getURL().toExternalForm());
        Assert.assertNotNull("URL stream not null", resItem.getURL().openStream());

        cntManager.addProfiles(cnt.getIdentity(), Collections.singleton("foo"), null);

        // Verify that the module got installed and is active
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        Module module = runtime.getModule(ResourceIdentity.create(DEPLOYMENT_A, Version.emptyVersion));
        Assert.assertNotNull("Module not null", module);
        Assert.assertEquals(State.ACTIVE, module.getState());

        cntManager.removeProfiles(cnt.getIdentity(), Collections.singleton("foo"), null);
        prfManager.removeProfile(DEFAULT_PROFILE_VERSION, "foo");
    }

    @Deployment(name = DEPLOYMENT_A, managed = false, testable = false)
    public static Archive<?> getDeploymentA() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_A + ".war");
        archive.addClasses(AnnotatedProxyServlet.class, AnnotatedProxyListener.class);
        archive.addClasses(AnnotatedContextListener.class, WebAppContextListener.class);
        archive.addClasses(ModuleActivatorBridge.class, SimpleEndpointActivator.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                if (ArchiveBuilder.getTargetContainer() == RuntimeType.KARAF) {
                    OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                    builder.addBundleManifestVersion(2);
                    builder.addBundleSymbolicName(DEPLOYMENT_A);
                    builder.addBundleActivator(ModuleActivatorBridge.class);
                    builder.addManifestHeader(Constants.MODULE_ACTIVATOR, SimpleEndpointActivator.class.getName());
                    builder.addImportPackages(ModuleActivatorBridge.class, Runtime.class, Servlet.class, HttpServlet.class, HttpService.class);
                    builder.addBundleClasspath("WEB-INF/classes");
                    return builder.openStream();
                } else {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addIdentityCapability(DEPLOYMENT_A, Version.emptyVersion);
                    return builder.openStream();
                }
            }
        });
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml").resolve("org.apache.felix:org.apache.felix.http.proxy").withoutTransitivity().asFile();
        archive.addAsLibraries(libs);
        return archive;
    }
}
