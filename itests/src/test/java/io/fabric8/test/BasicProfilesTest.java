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

import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ServiceLocator;
import io.fabric8.test.support.AbstractEmbeddedTest;

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
    public void testProfiles() throws Exception {

        ProfileManager manager = ServiceLocator.getRequiredService(ProfileManager.class);
        Set<ProfileVersion> versions = manager.getProfileVersions(null);
        Assert.assertTrue("No versions", versions.isEmpty());

        Version version = Version.parseVersion("1.0");

        ProfileVersionBuilder pvbuilder = ProfileVersionBuilder.create();
        ProfileVersion profileVersion = pvbuilder.addIdentity(version).createProfileVersion();

        // Add a profile version
        manager.addProfileVersion(profileVersion);
        Assert.assertEquals(1, manager.getProfileVersions(null).size());

        // Build a profile
        ProfileBuilder pbuilder = ProfileBuilder.create();
        Profile profile = pbuilder.addIdentity("foo").createProfile();

        // Add the profile to the given version
        profile = manager.addProfile(version, profile);
        Assert.assertEquals(1, manager.getProfiles(version, null).size());

        // Remove profile version
        manager.removeProfileVersion(version);
        Assert.assertEquals(0, manager.getProfiles(version, null).size());
    }
}
