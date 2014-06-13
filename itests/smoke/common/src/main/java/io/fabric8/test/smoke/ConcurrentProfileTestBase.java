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
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEvent.EventType;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.api.VersionIdentity;

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

    static final VersionIdentity version = VersionIdentity.createFrom("1.2");
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
        Profile prfA = ProfileBuilder.Factory.create("prfA")
                .addConfigurationItem(PID, Collections.singletonMap("keyA", (Object) new Integer(100)))
                .getProfile();

        Profile prfB = ProfileBuilder.Factory.create("prfB")
                .addParentProfile(prfA.getIdentity())
                .addConfigurationItem(PID, Collections.singletonMap("keyB", (Object) new Integer(100)))
                .getProfile();

        ProfileVersion profileVersion = ProfileVersionBuilder.Factory.create(version)
                .addProfile(ProfileBuilder.Factory.create(DEFAULT_PROFILE_IDENTITY).getProfile())
                .addProfile(prfA)
                .addProfile(prfB)
                .getProfileVersion();

        // Add the profile version
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        prfManager.addProfileVersion(profileVersion);

        Profile effectiveB = prfManager.getEffectiveProfile(version, prfB.getIdentity());
        List<ConfigurationItem> items = effectiveB.getProfileItems(ConfigurationItem.class);
        Assert.assertEquals(1, items.size());
        ConfigurationItem item = effectiveB.getProfileItem(PID, ConfigurationItem.class);
        Map<String, Object> config = item.getDefaultAttributes();
        Assert.assertEquals(2, config.size());
        Assert.assertEquals(100, config.get("keyA"));
        Assert.assertEquals(100, config.get("keyB"));

        // Create a container
        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        ContainerIdentity cntId = cntManager.getCurrentContainer().getIdentity();

        // Setup the provision listener
        final CountDownLatch latchA = new CountDownLatch(1);
        ProvisionEventListener listener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                String identity = event.getProfile().getIdentity().getCanonicalForm();
                if (event.getType() == EventType.PROVISIONED && "effective#1.2.0-default-prfB".equals(identity)) {
                    latchA.countDown();
                }
            }
        };

        // Add profile B to the container
        cntManager.setProfileVersion(cntId, version, null);
        cntManager.addProfiles(cntId, Collections.singletonList(prfB.getIdentity()), null);

        // Start the container
        cntManager.startContainer(cntId, listener);
        Assert.assertTrue("ProvisionEvent received", latchA.await(500, TimeUnit.MILLISECONDS));

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
                    ProfileIdentity prfId = ProfileIdentity.createFrom("prfB");
                    debug("Begin addProfiles: " + prfId);
                    cntManager.addProfiles(cntId, Collections.singletonList(prfId), null);
                    debug("End addProfiles: " + prfId);

                    // Verify that the effective profile is consistent
                    Profile effectiveProfile = prfManager.getEffectiveProfile(version, prfId);
                    ConfigurationItem effectiveItem = effectiveProfile.getProfileItem(PID, ConfigurationItem.class);
                    Map<String, Object> effectiveConfig = effectiveItem.getDefaultAttributes();

                    Integer valA = (Integer) effectiveConfig.get("keyA");
                    Integer valB = (Integer) effectiveConfig.get("keyB");
                    Assert.assertNotNull("keyA value expected", valA);
                    Assert.assertNotNull("keyB value expected", valB);
                    Assert.assertEquals("config values not equal: " + effectiveConfig, valA, valB);

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
                    debug("Begin removeProfiles: " + prfId);
                    cntManager.removeProfiles(cntId, Collections.singletonList(prfId), null);
                    debug("End removeProfiles: " + prfId);
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
                    ProfileIdentity prfIdA = ProfileIdentity.createFrom("prfA");
                    ProfileIdentity prfIdB = ProfileIdentity.createFrom("prfB");
                    LinkedProfile linkedB = prfManager.getLinkedProfile(version, prfIdB);
                    LinkedProfile linkedA = linkedB.getLinkedParent(prfIdA);

                    ProfileBuilder prfBuilderA = ProfileBuilder.Factory.createFrom(linkedA);
                    ConfigurationItem prfItemA = linkedA.getProfileItem(PID, ConfigurationItem.class);
                    Map<String, Object> configA = new HashMap<>(prfItemA.getDefaultAttributes());
                    configA.put("keyA", new Integer(i + 1));

                    Profile prfA = prfBuilderA.addConfigurationItem(PID, configA).getProfile();

                    ProfileBuilder prfBuilderB = ProfileBuilder.Factory.createFrom(linkedB);
                    ConfigurationItem prfItemB = linkedB.getProfileItem(PID, ConfigurationItem.class);
                    Map<String, Object> configB = new HashMap<>(prfItemB.getDefaultAttributes());
                    configB.put("keyB", new Integer(i + 1));

                    Profile prfB = prfBuilderB.addConfigurationItem(PID, configB).getProfile();

                    LockHandle lock = prfManager.aquireProfileVersionLock(version);
                    try {
                        debug("Begin updateProfile: " + prfA);
                        prfManager.updateProfile(prfA, null);
                        debug("End updateProfile: " + prfA);
                        Thread.sleep(10);

                        debug("Begin updateProfile: " + prfB);
                        prfManager.updateProfile(prfB, null);
                        debug("Begin updateProfile: " + prfB);
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

    private void debug(String msg) {
        //System.out.println(msg);
    }
}
