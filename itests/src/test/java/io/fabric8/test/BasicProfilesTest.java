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
import io.fabric8.api.ServiceLocator;
import io.fabric8.test.support.AbstractEmbeddedTest;

import java.util.List;

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
        List<Version> versions = manager.getVersions();
        Assert.assertTrue("No versions", versions.isEmpty());

        Version version = Version.parseVersion("1.0");

        // Add a profile version
        manager.addProfileVersion(version);
        Assert.assertEquals(1, manager.getVersions().size());
        Assert.assertEquals("1.0.0", manager.getVersions().get(0).toString());
        Assert.assertTrue("No profiles", manager.getAllProfiles().isEmpty());

        // Build a profile
        ProfileBuilder builder = manager.getProfileBuilder();
        Profile profile = builder.addIdentity("foo", null).createProfile();
        Assert.assertEquals(Version.emptyVersion, profile.getIdentity().getVersion());
        Assert.assertTrue("No profiles", manager.getAllProfiles().isEmpty());

        // Add the profile to the given version
        manager.addProfile(profile, version);
        Assert.assertEquals(1, manager.getAllProfiles().size());
        Assert.assertEquals(1, manager.getProfiles(version).size());

        // Remove profile version
        manager.removeProfileVersion(version);
        Assert.assertEquals(0, manager.getAllProfiles().size());
        Assert.assertEquals(0, manager.getProfiles(version).size());
    }
}
