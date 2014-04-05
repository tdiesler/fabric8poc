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

import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.ConfigurationItemBuilder;
import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerBuilder;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.Container.State;
import io.fabric8.api.ProvisionEvent.Type;
import io.fabric8.test.support.AbstractEmbeddedTest;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test basic profiles functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public class BasicProfilesTest extends AbstractEmbeddedTest {

    @Test
    public void testProfileAddRemove() throws Exception {

        // Verify the default profile
        ProfileManager prfManager = ServiceLocator.getRequiredService(ProfileManager.class);
        Profile defaultProfile = prfManager.getDefaultProfile();
        Assert.assertEquals(Constants.DEFAULT_PROFILE_VERSION, defaultProfile.getProfileVersion());
        Assert.assertEquals(Constants.DEFAULT_PROFILE_IDENTITY, defaultProfile.getIdentity());

        Set<ProfileVersion> versions = prfManager.getProfileVersions(null);
        Assert.assertEquals("One version", 1, versions.size());

        ProfileVersion defaultVersion = prfManager.getProfileVersion(Constants.DEFAULT_PROFILE_VERSION);
        Set<ProfileIdentity> profileIdentities = defaultVersion.getProfileIds();
        Assert.assertEquals("One profile", 1, profileIdentities.size());
        Assert.assertEquals(Constants.DEFAULT_PROFILE_IDENTITY, profileIdentities.iterator().next());

        Version version = Version.parseVersion("1.1");

        ProfileVersionBuilder versionBuilder = ProfileVersionBuilder.Factory.create();
        ProfileVersion profileVersion = versionBuilder.addIdentity(version).createProfileVersion();

        // Add a profile version
        prfManager.addProfileVersion(profileVersion);
        Assert.assertEquals(2, prfManager.getProfileVersions(null).size());

        // Build a profile
        ProfileBuilder profileBuilder = ProfileBuilder.Factory.create();
        profileBuilder.addIdentity("foo");
        ConfigurationItemBuilder ibuilder = profileBuilder.getItemBuilder(ConfigurationItemBuilder.class);
        ibuilder.addIdentity("some.pid").setConfiguration(Collections.singletonMap("xxx", (Object) "yyy"));
        profileBuilder.addProfileItem(ibuilder.getConfigurationItem());
        Profile profile = profileBuilder.createProfile();

        // Verify profile
        Set<ConfigurationItem> items = profile.getProfileItems(ConfigurationItem.class);
        Assert.assertEquals("One item", 1, items.size());
        ConfigurationItem citem = items.iterator().next();
        Assert.assertEquals("some.pid", citem.getIdentity());
        Assert.assertEquals("yyy", citem.getConfiguration().get("xxx"));

        // Add the profile to the given version
        profile = prfManager.addProfile(version, profile);
        Assert.assertEquals(1, prfManager.getProfiles(version, null).size());

        // Remove profile version
        profileVersion = prfManager.removeProfileVersion(version);
        Assert.assertEquals(0, profileVersion.getProfileIds().size());
    }

    @Test
    public void testProfileUpdate() throws Exception {

        Version version = Version.parseVersion("1.2");

        ProfileVersionBuilder versionBuilder = ProfileVersionBuilder.Factory.create();
        ProfileVersion profileVersion = versionBuilder.addIdentity(version).createProfileVersion();

        // Add a profile version
        ProfileManager prfManager = ServiceLocator.getRequiredService(ProfileManager.class);
        prfManager.addProfileVersion(profileVersion);
        Assert.assertEquals(2, prfManager.getProfileVersions(null).size());

        // Build a profile
        ProfileBuilder profileBuilder = ProfileBuilder.Factory.create();
        profileBuilder.addIdentity("foo");
        ConfigurationItemBuilder configBuilder = profileBuilder.getItemBuilder(ConfigurationItemBuilder.class);
        configBuilder.addIdentity("some.pid").setConfiguration(Collections.singletonMap("xxx", (Object) "yyy"));
        profileBuilder.addProfileItem(configBuilder.getConfigurationItem());
        Profile profile = profileBuilder.createProfile();

        // Add the profile to the given version
        profile = prfManager.addProfile(version, profile);
        Assert.assertEquals(1, prfManager.getProfiles(version, null).size());

        // Update the profile item
        profileBuilder = ProfileBuilder.Factory.create();
        configBuilder = profileBuilder.getItemBuilder(ConfigurationItemBuilder.class);
        configBuilder.addIdentity("some.pid").setConfiguration(Collections.singletonMap("xxx", (Object) "zzz"));
        ConfigurationItem item = configBuilder.getConfigurationItem();
        Set<ConfigurationItem> items = Collections.singleton(item);

        // Setup the profile listener
        final CountDownLatch latchA = new CountDownLatch(1);
        ProfileEventListener profileListener = new ProfileEventListener() {
            @Override
            public void processEvent(ProfileEvent event) {
                String symbolicName = event.getProfile().getIdentity().getSymbolicName();
                if (event.getType() == ProfileEvent.Type.UPDATED && "foo".equals(symbolicName)) {
                    latchA.countDown();
                }
            }
        };

        profile = prfManager.updateProfile(version, profile.getIdentity(), items, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.await(100, TimeUnit.MILLISECONDS));

        // Verify profile
        items = profile.getProfileItems(ConfigurationItem.class);
        Assert.assertEquals("One item", 1, items.size());
        ConfigurationItem citem = items.iterator().next();
        Assert.assertEquals("some.pid", citem.getIdentity());
        Assert.assertEquals("zzz", citem.getConfiguration().get("xxx"));

        // Remove profile version
        profileVersion = prfManager.removeProfileVersion(version);
        Assert.assertEquals(0, profileVersion.getProfileIds().size());
    }

    @Test
    public void testProfileUpdateInUse() throws Exception {

        ContainerManager cntManager = ServiceLocator.getRequiredService(ContainerManager.class);
        ProfileManager prfManager = ServiceLocator.getRequiredService(ProfileManager.class);

        // Create container A
        ContainerBuilder cntBuilder = ContainerBuilder.Factory.create(ContainerBuilder.class);
        CreateOptions options = cntBuilder.addIdentity("cntA").getCreateOptions();
        Container cntA = cntManager.createContainer(options);

        // Verify parent identity
        ContainerIdentity idA = cntA.getIdentity();
        Assert.assertEquals("cntA", idA.getSymbolicName());
        Assert.assertEquals("default", cntA.getAttribute(Container.ATTKEY_CONFIG_TOKEN));

        ProfileBuilder profileBuilder = ProfileBuilder.Factory.create();
        ConfigurationItemBuilder configBuilder = profileBuilder.getItemBuilder(ConfigurationItemBuilder.class);
        configBuilder.addIdentity(Container.CONTAINER_SERVICE_PID);
        configBuilder.setConfiguration(Collections.singletonMap(Container.CNFKEY_CONFIG_TOKEN, (Object) "bar"));
        ConfigurationItem item = configBuilder.getConfigurationItem();
        Set<ConfigurationItem> items = Collections.singleton(item);

        // Setup the profile listener
        final CountDownLatch latchA = new CountDownLatch(1);
        ProfileEventListener profileListener = new ProfileEventListener() {
            @Override
            public void processEvent(ProfileEvent event) {
                String symbolicName = event.getProfile().getIdentity().getSymbolicName();
                if (event.getType() == ProfileEvent.Type.UPDATED && "default".equals(symbolicName)) {
                    latchA.countDown();
                }
            }
        };

        // Setup the provision listener
        final CountDownLatch latchB = new CountDownLatch(2);
        ProvisionEventListener listener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                String symbolicName = event.getProfile().getIdentity().getSymbolicName();
                if (event.getType() == Type.REMOVED && "default".equals(symbolicName)) {
                    latchB.countDown();
                }
                if (event.getType() == Type.PROVISIONED && "default".equals(symbolicName)) {
                    latchB.countDown();
                }
            }
        };
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();
        syscontext.registerService(ProvisionEventListener.class, listener, null);

        Profile profile = prfManager.updateProfile(Constants.DEFAULT_PROFILE_VERSION, Constants.DEFAULT_PROFILE_IDENTITY, items, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.await(100, TimeUnit.MILLISECONDS));
        Assert.assertTrue("ProvisionEvent received", latchB.await(100, TimeUnit.MILLISECONDS));

        // Verify profile
        items = profile.getProfileItems(ConfigurationItem.class);
        Assert.assertEquals("One item", 1, items.size());
        ConfigurationItem citem = items.iterator().next();
        Assert.assertEquals(Container.CONTAINER_SERVICE_PID, citem.getIdentity());
        Assert.assertEquals("bar", citem.getConfiguration().get(Container.CNFKEY_CONFIG_TOKEN));

        // [TODO] ContainerManger may aquire a permit on ContainerService while the ContainerService service is already unregistered but the ContainerService component not yet deactivated.
        Thread.sleep(100);

        // Create container B
        cntBuilder = ContainerBuilder.Factory.create(ContainerBuilder.class);
        options = cntBuilder.addIdentity("cntB").getCreateOptions();
        Container cntB = cntManager.createContainer(options);

        // Verify child identity
        ContainerIdentity idB = cntB.getIdentity();
        Assert.assertEquals("cntB", idB.getSymbolicName());
        Assert.assertEquals("bar", cntB.getAttribute(Container.ATTKEY_CONFIG_TOKEN));

        cntB = cntManager.destroy(idB);
        Assert.assertSame(State.DESTROYED, cntB.getState());
        cntA = cntManager.destroy(idA);
        Assert.assertSame(State.DESTROYED, cntA.getState());

        Assert.assertTrue("No containers", cntManager.getContainers(null).isEmpty());
    }
}
