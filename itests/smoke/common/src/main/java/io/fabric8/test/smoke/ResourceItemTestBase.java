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

import static io.fabric8.api.Constants.CURRENT_CONTAINER_IDENTITY;
import static io.fabric8.api.Constants.DEFAULT_PROFILE_VERSION;
import static org.jboss.gravia.resource.ContentNamespace.CAPABILITY_INCLUDE_RUNTIME_TYPE_DIRECTIVE;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.ResourceItem;
import io.fabric8.test.smoke.container.ProvisionerTest;
import io.fabric8.test.smoke.sub.a.CamelTransformHttpActivator;
import io.fabric8.test.smoke.sub.a.ModuleActivatorA;
import io.fabric8.test.smoke.sub.a1.ModuleStateA;
import io.fabric8.test.smoke.sub.b.ModuleActivatorB;
import io.fabric8.test.smoke.sub.b1.ModuleStateB;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
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
import org.jboss.gravia.resource.Capability;
import org.jboss.gravia.resource.ContentCapability;
import org.jboss.gravia.resource.ContentNamespace;
import org.jboss.gravia.resource.DefaultResourceBuilder;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.resource.MavenCoordinates;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceBuilder;
import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.Module.State;
import org.jboss.gravia.runtime.ModuleActivatorBridge;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.gravia.runtime.WebAppContextListener;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.test.gravia.itests.support.AnnotatedContextListener;
import org.jboss.test.gravia.itests.support.AnnotatedProxyListener;
import org.jboss.test.gravia.itests.support.AnnotatedProxyServlet;
import org.jboss.test.gravia.itests.support.ArchiveBuilder;
import org.jboss.test.gravia.itests.support.HttpRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.http.HttpService;

/**
 * Test {@link ResourceInstaller} functionality
 *
 * @author thomas.diesler@jboss.com
 * @since 08-May-2014
 */
public abstract class ResourceItemTestBase {

    protected static final String RESOURCE_A = "resitemA";
    protected static final String RESOURCE_B = "resitemB";
    protected static final String RESOURCE_B1 = "resitemB1";
    protected static final String RESOURCE_C = "resitemC";
    protected static final String RESOURCE_F = "resitemF";
    protected static final String CONTENT_F1 = "itemcontF1";
    protected static final String CONTENT_F2 = "itemcontF2";
    protected static final String CONTENT_F3 = "itemcontF3";
    protected static final String RESOURCE_G = "resitemG";
    protected static final String CONTENT_G1 = "itemcontG1";
    protected static final String CONTENT_G2 = "itemcontG2";
    protected static final String CONTENT_G3 = "itemcontG3";

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
     * @see ProvisionerTest#testStreamResource()
     */
    @Test
    public void testStreamResource() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        Provisioner provisioner = cntManager.getProvisioner(CURRENT_CONTAINER_IDENTITY);

        // Build the resitem
        ResourceIdentity identityA = ResourceIdentity.fromString(RESOURCE_A);
        ResourceBuilder builderA = provisioner.getContentResourceBuilder(identityA, getDeployment(RESOURCE_A));
        Resource resourceA = builderA.getResource();

        // Build a profile
        Profile profile = ProfileBuilder.Factory.create("foo")
                .profileVersion(DEFAULT_PROFILE_VERSION)
                .addResourceItem(resourceA)
                .build();

        // Add the profile and verify the item URL
        profile = prfManager.addProfile(DEFAULT_PROFILE_VERSION, profile);
        Assert.assertEquals(1, profile.getProfileItems(ResourceItem.class).size());
        ResourceItem itemA = profile.getProfileItem(identityA);
        Assert.assertEquals("profile://1.0.0/foo/resitemA?version=0.0.0", getItemURL(itemA, 0).toExternalForm());
        Assert.assertNotNull("URL stream not null", new URL("profile://1.0.0/foo/resitemA").openStream());
        Assert.assertNotNull("URL stream not null", getItemURL(itemA, 0).openStream());

