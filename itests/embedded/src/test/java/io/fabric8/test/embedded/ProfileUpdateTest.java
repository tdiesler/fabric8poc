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
package io.fabric8.test.embedded;

import io.fabric8.api.ComponentEvent;
import io.fabric8.api.ComponentEventListener;
import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.ConfigurationItemBuilder;
import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerBuilder;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.api.ServiceLocator;
import io.fabric8.spi.ContainerService;
import io.fabric8.test.embedded.support.AbstractEmbeddedTest;

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
 * Test profile update functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public class ProfileUpdateTest extends AbstractEmbeddedTest {

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
        syscontext.registerService(ProvisionEventListener.class, provisionListener, null);

        profile = prfManager.updateProfile(version, profile.getIdentity(), items, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.await(100, TimeUnit.MILLISECONDS));
        Assert.assertFalse("ProvisionEvent not received", latchB.await(100, TimeUnit.MILLISECONDS));

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

        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();

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
                String symbolicName = event.getSource().getIdentity().getSymbolicName();
                if (event.getType() == ProfileEvent.EventType.UPDATED && "default".equals(symbolicName)) {
                    latchA.countDown();
                }
            }
        };

        // Setup the provision listener
        final CountDownLatch latchB = new CountDownLatch(2);
        ProvisionEventListener provisionListener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                String symbolicName = event.getProfile().getIdentity().getSymbolicName();
                if (event.getType() == ProvisionEvent.EventType.REMOVED && "default".equals(symbolicName)) {
                    latchB.countDown();
                }
                if (event.getType() == ProvisionEvent.EventType.PROVISIONED && "default".equals(symbolicName)) {
                    latchB.countDown();
                }
            }
        };
        syscontext.registerService(ProvisionEventListener.class, provisionListener, null);

        // Setup the component listener
        final CountDownLatch latchC = new CountDownLatch(2);
        ComponentEventListener componentListener = new ComponentEventListener() {
            @Override
            public void processEvent(ComponentEvent event) {
                Class<?> compType = event.getSource();
                if (event.getType() == ComponentEvent.EventType.DEACTIVATED && ContainerService.class.isAssignableFrom(compType)) {
                    latchC.countDown();
                }
                if (event.getType() == ComponentEvent.EventType.ACTIVATED && ContainerService.class.isAssignableFrom(compType)) {
                    latchC.countDown();
                }
            }
        };
        syscontext.registerService(ComponentEventListener.class, componentListener, null);

        Profile profile = prfManager.updateProfile(Constants.DEFAULT_PROFILE_VERSION, Constants.DEFAULT_PROFILE_IDENTITY, items, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.await(200, TimeUnit.MILLISECONDS));
        Assert.assertTrue("ProvisionEvent received", latchB.await(200, TimeUnit.MILLISECONDS));
        Assert.assertTrue("ComponentEvent received", latchC.await(200, TimeUnit.MILLISECONDS));

        // Verify profile
        items = profile.getProfileItems(ConfigurationItem.class);
        Assert.assertEquals("One item", 1, items.size());
        ConfigurationItem citem = items.iterator().next();
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

        cntB = cntManager.destroy(idB);
        Assert.assertSame(State.DESTROYED, cntB.getState());
        cntA = cntManager.destroy(idA);
        Assert.assertSame(State.DESTROYED, cntA.getState());

        Assert.assertTrue("No containers", cntManager.getContainers(null).isEmpty());
    }
}
