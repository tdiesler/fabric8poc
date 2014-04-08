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

import io.fabric8.core.api.ConfigurationProfileItem;
import io.fabric8.core.api.ConfigurationProfileItemBuilder;
import io.fabric8.core.api.Constants;
import io.fabric8.core.api.ContainerManager;
import io.fabric8.core.api.Profile;
import io.fabric8.core.api.ProfileBuilder;
import io.fabric8.core.api.ProfileIdentity;
import io.fabric8.core.api.ProfileManager;
import io.fabric8.core.api.ProfileVersion;
import io.fabric8.core.api.ProfileVersionBuilder;
import io.fabric8.core.api.ServiceLocator;

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
public class BasicProfiles {

    @Test
    public void testProfileAddRemove() throws Exception {

        ContainerManager cntManager = ServiceLocator.getRequiredService(ContainerManager.class);
        ProfileManager prfManager = ServiceLocator.getRequiredService(ProfileManager.class);

        // Verify the default profile
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
        ProfileVersion profileVersion = versionBuilder.addIdentity(version).getProfileVersion();

        // Add a profile version
        prfManager.addProfileVersion(profileVersion);
        Assert.assertEquals(2, prfManager.getProfileVersions(null).size());

        // Build a profile
        ProfileBuilder profileBuilder = ProfileBuilder.Factory.create();
        profileBuilder.addIdentity("foo");
        ConfigurationProfileItemBuilder ibuilder = profileBuilder.getItemBuilder(ConfigurationProfileItemBuilder.class);
        ibuilder.addIdentity("some.pid").setConfiguration(Collections.singletonMap("xxx", (Object) "yyy"));
        profileBuilder.addProfileItem(ibuilder.getProfileItem());
        Profile profile = profileBuilder.getProfile();

        // Verify profile
        Set<ConfigurationProfileItem> items = profile.getProfileItems(ConfigurationProfileItem.class);
        Assert.assertEquals("One item", 1, items.size());
        ConfigurationProfileItem citem = items.iterator().next();
        Assert.assertEquals("some.pid", citem.getIdentity());
        Assert.assertEquals("yyy", citem.getConfiguration().get("xxx"));

        // Add the profile to the given version
        profile = prfManager.addProfile(version, profile);
        Assert.assertEquals(1, prfManager.getProfiles(version, null).size());

        // Remove profile version
        profileVersion = prfManager.removeProfileVersion(version);
        Assert.assertEquals(0, profileVersion.getProfileIds().size());

        Assert.assertTrue("No containers", cntManager.getContainers(null).isEmpty());
    }
}