        // Add the profile to the current coontainer
        Container cnt = cntManager.getCurrentContainer();
        cntManager.addProfiles(cnt.getIdentity(), Collections.singletonList("foo"), null);

        // Verify that the module got installed
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        Module moduleA = runtime.getModule(identityA);
        Assert.assertNotNull("Module not null", moduleA);
        Assert.assertEquals(State.ACTIVE, moduleA.getState());

        // Verify that the module activator was called
        MBeanServer server = ServiceLocator.getRequiredService(MBeanServer.class);
        Assert.assertTrue("MBean registered", server.isRegistered(getObjectName(moduleA)));
        Assert.assertEquals("ACTIVE" + moduleA, "ACTIVE", server.getAttribute(getObjectName(moduleA), "ModuleState"));

        cntManager.removeProfiles(cnt.getIdentity(), Collections.singletonList("foo"), null);
        prfManager.removeProfile(DEFAULT_PROFILE_VERSION, "foo");
    }

    /**
     * @see ProvisionerTest#testSharedStreamResource()
     */
    @Test
    public void testSharedStreamResource() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        Provisioner provisioner = cntManager.getProvisioner(CURRENT_CONTAINER_IDENTITY);

        // Build the resources
        ResourceIdentity identityB = ResourceIdentity.fromString(RESOURCE_B);
        ResourceBuilder builderB = provisioner.getContentResourceBuilder(identityB, getDeployment(RESOURCE_B));
        Resource resourceB = builderB.getResource();

        ResourceIdentity identityB1 = ResourceIdentity.fromString(RESOURCE_B1);
        ResourceBuilder builderB1 = provisioner.getContentResourceBuilder(identityB1, getDeployment(RESOURCE_B1));
        Resource resourceB1 = builderB1.getResource();

        // Build a profile
        Profile profile = ProfileBuilder.Factory.create("foo")
                .profileVersion(DEFAULT_PROFILE_VERSION)
                .addSharedResourceItem(resourceB)
                .addResourceItem(resourceB1)
                .build();

        // Add the profile and verify the item URL
        profile = prfManager.addProfile(DEFAULT_PROFILE_VERSION, profile);
        Assert.assertEquals(2, profile.getProfileItems(ResourceItem.class).size());
        ResourceItem itemB = profile.getProfileItem(identityB);
        Assert.assertEquals("profile://1.0.0/foo/resitemB?version=0.0.0", getItemURL(itemB, 0).toExternalForm());
        Assert.assertNotNull("URL stream not null", new URL("profile://1.0.0/foo/resitemB").openStream());
        Assert.assertNotNull("URL stream not null", getItemURL(itemB, 0).openStream());
        ResourceItem itemB1 = profile.getProfileItem(identityB1);
        Assert.assertNotNull("URL stream not null", new URL("profile://1.0.0/foo/resitemB1").openStream());
        Assert.assertEquals("profile://1.0.0/foo/resitemB1?version=0.0.0", getItemURL(itemB1, 0).toExternalForm());
        Assert.assertNotNull("URL stream not null", getItemURL(itemB1, 0).openStream());

        // Add the profile to the current coontainer
        Container cnt = cntManager.getCurrentContainer();
        cntManager.addProfiles(cnt.getIdentity(), Collections.singletonList("foo"), null);

        // Verify that the module got installed
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        Module moduleB = runtime.getModule(identityB);
        Assert.assertNotNull("Module not null", moduleB);
        Assert.assertEquals(State.ACTIVE, moduleB.getState());
        Module moduleB1 = runtime.getModule(identityB1);
        Assert.assertNotNull("Module not null", moduleB1);
        Assert.assertEquals(State.ACTIVE, moduleB1.getState());

        // Verify that the module activator was called
        MBeanServer server = ServiceLocator.getRequiredService(MBeanServer.class);
        Assert.assertTrue("MBean registered", server.isRegistered(getObjectName(moduleB1)));
        Assert.assertEquals("ACTIVE" + moduleB, "ACTIVE", server.getAttribute(getObjectName(moduleB1), "ModuleState"));

        cntManager.removeProfiles(cnt.getIdentity(), Collections.singletonList("foo"), null);
        prfManager.removeProfile(DEFAULT_PROFILE_VERSION, "foo");
    }

    /**
     * @see ProvisionerTest#testMavenResource()
     */
    @Test
    public void testMavenResource() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        Provisioner provisioner = cntManager.getProvisioner(CURRENT_CONTAINER_IDENTITY);

        // Tomcat does not support jar deployments
        Assume.assumeFalse(RuntimeType.TOMCAT == RuntimeType.getRuntimeType());

        // Build the resource
        ResourceIdentity identityA = ResourceIdentity.fromString("resitem.camel.core");
        MavenCoordinates mavenid = MavenCoordinates.parse("org.apache.camel:camel-core:jar:2.11.0");
        ResourceBuilder builderA = provisioner.getMavenResourceBuilder(identityA, mavenid);
        Resource resourceA = builderA.getResource();

        // Build a profile
        Profile profile = ProfileBuilder.Factory.create("foo")
                .profileVersion(DEFAULT_PROFILE_VERSION)
                .addResourceItem(resourceA)
                .build();

        // Add the profile and verify the item URL
        profile = prfManager.addProfile(DEFAULT_PROFILE_VERSION, profile);
        Assert.assertEquals(1, profile.getProfileItems(ResourceItem.class).size());
        ResourceItem itemA = profile.getProfileItem(identityA);
        Assert.assertEquals("profile://1.0.0/foo/resitem.camel.core?version=0.0.0", getItemURL(itemA, 0).toExternalForm());
        Assert.assertNotNull("URL stream not null", new URL("profile://1.0.0/foo/resitem.camel.core").openStream());
        Assert.assertNotNull("URL stream not null", getItemURL(itemA, 0).openStream());

        // Add the profile to the current coontainer
        Container cnt = cntManager.getCurrentContainer();
        cntManager.addProfiles(cnt.getIdentity(), Collections.singletonList("foo"), null);

        // Verify that the module got installed
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        Module moduleA = runtime.getModule(identityA);
        Assert.assertNotNull("Module not null", moduleA);
        Assert.assertEquals(State.ACTIVE, moduleA.getState());

        cntManager.removeProfiles(cnt.getIdentity(), Collections.singletonList("foo"), null);
        prfManager.removeProfile(DEFAULT_PROFILE_VERSION, "foo");
    }

    /**
     * @see ProvisionerTest#testSharedMavenResource()
     */
    @Test
    public void testSharedMavenResource() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        Provisioner provisioner = cntManager.getProvisioner(CURRENT_CONTAINER_IDENTITY);

        // Build the resources
        ResourceIdentity identityA = ResourceIdentity.fromString("camel.core.resitem");
        MavenCoordinates mavenid = MavenCoordinates.parse("org.apache.camel:camel-core:jar:2.11.0");
        ResourceBuilder builderA = provisioner.getMavenResourceBuilder(identityA, mavenid);
        builderA.getMutableResource().getIdentityCapability().getAttributes().put(ContentNamespace.CAPABILITY_RUNTIME_NAME_ATTRIBUTE, "camel-core-shared-item-2.11.0.jar");
        builderA.addIdentityRequirement("javax.api");
        builderA.addIdentityRequirement("org.slf4j");
        Resource resourceA = builderA.getResource();

        ResourceIdentity identityC = ResourceIdentity.fromString(RESOURCE_C);
        ResourceBuilder builderC = provisioner.getContentResourceBuilder(identityC, getDeployment(RESOURCE_C));
        Map<String, Object> attsC = builderC.getMutableResource().getIdentityCapability().getAttributes();
        attsC.put(ContentNamespace.CAPABILITY_RUNTIME_NAME_ATTRIBUTE, RESOURCE_C + ".war");
        Resource resourceC = builderC.getResource();

        // Build a profile
        Profile profile = ProfileBuilder.Factory.create("foo")
                .profileVersion(DEFAULT_PROFILE_VERSION)
                .addSharedResourceItem(resourceA)
                .addResourceItem(resourceC)
                .build();

        // Add the profile and verify the item URL
        profile = prfManager.addProfile(DEFAULT_PROFILE_VERSION, profile);
        Assert.assertEquals(2, profile.getProfileItems(ResourceItem.class).size());
        ResourceItem itemA = profile.getProfileItem(identityA);
        Assert.assertEquals("profile://1.0.0/foo/camel.core.resitem?version=0.0.0", getItemURL(itemA, 0).toExternalForm());
        Assert.assertNotNull("URL stream not null", new URL("profile://1.0.0/foo/camel.core.resitem").openStream());
        Assert.assertNotNull("URL stream not null", getItemURL(itemA, 0).openStream());
        ResourceItem itemC = profile.getProfileItem(identityC);
        Assert.assertEquals("profile://1.0.0/foo/resitemC?version=0.0.0", getItemURL(itemC, 0).toExternalForm());
        Assert.assertNotNull("URL stream not null", new URL("profile://1.0.0/foo/resitemC").openStream());
        Assert.assertNotNull("URL stream not null", getItemURL(itemC, 0).openStream());

        // Add the profile to the current coontainer
        Container cnt = cntManager.getCurrentContainer();
        cntManager.addProfiles(cnt.getIdentity(), Collections.singletonList("foo"), null);

        // Make a call to the HttpService endpoint that goes through a Camel route
        if (RuntimeType.OTHER != RuntimeType.getRuntimeType()) {
            String reqspec = "/service?test=Kermit";
            String context = RuntimeType.getRuntimeType() == RuntimeType.KARAF ? "" : "/" + RESOURCE_C;
            Assert.assertEquals("Hello Kermit", performCall(context, reqspec));
        }

        cntManager.removeProfiles(cnt.getIdentity(), Collections.singletonList("foo"), null);
        prfManager.removeProfile(DEFAULT_PROFILE_VERSION, "foo");
    }

    /**
     * Provision a resource with multiple content capabilities to the container shared location.
     * Provision another resource with multiple content capabilities that has a class loader dependency on the first.
     *
     * The client controlls the resource identities
     * The installed resources are self sufficient - no additional dependency mapping needed.
     */
    @Test
    public void testProvisionMultipleContentCapabilities() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();

        // Build the resources
        ResourceBuilder builderF = new DefaultResourceBuilder();
        ResourceIdentity identityF = ResourceIdentity.create(RESOURCE_F, Version.emptyVersion);
        builderF.addIdentityCapability(identityF);
        builderF.addContentCapability(getDeployment(CONTENT_F1), null, Collections.singletonMap(CAPABILITY_INCLUDE_RUNTIME_TYPE_DIRECTIVE, "tomcat"));
        builderF.addContentCapability(getDeployment(CONTENT_F2), null, Collections.singletonMap(CAPABILITY_INCLUDE_RUNTIME_TYPE_DIRECTIVE, "wildfly"));
        builderF.addContentCapability(getDeployment(CONTENT_F3), null, Collections.singletonMap(CAPABILITY_INCLUDE_RUNTIME_TYPE_DIRECTIVE, "karaf,other"));
        Resource resourceF = builderF.getResource();

        ResourceBuilder builderG = new DefaultResourceBuilder();
        ResourceIdentity identityG = ResourceIdentity.create(RESOURCE_G, Version.emptyVersion);
        builderG.addIdentityCapability(identityG);
        builderG.addContentCapability(getDeployment(CONTENT_G1), null, Collections.singletonMap(CAPABILITY_INCLUDE_RUNTIME_TYPE_DIRECTIVE, "tomcat"));
        builderG.addContentCapability(getDeployment(CONTENT_G2), null, Collections.singletonMap(CAPABILITY_INCLUDE_RUNTIME_TYPE_DIRECTIVE, "wildfly"));
        builderG.addContentCapability(getDeployment(CONTENT_G3), null, Collections.singletonMap(CAPABILITY_INCLUDE_RUNTIME_TYPE_DIRECTIVE, "karaf,other"));
        Resource resourceG = builderG.getResource();

        // Build a profile
        Profile profile = ProfileBuilder.Factory.create("foo")
                .profileVersion(DEFAULT_PROFILE_VERSION)
                .addSharedResourceItem(resourceF)
                .addResourceItem(resourceG)
                .build();

        // Add the profile and verify the item URL
        profile = prfManager.addProfile(DEFAULT_PROFILE_VERSION, profile);
        Assert.assertEquals(2, profile.getProfileItems(ResourceItem.class).size());
        ResourceItem itemF = profile.getProfileItem(identityF);
        ResourceItem itemG = profile.getProfileItem(identityG);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals("profile://1.0.0/foo/resitemF?version=0.0.0&cntindex=" + i, getItemURL(itemF, i).toExternalForm());
            Assert.assertNotNull("URL stream not null", new URL("profile://1.0.0/foo/resitemF?cntindex=" + i).openStream());
            Assert.assertNotNull("URL stream not null", getItemURL(itemF, i).openStream());
            Assert.assertEquals("profile://1.0.0/foo/resitemG?version=0.0.0&cntindex=" + i, getItemURL(itemG, i).toExternalForm());
            Assert.assertNotNull("URL stream not null", new URL("profile://1.0.0/foo/resitemG?cntindex=" + i).openStream());
            Assert.assertNotNull("URL stream not null", getItemURL(itemG, i).openStream());
        }

        // Add the profile to the current coontainer
        Container cnt = cntManager.getCurrentContainer();
        cntManager.addProfiles(cnt.getIdentity(), Collections.singletonList("foo"), null);

        // Verify that the module got installed
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        Module moduleG = runtime.getModule(identityG);
        Assert.assertNotNull("Module not null", moduleG);
        Assert.assertEquals(State.ACTIVE, moduleG.getState());

        // Verify that the module activator was called
        MBeanServer server = ServiceLocator.getRequiredService(MBeanServer.class);
        Assert.assertTrue("MBean registered", server.isRegistered(getObjectName(moduleG)));
        Assert.assertEquals("ACTIVE" + moduleG, "ACTIVE", server.getAttribute(getObjectName(moduleG), "ModuleState"));

        cntManager.removeProfiles(cnt.getIdentity(), Collections.singletonList("foo"), null);
        prfManager.removeProfile(DEFAULT_PROFILE_VERSION, "foo");
    }

    private URL getItemURL(ResourceItem item, int index) {
        List<Capability> ccaps = item.getResource().getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        return ccaps.get(index).adapt(ContentCapability.class).getContentURL();
    }
    private ObjectName getObjectName(Module module) throws MalformedObjectNameException {
        ResourceIdentity identity = module.getIdentity();
        return new ObjectName("test:name=" + identity.getSymbolicName() + ",version=" + identity.getVersion());
    }

    private String performCall(String context, String path) throws Exception {
        return performCall(context, path, null, 2, TimeUnit.SECONDS);
    }

    private String performCall(String context, String path, Map<String, String> headers, long timeout, TimeUnit unit) throws Exception {
        return HttpRequest.get("http://localhost:8080" + context + path, headers, timeout, unit);
    }

    @Deployment(name = RESOURCE_A, managed = false, testable = false)
    public static Archive<?> getResourceA() {
        final ArchiveBuilder archive = new ArchiveBuilder(RESOURCE_A);
        archive.addClasses(RuntimeType.TOMCAT, AnnotatedContextListener.class, WebAppContextListener.class);
        archive.addClasses(RuntimeType.KARAF, ModuleActivatorBridge.class);
        archive.addClasses(ModuleActivatorA.class, ModuleStateA.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                if (ArchiveBuilder.getTargetContainer() == RuntimeType.KARAF) {
                    OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                    builder.addBundleManifestVersion(2);
                    builder.addBundleSymbolicName(RESOURCE_A);
                    builder.addBundleActivator(ModuleActivatorBridge.class);
                    builder.addManifestHeader(Constants.MODULE_ACTIVATOR, ModuleActivatorA.class.getName());
                    builder.addImportPackages(Runtime.class, Resource.class, ServiceLocator.class, MBeanServer.class);
                    return builder.openStream();
                } else {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addIdentityCapability(RESOURCE_A, Version.emptyVersion);
                    builder.addManifestHeader(Constants.MODULE_ACTIVATOR, ModuleActivatorA.class.getName());
                    builder.addManifestHeader("Dependencies", "org.jboss.gravia");
                    return builder.openStream();
                }
            }
        });
        return archive.getArchive();
    }

    @Deployment(name = RESOURCE_B, managed = false, testable = false)
    public static Archive<?> getResourceB() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, RESOURCE_B + ".jar");
        archive.addClasses(ModuleStateA.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(RESOURCE_B);
                builder.addExportPackages(ModuleStateA.class);
                builder.addImportPackages(Runtime.class, Resource.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = RESOURCE_B1, managed = false, testable = false)
    public static Archive<?> getResourceB1() {
        final ArchiveBuilder archive = new ArchiveBuilder(RESOURCE_B1);
        archive.addClasses(RuntimeType.TOMCAT, AnnotatedContextListener.class, WebAppContextListener.class);
        archive.addClasses(RuntimeType.KARAF, ModuleActivatorBridge.class);
        archive.addClasses(ModuleActivatorA.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                if (ArchiveBuilder.getTargetContainer() == RuntimeType.KARAF) {
                    OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                    builder.addBundleManifestVersion(2);
                    builder.addBundleSymbolicName(RESOURCE_B1);
                    builder.addBundleActivator(ModuleActivatorBridge.class);
                    builder.addManifestHeader(Constants.MODULE_ACTIVATOR, ModuleActivatorA.class.getName());
                    builder.addImportPackages(Runtime.class, Resource.class, ServiceLocator.class);
                    builder.addImportPackages(MBeanServer.class, ModuleStateA.class);
                    return builder.openStream();
                } else {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addIdentityCapability(RESOURCE_B1, Version.emptyVersion);
                    builder.addManifestHeader(Constants.MODULE_ACTIVATOR, ModuleActivatorA.class.getName());
                    builder.addManifestHeader("Dependencies", "org.jboss.gravia," + RESOURCE_B);
                    return builder.openStream();
                }
            }
        });
        return archive.getArchive();
    }

    @Deployment(name = RESOURCE_C, managed = false, testable = false)
    public static Archive<?> getResourceC() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, RESOURCE_C + ".war");
        archive.addClasses(AnnotatedProxyServlet.class, AnnotatedProxyListener.class);
        archive.addClasses(AnnotatedContextListener.class, WebAppContextListener.class);
        archive.addClasses(CamelTransformHttpActivator.class, ModuleActivatorBridge.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                if (ArchiveBuilder.getTargetContainer() == RuntimeType.KARAF) {
                    OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                    builder.addBundleManifestVersion(2);
                    builder.addBundleSymbolicName(RESOURCE_C);
                    builder.addBundleActivator(ModuleActivatorBridge.class);
                    builder.addManifestHeader(Constants.GRAVIA_ENABLED, Boolean.TRUE.toString());
                    builder.addManifestHeader(Constants.MODULE_ACTIVATOR, CamelTransformHttpActivator.class.getName());
                    builder.addImportPackages(ModuleActivatorBridge.class, Runtime.class, Servlet.class, HttpServlet.class, HttpService.class);
                    builder.addImportPackages(CamelContext.class, DefaultCamelContext.class, RouteBuilder.class, RouteDefinition.class);
                    builder.addBundleClasspath("WEB-INF/classes");
                    return builder.openStream();
                } else {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addIdentityCapability(RESOURCE_C, Version.emptyVersion);
                    builder.addManifestHeader(Constants.MODULE_ACTIVATOR, CamelTransformHttpActivator.class.getName());
                    builder.addManifestHeader("Dependencies", "camel.core.resitem");
                    return builder.openStream();
                }
            }
        });
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml").resolve("org.apache.felix:org.apache.felix.http.proxy").withoutTransitivity().asFile();
        archive.addAsLibraries(libs);
        return archive;
    }

    // Shared Tomcat jar
    @Deployment(name = CONTENT_F1, managed = false, testable = false)
    public static Archive<?> getContentF1() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CONTENT_F1 + ".jar");
        archive.addClasses(ModuleStateB.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addIdentityCapability(RESOURCE_F, Version.emptyVersion);
                return builder.openStream();
            }
        });
        return archive;
    }

    // Shared Wildfly jar
    @Deployment(name = CONTENT_F2, managed = false, testable = false)
    public static Archive<?> getContentF2() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CONTENT_F2 + ".jar");
        archive.addClasses(ModuleStateB.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addIdentityCapability(RESOURCE_F, Version.emptyVersion);
                builder.addManifestHeader("Dependencies", "org.jboss.gravia");
                return builder.openStream();
            }
        });
        return archive;
    }

    // Karaf jar
    @Deployment(name = CONTENT_F3, managed = false, testable = false)
    public static Archive<?> getContentF3() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CONTENT_F3 + ".jar");
        archive.addClasses(ModuleStateB.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(RESOURCE_F);
                builder.addExportPackages(ModuleStateB.class);
                builder.addImportPackages(Runtime.class, Resource.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    // Unshared Tomcat deployment
    @Deployment(name = CONTENT_G1, managed = false, testable = false)
    public static Archive<?> getContentG1() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, CONTENT_G1 + ".war");
        archive.addClasses(AnnotatedContextListener.class, WebAppContextListener.class);
        archive.addClasses(ModuleActivatorB.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addIdentityCapability(RESOURCE_G, Version.emptyVersion);
                builder.addManifestHeader(Constants.MODULE_ACTIVATOR, ModuleActivatorB.class.getName());
                return builder.openStream();
            }
        });
        return archive;
    }

    // Unshared Wildfly deployment
    @Deployment(name = CONTENT_G2, managed = false, testable = false)
    public static Archive<?> getContentG2() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CONTENT_G2 + ".jar");
        archive.addClasses(ModuleActivatorB.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addIdentityCapability(RESOURCE_G, Version.emptyVersion);
                builder.addManifestHeader(Constants.MODULE_ACTIVATOR, ModuleActivatorB.class.getName());
                builder.addManifestHeader("Dependencies", "org.jboss.gravia," + RESOURCE_F);
                return builder.openStream();
            }
        });
        return archive;
    }

    // Karaf deployment
    @Deployment(name = CONTENT_G3, managed = false, testable = false)
    public static Archive<?> getContentG3() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CONTENT_G3 + ".jar");
        archive.addClasses(ModuleActivatorBridge.class, ModuleActivatorB.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(RESOURCE_G);
                builder.addBundleActivator(ModuleActivatorBridge.class);
                builder.addManifestHeader(Constants.MODULE_ACTIVATOR, ModuleActivatorB.class.getName());
                builder.addImportPackages(Runtime.class, Resource.class, ServiceLocator.class);
                builder.addImportPackages(MBeanServer.class, ModuleStateB.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}
