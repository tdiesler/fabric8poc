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

import io.fabric8.api.ConfigurationProfileItem;
import io.fabric8.api.ConfigurationProfileItemBuilder;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
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
import io.fabric8.spi.DefaultContainerBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.gravia.resource.Version;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
        ProfileVersionBuilder vsnBuilder = ProfileVersionBuilder.Factory.create(version);

        ProfileVersion profileVersion =  vsnBuilder
                .addProfile(
                        vsnBuilder.getProfileBuilder("prfA")
                                .addConfigurationItem(PID,Collections.singletonMap("keyA", (Object) new Integer(0)))
                                .build())
                .addProfile(
                        vsnBuilder.getProfileBuilder("prfB")
                                .addParentProfile("prfA")
                                .addConfigurationItem(PID, Collections.singletonMap("keyB", (Object) new Integer(0)))
                        .build()
                ).build();

        // Add the profile version
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        prfManager.addProfileVersion(profileVersion);

        Profile effectiveB = prfManager.getEffectiveProfile(version, "prfB");
        Set<ConfigurationProfileItem> items = effectiveB.getProfileItems(ConfigurationProfileItem.class);
        Assert.assertEquals(1, items.size());
        ConfigurationProfileItem item = effectiveB.getProfileItem(PID, ConfigurationProfileItem.class);
        Map<String, Object> config = item.getConfiguration();
        Assert.assertEquals(2, config.size());
        Assert.assertEquals(0, config.get("keyA"));
        Assert.assertEquals(0, config.get("keyB"));

        // Create a container
        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        DefaultContainerBuilder cntBuilder = DefaultContainerBuilder.create().identityPrefix("cntA");
        Container cnt = cntManager.createContainer(cntBuilder.build());
        ContainerIdentity cntId = cnt.getIdentity();

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

        cntManager.destroyContainer(cntId);
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
            for (int i = 0; lastException == null && i < 25; i++) {
                try {
                    // Add the profile
                    cntManager.addProfiles(cntId, Collections.singleton("prfB"), null);

                    // Verify that that the effective profile is consistent
                    Profile effectiveB = prfManager.getEffectiveProfile(version, "prfB");
                    ConfigurationProfileItem item = effectiveB.getProfileItem(PID, ConfigurationProfileItem.class);
                    Map<String, Object> config = item.getConfiguration();

                    Integer valA = (Integer) config.get("keyA");
                    Integer valB = (Integer) config.get("keyB");
                    Assert.assertNotNull("keyA value expected", valA);
                    Assert.assertNotNull("keyB value expected", valB);
                    Assert.assertEquals("config values not equal: " + config, valA, valB);
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
                    ProfileBuilder prfBuilder = ProfileBuilder.Factory.createFrom(version, "prfA");
                    Profile prfA = prfBuilder.build();
                    ConfigurationProfileItem prfItem = prfA.getProfileItem(PID, ConfigurationProfileItem.class);
                    Map<String, Object> config = new HashMap<>(prfItem.getConfiguration());
                    config.put("keyA", new Integer(i + 1));

                    prfA = prfBuilder
                            .addConfigurationItem(PID, config)
                            .build();

                    prfBuilder = ProfileBuilder.Factory.createFrom(version, "prfB");
                    Profile prfB = prfBuilder.build();
                    prfItem = prfB.getProfileItem(PID, ConfigurationProfileItem.class);
                    config = new HashMap<>(prfItem.getConfiguration());
                    config.put("keyB", new Integer(i + 1));
                    prfB = prfBuilder.addConfigurationItem(PID, config).build();

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
