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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ProvisionEvent.EventType;
import io.fabric8.api.ContainerBuilder;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionListener;
import io.fabric8.api.ServiceLocator;
import io.fabric8.test.support.AbstractEmbeddedTest;

import org.jboss.gravia.resource.Version;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test child container functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public class ChildContainerTest extends AbstractEmbeddedTest {

    @Test
    public void testChildContainers() throws Exception {

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
        Assert.assertEquals(1, cntParent.getProfileIds().size());
        Assert.assertTrue(cntParent.getProfileIds().contains(Constants.DEFAULT_PROFILE_IDENTITY));

        // Create child container
        builder = ContainerBuilder.Factory.create(ContainerBuilder.class);
        options = builder.addIdentity("cntB").getCreateOptions();
        Container cntChild = cntManager.createContainer(idParent, options, null);

        // Verify child identity
        ContainerIdentity idChild = cntChild.getIdentity();
        Assert.assertEquals("cntA:cntB", idChild.getSymbolicName());

        // Verify that the child has the default profile assigned
        Assert.assertEquals(Constants.DEFAULT_PROFILE_VERSION, cntChild.getProfileVersion());
        Assert.assertEquals(1, cntChild.getProfileIds().size());
        Assert.assertTrue(cntChild.getProfileIds().contains(Constants.DEFAULT_PROFILE_IDENTITY));

        // Verify that the parent cannot be destroyed
        try {
            cntManager.destroy(idParent);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            // expected
        }

        // Build a new profile version
        Version version20 = Version.parseVersion("2.0");
        ProfileVersionBuilder pvbuilder = ProfileVersionBuilder.Factory.create();
        ProfileVersion profVersion20 = pvbuilder.addIdentity(version20).createProfileVersion();

        // Verify that the version cannot be set
        try {
            cntManager.setVersion(idParent, version20, null);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            // expected
        }

        prfManager.addProfileVersion(profVersion20);

        // Verify that the version can still not be set
        try {
            cntManager.setVersion(idParent, version20, null);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            // expected
        }

        // Build a new profile and associated it with the version
        ProfileBuilder pbuilder = ProfileBuilder.Factory.create();
        Profile default20 = pbuilder.addIdentity("default").createProfile();
        prfManager.addProfile(version20, default20);

        // Setup the provision listener
        final CountDownLatch latch = new CountDownLatch(1);
        ProvisionListener listener = new ProvisionListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                String symbolicName = event.getProfile().getIdentity().getSymbolicName();
                if (event.getType() == EventType.PROVISIONED && "default".equals(symbolicName)) {
                    latch.countDown();
                }
            }
        };

        // Switch the container to version 2.0
        cntManager.setVersion(idParent, version20, listener);
        Assert.assertTrue(latch.await(100, TimeUnit.MILLISECONDS));

        // Create and add another profile
        pbuilder = ProfileBuilder.Factory.create();
        Profile profFoo = pbuilder.addIdentity("foo").createProfile();
        prfManager.addProfile(version20, profFoo);

        // Verify that the profile cannot be added again
        try {
            prfManager.addProfile(version20, profFoo);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            // expected
        }

        // Remove the profile version
        prfManager.removeProfileVersion(version20);

        cntChild = cntManager.destroy(idChild);
        Assert.assertSame(State.DESTROYED, cntChild.getState());
        cntParent = cntManager.destroy(idParent);
        Assert.assertSame(State.DESTROYED, cntParent.getState());

        Assert.assertTrue("No containers", cntManager.getContainers(null).isEmpty());
    }
}
