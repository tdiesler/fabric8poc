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
import io.fabric8.api.ComponentEvent;
import io.fabric8.api.ComponentEventListener;
import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.api.VersionIdentity;
import io.fabric8.spi.ContainerService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test profile update functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public abstract class ProfileUpdateTestBase  {

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        PrePostConditions.assertPostConditions();
    }

    @Test
    public void testProfileUpdate() throws Exception {

        final VersionIdentity version12 = VersionIdentity.createFrom("1.2");
        final ProfileIdentity identityA = ProfileIdentity.createFrom("foo");

        // Build a profile version
        Profile prfA = ProfileBuilder.Factory.create(identityA)
                .addConfigurationItem("some.pid", Collections.singletonMap("xxx", (Object) "yyy"))
                .getProfile();

        ProfileVersion profileVersion = ProfileVersionBuilder.Factory.create(version12)
                .addProfile(ProfileBuilder.Factory.create(DEFAULT_PROFILE_IDENTITY).getProfile())
                .addProfile(prfA)
                .getProfileVersion();

        // Add a profile version
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        prfManager.addProfileVersion(profileVersion);
        Assert.assertEquals(2, prfManager.getProfileVersions(null).size());

        // Verify that the profile also got added
        Profile profile = prfManager.getProfile(version12, identityA);
        Assert.assertNotNull("Profile added", profile);
        Assert.assertEquals(identityA, profile.getIdentity());
        Assert.assertEquals(1, profile.getProfileItems(null).size());
        ConfigurationItem profileItem = profile.getProfileItem("some.pid", ConfigurationItem.class);
        Assert.assertEquals("yyy", profileItem.getDefaultAttributes().get("xxx"));

        Profile updateProfile = ProfileBuilder.Factory.createFrom(version12, identityA)
                .addConfigurationItem("some.pid", Collections.singletonMap("xxx", (Object) "zzz"))
                .getProfile();

        // Verify update profile
        Assert.assertEquals(identityA, updateProfile.getIdentity());
        Assert.assertEquals(1, updateProfile.getProfileItems(null).size());
        profileItem = updateProfile.getProfileItem("some.pid", ConfigurationItem.class);
        Assert.assertEquals("zzz", profileItem.getDefaultAttributes().get("xxx"));

        // Setup the profile listener
        final CountDownLatch latchA = new CountDownLatch(1);
        ProfileEventListener profileListener = new ProfileEventListener() {
            @Override
            public void processEvent(ProfileEvent event) {
                ProfileIdentity prfid = event.getSource().getIdentity();
                if (event.getType() == ProfileEvent.EventType.UPDATED && identityA.equals(prfid)) {
                    latchA.countDown();
                }
            }
        };

        // An update on a profile that is not in use by a container does not trigger profile provisioning
        final CountDownLatch latchB = new CountDownLatch(1);
        ProvisionEventListener provisionListener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                ProfileIdentity identity = event.getProfile().getIdentity();
                if (event.getType() == ProvisionEvent.EventType.PROVISIONED && "default".equals(identity)) {
                    latchB.countDown();
                }
            }
        };
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();
        ServiceRegistration<ProvisionEventListener> sregB = syscontext.registerService(ProvisionEventListener.class, provisionListener, null);

        profile = prfManager.updateProfile(updateProfile, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.await(1, TimeUnit.SECONDS));
        Assert.assertFalse("ProvisionEvent not received", latchB.await(1, TimeUnit.SECONDS));
        sregB.unregister();

        // Verify profile
        List<ConfigurationItem> items = profile.getProfileItems(ConfigurationItem.class);
        Assert.assertEquals("One item", 1, items.size());
        ConfigurationItem citem = items.iterator().next();
        Assert.assertEquals("some.pid", citem.getIdentity());
        Assert.assertEquals("zzz", citem.getDefaultAttributes().get("xxx"));

        // Remove profile version
        profileVersion = prfManager.removeProfileVersion(version12);
    }

    @Test
    public void testProfileUpdateInUse() throws Exception {

        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();

        // Get current container
        Container cntA = cntManager.getCurrentContainer();
        Assert.assertSame(State.STARTED, cntA.getState());
        Assert.assertEquals(DEFAULT_PROFILE_VERSION, cntA.getProfileVersion());

        Profile updateProfile = ProfileBuilder.Factory.createFrom(DEFAULT_PROFILE_VERSION, DEFAULT_PROFILE_IDENTITY)
                .addConfigurationItem(Container.CONTAINER_SERVICE_PID, Collections.singletonMap("config.token", (Object) "bar"))
                .getProfile();

        // Setup the profile listener
        final AtomicReference<CountDownLatch> latchA = new AtomicReference<CountDownLatch>(new CountDownLatch(1));
        ProfileEventListener profileListener = new ProfileEventListener() {
            @Override
            public void processEvent(ProfileEvent event) {
                String identity = event.getSource().getIdentity().getCanonicalForm();
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
                String identity = event.getProfile().getIdentity().getCanonicalForm();
                if (event.getType() == ProvisionEvent.EventType.PROVISIONED && "effective#1.0.0-default".equals(identity)) {
                    latchB.get().countDown();
                }
            }
        };
        ServiceRegistration<ProvisionEventListener> sregB = syscontext.registerService(ProvisionEventListener.class, provisionListener, null);

        // Setup the component listener
        final AtomicReference<CountDownLatch> latchC = new AtomicReference<CountDownLatch>(new CountDownLatch(2));
        ComponentEventListener componentListener = new ComponentEventListener() {
            @Override
            public void processEvent(ComponentEvent event) {
                Class<?> compType = event.getSource();
                if (event.getType() == ComponentEvent.EventType.DEACTIVATED && ContainerService.class.isAssignableFrom(compType)) {
                    latchC.get().countDown();
                }
                if (event.getType() == ComponentEvent.EventType.ACTIVATED && ContainerService.class.isAssignableFrom(compType)) {
                    latchC.get().countDown();
                }
            }
        };
        ServiceRegistration<ComponentEventListener> sregC = syscontext.registerService(ComponentEventListener.class, componentListener, null);

        // Update the default profile
        Profile profile = prfManager.updateProfile(updateProfile, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.get().await(1, TimeUnit.SECONDS));
        Assert.assertTrue("ProvisionEvent received", latchB.get().await(1, TimeUnit.SECONDS));
        Assert.assertTrue("ComponentEvent received", latchC.get().await(1, TimeUnit.SECONDS));
        sregB.unregister();
        sregC.unregister();

        // Verify profile
        List<ConfigurationItem> items = profile.getProfileItems(ConfigurationItem.class);
        Assert.assertEquals("One item", 1, items.size());
        ConfigurationItem citem = items.iterator().next();
        Assert.assertEquals(Container.CONTAINER_SERVICE_PID, citem.getIdentity());
        Assert.assertEquals("bar", citem.getDefaultAttributes().get("config.token"));

        // Build an update profile
        updateProfile =  ProfileBuilder.Factory.createFrom(DEFAULT_PROFILE_VERSION, DEFAULT_PROFILE_IDENTITY)
                .addConfigurationItem(Container.CONTAINER_SERVICE_PID, Collections.singletonMap("config.token", (Object) "default"))
                .getProfile();

        latchA.set(new CountDownLatch(1));
        prfManager.updateProfile(updateProfile, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.get().await(1, TimeUnit.SECONDS));
    }
}
