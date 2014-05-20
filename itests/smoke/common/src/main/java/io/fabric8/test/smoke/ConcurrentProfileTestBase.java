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
import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.Constants;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.LinkedProfile;
import io.fabric8.api.LockHandle;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEvent.EventType;
import io.fabric8.api.ProvisionEventListener;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.ServiceLocator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Test concurrent access to profiles
 *
 * One thread continuesly modifies the profile that is associated with a container
 *
 * Another threads adds/removes that profile from the container
 *
 * @author thomas.diesler@jboss.com
 * @since 29-Apr-2014
 */
public abstract class ConcurrentProfileTestBase {

    static final Version version = Version.parseVersion("1.2");
    static final String PID = "pidA";

    final ExecutorService executor = Executors.newFixedThreadPool(2);
    volatile Exception lastException;

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() throws Exception {
        executor.shutdown();
        Assert.assertTrue("Terminated in time", executor.awaitTermination(10, TimeUnit.SECONDS));
        PrePostConditions.assertPostConditions();
    }

    @Test
    public void testConcurrentProfiles() throws Exception {

        // Build a profile version with two profiles
        // A <= B

        ProfileVersion profileVersion = ProfileVersionBuilder.Factory.create(version)
                .withProfile(DEFAULT_PROFILE_IDENTITY)
                .and()
                .withProfile("prfA")
                .addConfigurationItem(PID, Collections.singletonMap("keyA", (Object) new Integer(0)))
                .and()
                .withProfile("prfB")
                .addParentProfile("prfA")
                .addConfigurationItem(PID, Collections.singletonMap("keyB", (Object) new Integer(0)))
                .and()
                .build();

        // Add the profile version
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        prfManager.addProfileVersion(profileVersion);

        Profile effectiveB = prfManager.getEffectiveProfile(version, "prfB");
        List<ConfigurationItem> items = effectiveB.getProfileItems(ConfigurationItem.class);
        Assert.assertEquals(1, items.size());
        ConfigurationItem item = effectiveB.getProfileItem(PID, ConfigurationItem.class);
        Map<String, Object> config = item.getConfiguration();
        Assert.assertEquals(2, config.size());
        Assert.assertEquals(0, config.get("keyA"));
        Assert.assertEquals(0, config.get("keyB"));

        // Create a container
        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        ContainerIdentity cntId = Constants.CURRENT_CONTAINER_IDENTITY;

        // Setup the provision listener
        final CountDownLatch latchA = new CountDownLatch(1);
        ProvisionEventListener listener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                String identity = event.getProfile().getIdentity();
                if (event.getType() == EventType.PROVISIONED && "prfB".equals(identity)) {
                    latchA.countDown();
                }
            }
        };

        // Add profile B to the container
        cntManager.setProfileVersion(cntId, version, null);
        cntManager.addProfiles(cntId, Collections.singleton("prfB"), null);

        // Start the container
        cntManager.startContainer(cntId, listener);
        Assert.assertTrue("ProvisionEvent received", latchA.await(200, TimeUnit.MILLISECONDS));

        Future<Boolean> cntClient = executor.submit(new ContainerClient(cntId));
        Future<Boolean> prfClient = executor.submit(new ProfileClient());
        Assert.assertTrue("cntClient ok", cntClient.get());
        Assert.assertTrue("prfClient ok", prfClient.get());

        cntManager.setProfileVersion(cntId, Constants.DEFAULT_PROFILE_VERSION, null);
        prfManager.removeProfileVersion(version);
    }

    class ContainerClient implements Callable<Boolean> {

        final ContainerIdentity cntId;

        ContainerClient(ContainerIdentity cntId) {
            this.cntId = cntId;
        }

        @Override
        public Boolean call() throws Exception {
            ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
            ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
            ConfigurationAdmin configAdmin = ServiceLocator.getRequiredService(ConfigurationAdmin.class);
            for (int i = 0; lastException == null && i < 25; i++) {
                try {
                    // Add the profile
                    cntManager.addProfiles(cntId, Collections.singleton("prfB"), null);

                    // Verify that the effective profile is consistent
                    Profile effectiveB = prfManager.getEffectiveProfile(version, "prfB");
                    ConfigurationItem item = effectiveB.getProfileItem(PID, ConfigurationItem.class);
                    Map<String, Object> config = item.getConfiguration();

                    Integer valA = (Integer) config.get("keyA");
                    Integer valB = (Integer) config.get("keyB");
                    Assert.assertNotNull("keyA value expected", valA);
                    Assert.assertNotNull("keyB value expected", valB);
                    Assert.assertEquals("config values not equal: " + config, valA, valB);

                    // Verify that the ConfigurationAdmin data is consistent
                    Configuration configuration = configAdmin.getConfiguration(PID, null);
                    Dictionary<String, Object> props = configuration.getProperties();

                    valA = (Integer) props.get("keyA");
                    valB = (Integer) props.get("keyB");
                    Assert.assertNotNull("keyA value expected", valA);
                    Assert.assertNotNull("keyB value expected", valB);
                    Assert.assertEquals("config values not equal: " + props, valA, valB);

                    Thread.sleep(10);

                    // Remove the profile
                    cntManager.removeProfiles(cntId, Collections.singleton("prfB"), null);
                    Thread.sleep(10);

                } catch (Exception ex) {
                    lastException = ex;
                    throw ex;
                }
            }
            return true;
        }
    }


    class ProfileClient implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
            for (int i = 0; lastException == null && i < 25; i++) {
                try {
                    LinkedProfile linkedB = prfManager.getLinkedProfile(version, "prfB");
                    LinkedProfile linkedA = linkedB.getLinkedParent("prfA");

                    ProfileBuilder prfBuilder = ProfileBuilder.Factory.createFrom(linkedA);
                    ConfigurationItem prfItem = linkedA.getProfileItem(PID, ConfigurationItem.class);
                    Map<String, Object> config = new HashMap<>(prfItem.getConfiguration());
                    config.put("keyA", new Integer(i + 1));

                    Profile prfA = prfBuilder.addConfigurationItem(PID, config).build();

                    prfBuilder = ProfileBuilder.Factory.createFrom(linkedB);
                    prfItem = linkedB.getProfileItem(PID, ConfigurationItem.class);
                    config = new HashMap<>(prfItem.getConfiguration());
                    config.put("keyB", new Integer(i + 1));

                    Profile prfB = prfBuilder.addConfigurationItem(PID, config).build();

                    LockHandle lock = prfManager.aquireProfileVersionLock(version);
                    try {
                        prfManager.updateProfile(prfA, null);
                        Thread.sleep(10);

                        prfManager.updateProfile(prfB, null);
                        Thread.sleep(10);
                    } finally {
                        lock.unlock();
                    }
                } catch (Exception ex) {
                    lastException = ex;
                    throw ex;
                }
            }
            return true;
        }
    }
}
