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
package io.fabric8.test;

import static io.fabric8.core.api.Constants.DEFAULT_PROFILE_VERSION;
import io.fabric8.core.api.ComponentEvent;
import io.fabric8.core.api.ComponentEventListener;
import io.fabric8.core.api.ConfigurationProfileItem;
import io.fabric8.core.api.ConfigurationProfileItemBuilder;
import io.fabric8.core.api.Constants;
import io.fabric8.core.api.Container;
import io.fabric8.core.api.Container.State;
import io.fabric8.core.api.ContainerBuilder;
import io.fabric8.core.api.ContainerIdentity;
import io.fabric8.core.api.ContainerManager;
import io.fabric8.core.api.CreateOptions;
import io.fabric8.core.api.Profile;
import io.fabric8.core.api.ProfileBuilder;
import io.fabric8.core.api.ProfileEvent;
import io.fabric8.core.api.ProfileEventListener;
import io.fabric8.core.api.ProfileManager;
import io.fabric8.core.api.ProfileVersion;
import io.fabric8.core.api.ProfileVersionBuilder;
import io.fabric8.core.api.ProvisionEvent;
import io.fabric8.core.api.ProvisionEventListener;
import io.fabric8.core.api.ServiceLocator;
import io.fabric8.core.spi.ContainerService;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test profile update functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public abstract class ProfileUpdateTests extends PortableTestConditionsTests {

    @Test
    public void testProfileUpdate() throws Exception {

        Version version12 = Version.parseVersion("1.2");

        ProfileVersionBuilder versionBuilder = ProfileVersionBuilder.Factory.create();
        ProfileVersion profileVersion = versionBuilder.addIdentity(version12).getProfileVersion();

        // Add a profile version
        ProfileManager prfManager = ServiceLocator.getRequiredService(ProfileManager.class);
        prfManager.addProfileVersion(profileVersion);
        Assert.assertEquals(2, prfManager.getProfileVersions(null).size());

        // Build a profile
        ProfileBuilder profileBuilder = ProfileBuilder.Factory.create();
        profileBuilder.addIdentity("foo");
        ConfigurationProfileItemBuilder configBuilder = profileBuilder.getItemBuilder(ConfigurationProfileItemBuilder.class);
        configBuilder.addIdentity("some.pid").setConfiguration(Collections.singletonMap("xxx", (Object) "yyy"));
        profileBuilder.addProfileItem(configBuilder.getProfileItem());
        Profile profile = profileBuilder.getProfile();

        // Add the profile to the given version
        profile = prfManager.addProfile(version12, profile);
        Assert.assertEquals(1, prfManager.getProfiles(version12, null).size());

        // Update the profile item
        profileBuilder = ProfileBuilder.Factory.create();
        profileBuilder.addIdentity("foo");
        configBuilder = profileBuilder.getItemBuilder(ConfigurationProfileItemBuilder.class);
        configBuilder.addIdentity("some.pid").setConfiguration(Collections.singletonMap("xxx", (Object) "zzz"));
        Profile updateProfile = profileBuilder.addProfileItem(configBuilder.getProfileItem()).getProfile();

        // Setup the profile listener
        final CountDownLatch latchA = new CountDownLatch(1);
        ProfileEventListener profileListener = new ProfileEventListener() {
            @Override
            public void processEvent(ProfileEvent event) {
                String symbolicName = event.getSource().getIdentity().getSymbolicName();
                if (event.getType() == ProfileEvent.EventType.UPDATED && "foo".equals(symbolicName)) {
                    latchA.countDown();
                }
            }
        };

        // An update on a profile that is not in use by a container does not trigger profile provisioning
        final CountDownLatch latchB = new CountDownLatch(1);
        ProvisionEventListener provisionListener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                String symbolicName = event.getProfile().getIdentity().getSymbolicName();
                if (event.getType() == ProvisionEvent.EventType.REMOVED && "default".equals(symbolicName)) {
                    latchB.countDown();
                }
            }
        };
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();
        ServiceRegistration<ProvisionEventListener> sregB = syscontext.registerService(ProvisionEventListener.class, provisionListener, null);

        profile = prfManager.updateProfile(version12, updateProfile, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.await(100, TimeUnit.MILLISECONDS));
        Assert.assertFalse("ProvisionEvent not received", latchB.await(100, TimeUnit.MILLISECONDS));
        sregB.unregister();

        // Verify profile
        Set<ConfigurationProfileItem> items = profile.getProfileItems(ConfigurationProfileItem.class);
        Assert.assertEquals("One item", 1, items.size());
        ConfigurationProfileItem citem = items.iterator().next();
        Assert.assertEquals("some.pid", citem.getIdentity());
        Assert.assertEquals("zzz", citem.getConfiguration().get("xxx"));

        // Remove profile version
        profileVersion = prfManager.removeProfileVersion(version12);
        Assert.assertEquals(0, profileVersion.getProfiles().size());
    }

    @Test
    public void testProfileUpdateInUse() throws Exception {

        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();

        ContainerManager cntManager = ServiceLocator.getRequiredService(ContainerManager.class);
        ProfileManager prfManager = ServiceLocator.getRequiredService(ProfileManager.class);

        // Create container cntA
        ContainerBuilder cntBuilder = ContainerBuilder.Factory.create(ContainerBuilder.class);
        CreateOptions options = cntBuilder.addIdentity("cntA").getCreateOptions();
        Container cntA = cntManager.createContainer(options);

        // Verify cntA identity
        ContainerIdentity idA = cntA.getIdentity();
        Assert.assertEquals("cntA", idA.getSymbolicName());
        Assert.assertEquals("default", cntA.getAttribute(Container.ATTKEY_CONFIG_TOKEN));

        // Start container cntA
        cntA = cntManager.startContainer(idA, null);
        Assert.assertSame(State.STARTED, cntA.getState());
        Assert.assertEquals(DEFAULT_PROFILE_VERSION, cntA.getProfileVersion());

        ProfileBuilder profileBuilder = ProfileBuilder.Factory.create();
        profileBuilder.addIdentity("default");
        ConfigurationProfileItemBuilder configBuilder = profileBuilder.getItemBuilder(ConfigurationProfileItemBuilder.class);
        configBuilder.addIdentity(Container.CONTAINER_SERVICE_PID);
        configBuilder.setConfiguration(Collections.singletonMap(Container.CNFKEY_CONFIG_TOKEN, (Object) "bar"));
        Profile updateProfile = profileBuilder.addProfileItem(configBuilder.getProfileItem()).getProfile();

        // Setup the profile listener
        final AtomicReference<CountDownLatch> latchA = new AtomicReference<CountDownLatch>(new CountDownLatch(1));
        ProfileEventListener profileListener = new ProfileEventListener() {
            @Override
            public void processEvent(ProfileEvent event) {
                String symbolicName = event.getSource().getIdentity().getSymbolicName();
                if (event.getType() == ProfileEvent.EventType.UPDATED && "default".equals(symbolicName)) {
                    latchA.get().countDown();
                }
            }
        };

        // Setup the provision listener
        final AtomicReference<CountDownLatch> latchB = new AtomicReference<CountDownLatch>(new CountDownLatch(2));
        ProvisionEventListener provisionListener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                String symbolicName = event.getProfile().getIdentity().getSymbolicName();
                if (event.getType() == ProvisionEvent.EventType.REMOVED && "default".equals(symbolicName)) {
                    latchB.get().countDown();
                }
                if (event.getType() == ProvisionEvent.EventType.PROVISIONED && "default".equals(symbolicName)) {
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
        Profile profile = prfManager.updateProfile(Constants.DEFAULT_PROFILE_VERSION, updateProfile, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.get().await(200, TimeUnit.MILLISECONDS));
        Assert.assertTrue("ProvisionEvent received", latchB.get().await(200, TimeUnit.MILLISECONDS));
        Assert.assertTrue("ComponentEvent received", latchC.get().await(200, TimeUnit.MILLISECONDS));
        sregB.unregister();
        sregC.unregister();

        // Verify profile
        Set<ConfigurationProfileItem> items = profile.getProfileItems(ConfigurationProfileItem.class);
        Assert.assertEquals("One item", 1, items.size());
        ConfigurationProfileItem citem = items.iterator().next();
        Assert.assertEquals(Container.CONTAINER_SERVICE_PID, citem.getIdentity());
        Assert.assertEquals("bar", citem.getConfiguration().get(Container.CNFKEY_CONFIG_TOKEN));

        // Create container B
        cntBuilder = ContainerBuilder.Factory.create(ContainerBuilder.class);
        options = cntBuilder.addIdentity("cntB").getCreateOptions();
        Container cntB = cntManager.createContainer(options);

        // Verify child identity
        ContainerIdentity idB = cntB.getIdentity();
        Assert.assertEquals("cntB", idB.getSymbolicName());
        Assert.assertEquals("bar", cntB.getAttribute(Container.ATTKEY_CONFIG_TOKEN));

        cntB = cntManager.destroyContainer(idB);
        Assert.assertSame(State.DESTROYED, cntB.getState());
        cntA = cntManager.destroyContainer(idA);
        Assert.assertSame(State.DESTROYED, cntA.getState());

        // Build an update profile
        profileBuilder = ProfileBuilder.Factory.create();
        profileBuilder.addIdentity("default");
        configBuilder = profileBuilder.getItemBuilder(ConfigurationProfileItemBuilder.class);
        configBuilder.addIdentity(Container.CONTAINER_SERVICE_PID);
        configBuilder.setConfiguration(Collections.singletonMap(Container.CNFKEY_CONFIG_TOKEN, (Object) "default"));
        updateProfile = profileBuilder.addProfileItem(configBuilder.getProfileItem()).getProfile();

        latchA.set(new CountDownLatch(1));
        prfManager.updateProfile(Constants.DEFAULT_PROFILE_VERSION, updateProfile, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.get().await(200, TimeUnit.MILLISECONDS));
    }
}
