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
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.RequirementItem;
import io.fabric8.test.smoke.container.ProvisionerTest;
import io.fabric8.test.smoke.sub.a.CamelTransformHttpActivator;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.gravia.Constants;
import org.jboss.gravia.provision.Provisioner;
import org.jboss.gravia.provision.ResourceInstaller;
import org.jboss.gravia.resource.ContentNamespace;
import org.jboss.gravia.resource.IdentityRequirementBuilder;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.resource.Requirement;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceBuilder;
import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.ModuleActivatorBridge;
import org.jboss.gravia.runtime.Runtime;
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
import org.jboss.test.gravia.itests.support.HttpRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.http.HttpService;

/**
 * Test {@link ResourceInstaller} functionality
 *
 * @author thomas.diesler@jboss.com
 * @since 08-May-2014
 */
public abstract class RequirementItemTestBase {

    protected static final String RESOURCE_A = "reqitemA";

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
     * @see ProvisionerTest#testAbstractFeature()
     */
    @Test
    public void testAbstractFeature() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        ContainerIdentity cntId = cntManager.getCurrentContainer().getIdentity();
        Provisioner provisioner = cntManager.getProvisioner(cntId);

        // Build the requirement & client resource
        ResourceIdentity featureId = ResourceIdentity.fromString("camel.core.feature:0.0.0");
        Requirement requirement = new IdentityRequirementBuilder(featureId).getRequirement();
        ResourceIdentity identityA = ResourceIdentity.fromString(RESOURCE_A);
        ResourceBuilder builderA = provisioner.getContentResourceBuilder(identityA, getDeployment(RESOURCE_A));
        Map<String, Object> attsA = builderA.getMutableResource().getIdentityCapability().getAttributes();
        attsA.put(ContentNamespace.CAPABILITY_RUNTIME_NAME_ATTRIBUTE, RESOURCE_A + ".war");
        Resource resourceA = builderA.getResource();

        // Build a profile
        Profile profile = ProfileBuilder.Factory.create("foo")
                .profileVersion(DEFAULT_PROFILE_VERSION)
                .addRequirementItem(requirement)
                .addResourceItem(resourceA)
                .getProfile();

        // Add the profile and verify the item URL
        profile = prfManager.addProfile(DEFAULT_PROFILE_VERSION, profile);
        Assert.assertEquals(1, profile.getProfileItems(RequirementItem.class).size());
        RequirementItem itemA = profile.getProfileItem(featureId.getSymbolicName(), RequirementItem.class);
        Assert.assertNotNull("Requirement not null", itemA.getRequirement());

        // Add the profile to the current coontainer
        Container cnt = cntManager.getCurrentContainer();
        cntManager.addProfiles(cnt.getIdentity(), Collections.singletonList("foo"), null);

        // Make a call to the HttpService endpoint that goes through a Camel route
        if (RuntimeType.OTHER != RuntimeType.getRuntimeType()) {
            String reqspec = "/service?test=Kermit";
            String context = RuntimeType.getRuntimeType() == RuntimeType.KARAF ? "" : "/" + RESOURCE_A;
            Assert.assertEquals("Hello Kermit", performCall(context, reqspec));
        }

        cntManager.removeProfiles(cnt.getIdentity(), Collections.singletonList("foo"), null);
        prfManager.removeProfile(DEFAULT_PROFILE_VERSION, "foo");
    }

    private String performCall(String context, String path) throws Exception {
        return performCall(context, path, null, 2, TimeUnit.SECONDS);
    }

    private String performCall(String context, String path, Map<String, String> headers, long timeout, TimeUnit unit) throws Exception {
        return HttpRequest.get("http://localhost:8080" + context + path, headers, timeout, unit);
    }

    @Deployment(name = RESOURCE_A, managed = false, testable = false)
    public static Archive<?> getResourceA() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, RESOURCE_A + ".war");
        archive.addClasses(AnnotatedProxyServlet.class, AnnotatedProxyListener.class);
        archive.addClasses(AnnotatedContextListener.class, WebAppContextListener.class);
        archive.addClasses(CamelTransformHttpActivator.class, ModuleActivatorBridge.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                if (ArchiveBuilder.getTargetContainer() == RuntimeType.KARAF) {
                    OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                    builder.addBundleManifestVersion(2);
                    builder.addBundleSymbolicName(RESOURCE_A);
                    builder.addBundleActivator(ModuleActivatorBridge.class);
                    builder.addManifestHeader(Constants.MODULE_ACTIVATOR, CamelTransformHttpActivator.class.getName());
                    builder.addImportPackages(ModuleActivatorBridge.class, Runtime.class, Servlet.class, HttpServlet.class, HttpService.class);
                    builder.addImportPackages(CamelContext.class, DefaultCamelContext.class, RouteBuilder.class, RouteDefinition.class);
                    builder.addBundleClasspath("WEB-INF/classes");
                    return builder.openStream();
                } else {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addIdentityCapability(RESOURCE_A, Version.emptyVersion);
                    builder.addManifestHeader(Constants.MODULE_ACTIVATOR, CamelTransformHttpActivator.class.getName());
                    builder.addManifestHeader("Dependencies", "org.apache.camel.core");
                    return builder.openStream();
                }
            }
        });
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml").resolve("org.apache.felix:org.apache.felix.http.proxy").withoutTransitivity().asFile();
        archive.addAsLibraries(libs);
        return archive;
    }
}
