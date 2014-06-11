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

import static io.fabric8.api.Constants.DEFAULT_PROFILE_IDENTITY;
import static io.fabric8.api.Constants.DEFAULT_PROFILE_VERSION;
import static org.jboss.gravia.resource.ContentNamespace.CAPABILITY_INCLUDE_RUNTIME_TYPE_DIRECTIVE;
import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.ConfigurationItemBuilder;
import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ProvisionEvent.EventType;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEventListener;

import java.util.Collections;
import java.util.Dictionary;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Test profile items functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public abstract class ConfigurationItemsTestBase {

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        PrePostConditions.assertPostConditions();
    }

    @Test
    public void testConfigurationItem() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();

        // Get current container
        Container cnt = cntManager.getCurrentContainer();
        Assert.assertSame(State.STARTED, cnt.getState());
        Assert.assertEquals(DEFAULT_PROFILE_VERSION, cnt.getProfileVersion());

        // Build an update profile
        Profile updateProfile = ProfileBuilder.Factory.createFrom(DEFAULT_PROFILE_VERSION, DEFAULT_PROFILE_IDENTITY)
                .addConfigurationItem("some.pid", Collections.singletonMap("foo", (Object) "bar"))
                .getProfile();

        Assert.assertEquals("Two items", 2, updateProfile.getProfileItems(null).size());

        // Setup the profile listener
        final AtomicReference<CountDownLatch> latchA = new AtomicReference<CountDownLatch>(new CountDownLatch(1));
        ProfileEventListener profileListener = new ProfileEventListener() {
            @Override
            public void processEvent(ProfileEvent event) {
                String identity = event.getSource().getIdentity();
                if (event.getType() == ProfileEvent.EventType.UPDATED && "default".equals(identity)) {
                    latchA.get().countDown();
                }
            }
        };

        // Setup the provision listener
        final AtomicReference<CountDownLatch> latchB = new AtomicReference<>(new CountDownLatch(1));
        ProvisionEventListener provisionListener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                String identity = event.getProfile().getIdentity();
                if (event.getType() == EventType.PROVISIONED && "effective#1.0.0[default]".equals(identity)) {
                    latchB.get().countDown();
                }
            }
        };
        ModuleContext syscontext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceRegistration<ProvisionEventListener> sregB = syscontext.registerService(ProvisionEventListener.class, provisionListener, null);

        // Verify that the configuration does not exist
        ConfigurationAdmin configAdmin = ServiceLocator.getRequiredService(ConfigurationAdmin.class);
        Configuration config = configAdmin.getConfiguration("some.pid");
        Assert.assertNull("Configuration null", config.getProperties());

        // Update the default profile
        Profile defaultProfile = prfManager.updateProfile(updateProfile, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.get().await(500, TimeUnit.MILLISECONDS));
        Assert.assertTrue("ProvisionEvent received", latchB.get().await(500, TimeUnit.MILLISECONDS));
        Assert.assertEquals("Two items", 2, defaultProfile.getProfileItems(null).size());

        // Verify the configuration
        config = configAdmin.getConfiguration("some.pid");
        Dictionary<String, Object> props = config.getProperties();
        Assert.assertEquals("bar", props.get("foo"));

        // Build an update profile
        updateProfile = ProfileBuilder.Factory.createFrom(DEFAULT_PROFILE_VERSION, DEFAULT_PROFILE_IDENTITY)
                .removeProfileItem("some.pid")
                .getProfile();

        // Update the default profile
        latchA.set(new CountDownLatch(1));
        latchB.set(new CountDownLatch(1));
        defaultProfile = prfManager.updateProfile(updateProfile, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.get().await(500, TimeUnit.MILLISECONDS));
        Assert.assertTrue("ProvisionEvent received", latchB.get().await(500, TimeUnit.MILLISECONDS));
        Assert.assertEquals("One item", 1, defaultProfile.getProfileItems(null).size());

        sregB.unregister();
    }

    @Test
    public void testMultipleConfigurationItem() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();

        // Build ConfigurationItem with multiple configs
        ConfigurationItem configItem = ConfigurationItemBuilder.Factory.create("some.pid")
            .addConfiguration("tomcat", Collections.singletonMap("some.key", (Object)"For TOMCAT"), Collections.singletonMap(CAPABILITY_INCLUDE_RUNTIME_TYPE_DIRECTIVE, "tomcat"))
            .addConfiguration("wildfly", Collections.singletonMap("some.key", (Object)"For WILDFLY"), Collections.singletonMap(CAPABILITY_INCLUDE_RUNTIME_TYPE_DIRECTIVE, "wildfly"))
            .addConfiguration("karaf", Collections.singletonMap("some.key", (Object)"For KARAF"), Collections.singletonMap(CAPABILITY_INCLUDE_RUNTIME_TYPE_DIRECTIVE, "karaf"))
            .getConfigurationItem();

        // Build profile
        Profile profile = ProfileBuilder.Factory.create("foo")
                .addProfileItem(configItem)
                .getProfile();

        // Build a profile version
        Version version = Version.parseVersion("1.2");
        ProfileVersion profileVersion = ProfileVersionBuilder.Factory.create(version)
                .addProfile(ProfileBuilder.Factory.create(DEFAULT_PROFILE_IDENTITY).getProfile())
                .addProfile(profile)
                .getProfileVersion();

        // Add the profile version
        prfManager.addProfileVersion(profileVersion);

        // Setup the provision listener
        final CountDownLatch latchA = new CountDownLatch(1);
        final CountDownLatch latchB = new CountDownLatch(1);
        ProvisionEventListener listener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                String identity = event.getProfile().getIdentity();
                if (event.getType() == EventType.PROVISIONED && "effective#1.2.0[default]".equals(identity)) {
                    latchA.countDown();
                }
                if (event.getType() == EventType.PROVISIONED && "effective#1.2.0[default,foo]".equals(identity)) {
                    latchB.countDown();
                }
            }
        };

        // Switch the current container to 1.2
        ContainerIdentity cntIdentity = cntManager.getCurrentContainer().getIdentity();
        cntManager.setProfileVersion(cntIdentity, version, listener);
        Assert.assertTrue("ProvisionEvent received", latchA.await(500, TimeUnit.MILLISECONDS));

        // Add profile foo to the current container
        cntManager.addProfiles(cntIdentity, Collections.singletonList("foo"), listener);
        Assert.assertTrue("ProvisionEvent received", latchB.await(500, TimeUnit.MILLISECONDS));

        // Verify runtime specific configuration
        ConfigurationAdmin configAdmin = ServiceLocator.getRequiredService(ConfigurationAdmin.class);
        Configuration config = configAdmin.getConfiguration("some.pid", null);
        Object configValue = config.getProperties().get("some.key");
        if (RuntimeType.OTHER != RuntimeType.getRuntimeType()) {
            Assert.assertEquals("For " + RuntimeType.getRuntimeType(), configValue);
        }

        // Restore defaults
        cntManager.removeProfiles(cntIdentity, Collections.singletonList("foo"), null);
        cntManager.setProfileVersion(cntIdentity, DEFAULT_PROFILE_VERSION, null);
        prfManager.removeProfileVersion(version);
    }
}
