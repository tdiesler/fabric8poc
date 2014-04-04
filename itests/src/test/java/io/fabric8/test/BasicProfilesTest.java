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
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ServiceLocator;
import io.fabric8.test.support.AbstractEmbeddedTest;

import java.util.Collections;
import java.util.Set;

import org.jboss.gravia.resource.Version;
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
        Set<ProfileIdentity> profileIdentities = defaultVersion.getProfileIdentities();
        Assert.assertEquals("One profile", 1, profileIdentities.size());
        Assert.assertEquals(Constants.DEFAULT_PROFILE_IDENTITY, profileIdentities.iterator().next());

        Version version = Version.parseVersion("1.1");

        ProfileVersionBuilder pvbuilder = ProfileVersionBuilder.Factory.create();
        ProfileVersion profileVersion = pvbuilder.addIdentity(version).createProfileVersion();

        // Add a profile version
        prfManager.addProfileVersion(profileVersion);
        Assert.assertEquals(2, prfManager.getProfileVersions(null).size());

        // Build a profile
        ProfileBuilder pbuilder = ProfileBuilder.Factory.create();
        pbuilder.addIdentity("foo");
        ConfigurationItemBuilder ibuilder = pbuilder.getItemBuilder(ConfigurationItemBuilder.class);
        ibuilder.addIdentity("some.pid").setConfiguration(Collections.singletonMap("xxx", "yyy"));
        pbuilder.addProfileItem(ibuilder.getConfigurationItem());
        Profile profile = pbuilder.createProfile();

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
        Assert.assertEquals(0, profileVersion.getProfileIdentities().size());
    }

    @Test
    public void testProfileUpdate() throws Exception {

        Version version = Version.parseVersion("1.2");

        ProfileVersionBuilder pvbuilder = ProfileVersionBuilder.Factory.create();
        ProfileVersion profileVersion = pvbuilder.addIdentity(version).createProfileVersion();

        // Add a profile version
        ProfileManager manager = ServiceLocator.getRequiredService(ProfileManager.class);
        manager.addProfileVersion(profileVersion);
        Assert.assertEquals(2, manager.getProfileVersions(null).size());

        // Build a profile
        ProfileBuilder pbuilder = ProfileBuilder.Factory.create();
        pbuilder.addIdentity("foo");
        ConfigurationItemBuilder ibuilder = pbuilder.getItemBuilder(ConfigurationItemBuilder.class);
        ibuilder.addIdentity("some.pid").setConfiguration(Collections.singletonMap("xxx", "yyy"));
        pbuilder.addProfileItem(ibuilder.getConfigurationItem());
        Profile profile = pbuilder.createProfile();

        // Add the profile to the given version
        profile = manager.addProfile(version, profile);
        Assert.assertEquals(1, manager.getProfiles(version, null).size());

        // Update the profile item
        pbuilder = ProfileBuilder.Factory.create();
        ibuilder = pbuilder.getItemBuilder(ConfigurationItemBuilder.class);
        ibuilder.addIdentity("some.pid").setConfiguration(Collections.singletonMap("xxx", "zzz"));
        ConfigurationItem item = ibuilder.getConfigurationItem();

        Set<ConfigurationItem> items = Collections.singleton(item);
        profile = manager.updateProfile(version, profile.getIdentity(), items, false);

        // Verify profile
        items = profile.getProfileItems(ConfigurationItem.class);
        Assert.assertEquals("One item", 1, items.size());
        ConfigurationItem citem = items.iterator().next();
        Assert.assertEquals("some.pid", citem.getIdentity());
        Assert.assertEquals("zzz", citem.getConfiguration().get("xxx"));

        // Remove profile version
        profileVersion = manager.removeProfileVersion(version);
        Assert.assertEquals(0, profileVersion.getProfileIdentities().size());
    }
}
