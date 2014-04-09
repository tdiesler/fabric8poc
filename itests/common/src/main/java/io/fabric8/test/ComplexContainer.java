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
import io.fabric8.core.api.ProfileManager;
import io.fabric8.core.api.ProfileVersion;
import io.fabric8.core.api.ProfileVersionBuilder;
import io.fabric8.core.api.ProvisionEvent;
import io.fabric8.core.api.ProvisionEvent.EventType;
import io.fabric8.core.api.ProvisionEventListener;
import io.fabric8.core.api.ServiceLocator;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.gravia.resource.Version;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test container/profile functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public class ComplexContainer extends PortableTestConditions {

    @Test
    public void testContainersAndProfiles() throws Exception {

        ContainerManager cntManager = ServiceLocator.getRequiredService(ContainerManager.class);
        ProfileManager prfManager = ServiceLocator.getRequiredService(ProfileManager.class);

        // Create parent container
        ContainerBuilder builder = ContainerBuilder.Factory.create(ContainerBuilder.class);
        CreateOptions options = builder.addIdentity("cntA").getCreateOptions();
        Container cntParent = cntManager.createContainer(options);

        // Verify parent identity
        ContainerIdentity idParent = cntParent.getIdentity();
        Assert.assertEquals("cntA", idParent.getSymbolicName());

        // Verify that the parent has the default profile assigned
        Assert.assertEquals(Constants.DEFAULT_PROFILE_VERSION, cntParent.getProfileVersion());
        Assert.assertEquals(1, cntParent.getProfiles().size());
        Assert.assertTrue(cntParent.getProfiles().contains(Constants.DEFAULT_PROFILE_IDENTITY));

        // Build a new profile version
        Version version20 = Version.parseVersion("2.0");
        ProfileVersionBuilder pvbuilder = ProfileVersionBuilder.Factory.create();
        ProfileVersion profVersion20 = pvbuilder.addIdentity(version20).getProfileVersion();

        // Verify that the version cannot be set
        // because it is not registered with the {@link ProfileManager}
        try {
            cntManager.setVersion(idParent, version20, null);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            // expected
        }

        prfManager.addProfileVersion(profVersion20);

        // Verify that the version can still not be set
        // because the contaier uses the default profile
        // which is not yet part of profile 2.0
        try {
            cntManager.setVersion(idParent, version20, null);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            // expected
        }

        // Build a new profile and associated it with 2.0
        ProfileBuilder profileBuilder = ProfileBuilder.Factory.create();
        Profile default20 = profileBuilder.addIdentity("default").getProfile();
        prfManager.addProfile(version20, default20);

        // Setup the provision listener
        final CountDownLatch latchA = new CountDownLatch(1);
        ProvisionEventListener listener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                String symbolicName = event.getProfile().getIdentity().getSymbolicName();
                if (event.getType() == EventType.PROVISIONED && "default".equals(symbolicName)) {
                    latchA.countDown();
                }
            }
        };

        // Switch the container to version 2.0
        cntParent = cntManager.setVersion(idParent, version20, listener);
        Assert.assertTrue("ProvisionEvent received", latchA.await(100, TimeUnit.MILLISECONDS));
        Assert.assertEquals(version20, cntParent.getProfileVersion());
        Assert.assertTrue(cntParent.getProfiles().contains(Constants.DEFAULT_PROFILE_IDENTITY));
        Assert.assertEquals(1, cntParent.getProfiles().size());

        // Create profile foo
        profileBuilder = ProfileBuilder.Factory.create();
        profileBuilder = profileBuilder.addIdentity("foo");
        ConfigurationProfileItemBuilder configBuilder = profileBuilder.getItemBuilder(ConfigurationProfileItemBuilder.class);
        configBuilder.addIdentity(Container.CONTAINER_SERVICE_PID);
        configBuilder.setConfiguration(Collections.singletonMap(Container.CNFKEY_CONFIG_TOKEN, (Object) "bar"));
        profileBuilder.addProfileItem(configBuilder.getProfileItem());
        Profile fooProfile = profileBuilder.getProfile();

        // Verify that the profile cannot be added
        // because the profile version does not yet exist
        try {
            cntManager.addProfiles(idParent, Collections.singleton(fooProfile.getIdentity()), null);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            // expected
        }

        // Add profile foo to 2.0
        prfManager.addProfile(version20, fooProfile);
        Assert.assertEquals(2, prfManager.getProfileIdentities(version20).size());

        // Verify that the profile cannot be added again
        try {
            prfManager.addProfile(version20, fooProfile);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            // expected
        }

        // Setup the provision listener
        final CountDownLatch latchB = new CountDownLatch(1);
        listener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                String symbolicName = event.getProfile().getIdentity().getSymbolicName();
                if (event.getType() == EventType.PROVISIONED && "foo".equals(symbolicName)) {
                    latchB.countDown();
                }
            }
        };

        // Add profile foo to parent container
        cntParent = cntManager.addProfiles(idParent, Collections.singleton(fooProfile.getIdentity()), listener);
        Assert.assertTrue("ProvisionEvent received", latchB.await(100, TimeUnit.MILLISECONDS));
        Assert.assertEquals(version20, cntParent.getProfileVersion());
        Assert.assertTrue(cntParent.getProfiles().contains(Constants.DEFAULT_PROFILE_IDENTITY));
        Assert.assertTrue(cntParent.getProfiles().contains(fooProfile.getIdentity()));
        Assert.assertEquals(2, cntParent.getProfiles().size());

        // Create child container
        builder = ContainerBuilder.Factory.create(ContainerBuilder.class);
        options = builder.addIdentity("cntB").getCreateOptions();
        Container cntChild = cntManager.createContainer(idParent, options, null);

        // Verify child identity
        ContainerIdentity idChild = cntChild.getIdentity();
        Assert.assertEquals("cntA:cntB", idChild.getSymbolicName());
        //Assert.assertEquals("bar", cntChild.getAttribute(Container.ATTKEY_CONFIG_TOKEN));

        // Verify that the child has the default profile assigned
        Assert.assertEquals(Constants.DEFAULT_PROFILE_VERSION, cntChild.getProfileVersion());
        Assert.assertEquals(1, cntChild.getProfiles().size());
        Assert.assertTrue(cntChild.getProfiles().contains(Constants.DEFAULT_PROFILE_IDENTITY));

        // Verify that the profile cannot be removed
        // because it is still used by a container
        try {
            prfManager.removeProfile(version20, fooProfile.getIdentity());
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            // expected
        }

        // Verify that the parent cannot be destroyed
        try {
            cntManager.destroy(idParent);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            // expected
        }

        // Setup the provision listener
        final CountDownLatch latchC = new CountDownLatch(1);
        listener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                String symbolicName = event.getProfile().getIdentity().getSymbolicName();
                if (event.getType() == EventType.REMOVED && "foo".equals(symbolicName)) {
                    latchC.countDown();
                }
            }
        };

        // Remove profile foo from container
        cntParent = cntManager.removeProfiles(idParent, Collections.singleton(fooProfile.getIdentity()), listener);
        Assert.assertTrue("ProvisionEvent received", latchC.await(100, TimeUnit.MILLISECONDS));
        Assert.assertEquals(version20, cntParent.getProfileVersion());
        Assert.assertTrue(cntParent.getProfiles().contains(Constants.DEFAULT_PROFILE_IDENTITY));
        Assert.assertEquals(1, cntParent.getProfiles().size());

        // Remove profile foo from 2.0
        prfManager.removeProfile(version20, fooProfile.getIdentity());
        Assert.assertEquals(1, prfManager.getProfileIdentities(version20).size());

        // Verify that the profile version cannot be removed
        // because it is still used by a container
        try {
            prfManager.removeProfileVersion(version20);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            // expected
        }

        // Setup the provision listener
        final CountDownLatch latchD = new CountDownLatch(2);
        listener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                Profile profile = event.getProfile();
                String version = profile.getProfileVersion().toString();
                String symbolicName = profile.getIdentity().getSymbolicName();
                if (event.getType() == EventType.REMOVED && "2.0.0".equals(version) && "default".equals(symbolicName)) {
                    latchD.countDown();
                }
                if (event.getType() == EventType.PROVISIONED && "1.0.0".equals(version) && "default".equals(symbolicName)) {
                    latchD.countDown();
                }
            }
        };

        // Set the default profile version
        cntParent = cntManager.setVersion(idParent, Constants.DEFAULT_PROFILE_VERSION, listener);
        Assert.assertTrue("ProvisionEvent received", latchD.await(100, TimeUnit.MILLISECONDS));
        Assert.assertEquals(Constants.DEFAULT_PROFILE_VERSION, cntParent.getProfileVersion());
        Assert.assertTrue(cntParent.getProfiles().contains(Constants.DEFAULT_PROFILE_IDENTITY));
        Assert.assertEquals(1, cntParent.getProfiles().size());

        // Remove profile version 2.0
        prfManager.removeProfileVersion(version20);

        cntChild = cntManager.destroy(idChild);
        Assert.assertSame(State.DESTROYED, cntChild.getState());
        cntParent = cntManager.destroy(idParent);
        Assert.assertSame(State.DESTROYED, cntParent.getState());
    }
}
