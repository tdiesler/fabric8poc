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

import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.api.ServiceLocator;
import io.fabric8.spi.DefaultContainerBuilder;

import java.util.Collections;
import java.util.Dictionary;
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
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Test profile items functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public abstract class ProfileItemsTestBase {

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

        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        Profile defaultProfile = prfManager.getDefaultProfile();

        // Create container A
        DefaultContainerBuilder cntBuilder = DefaultContainerBuilder.create();
        CreateOptions options = cntBuilder.identityPrefix("cntA").build();
        Container cntA = cntManager.createContainer(options);
        ContainerIdentity cntIdA = cntA.getIdentity();

        // Verify identityA
        Assert.assertTrue(cntIdA.getSymbolicName().startsWith("cntA#"));

        // Start container A
        cntA = cntManager.startContainer(cntIdA, null);
        Assert.assertSame(State.STARTED, cntA.getState());
        Assert.assertEquals(DEFAULT_PROFILE_VERSION, cntA.getProfileVersion());

        // Build an update profile
        Profile updateProfile = ProfileBuilder.Factory.createFrom(DEFAULT_PROFILE_VERSION, DEFAULT_PROFILE_IDENTITY)
                .addConfigurationItem("some.pid", Collections.singletonMap("foo", (Object) "bar"))
                .build();

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
        final AtomicReference<CountDownLatch> latchB = new AtomicReference<CountDownLatch>(new CountDownLatch(2));
        ProvisionEventListener provisionListener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                String identity = event.getProfile().getIdentity();
                if (event.getType() == ProvisionEvent.EventType.REMOVED && "default".equals(identity)) {
                    latchB.get().countDown();
                }
                if (event.getType() == ProvisionEvent.EventType.PROVISIONED && "default".equals(identity)) {
                    latchB.get().countDown();
                }
            }
        };
        ServiceRegistration<ProvisionEventListener> sregB = syscontext.registerService(ProvisionEventListener.class, provisionListener, null);

        // Verify that the configuration does not exist
        ConfigurationAdmin configAdmin = ServiceLocator.getRequiredService(ConfigurationAdmin.class);
        Configuration config = configAdmin.getConfiguration("some.pid");
        Assert.assertNull("Configuration null", config.getProperties());

        // Update the default profile
        defaultProfile = prfManager.updateProfile(updateProfile, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.get().await(200, TimeUnit.MILLISECONDS));
        Assert.assertTrue("ProvisionEvent received", latchB.get().await(200, TimeUnit.MILLISECONDS));
        Assert.assertEquals("One item", 2, defaultProfile.getProfileItems(null).size());

        // Verify the configuration
        config = configAdmin.getConfiguration("some.pid");
        Dictionary<String, Object> props = config.getProperties();
        Assert.assertEquals("bar", props.get("foo"));

        // Build an update profile
        updateProfile = ProfileBuilder.Factory.createFrom(DEFAULT_PROFILE_VERSION, DEFAULT_PROFILE_IDENTITY)
                .removeProfileItem("some.pid")
                .build();

        // Update the default profile
        latchA.set(new CountDownLatch(1));
        latchB.set(new CountDownLatch(2));
        defaultProfile = prfManager.updateProfile(updateProfile, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.get().await(200, TimeUnit.MILLISECONDS));
        Assert.assertTrue("ProvisionEvent received", latchB.get().await(200, TimeUnit.MILLISECONDS));
        Assert.assertEquals("One item", 1, defaultProfile.getProfileItems(null).size());

        sregB.unregister();

        cntA = cntManager.destroyContainer(cntIdA);
        Assert.assertSame(State.DESTROYED, cntA.getState());
    }
}
