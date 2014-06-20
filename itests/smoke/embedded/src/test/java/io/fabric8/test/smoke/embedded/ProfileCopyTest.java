/*
 * #%L
 * Fabric8 :: Testsuite :: Smoke :: Embedded
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
package io.fabric8.test.smoke.embedded;

import io.fabric8.api.Constants;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileItem;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.VersionIdentity;

import java.util.List;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the {@link Profile} copy.
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Apr-2014
 */
@RunWith(Arquillian.class)
public class ProfileCopyTest {

    @Test
    public void testProfileCopy() throws Exception {

        VersionIdentity version = Constants.DEFAULT_PROFILE_VERSION;
        ProfileIdentity identity = Constants.DEFAULT_PROFILE_IDENTITY;

        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        Profile profileA = prfManager.getDefaultProfile();
        Assert.assertEquals(version, profileA.getVersion());
        Assert.assertEquals(identity, profileA.getIdentity());
        List<ProfileItem> itemsA = profileA.getProfileItems(null);

        ProfileBuilder profileBuilder = ProfileBuilder.Factory.createFrom(version, identity);
        Profile profileB = profileBuilder.getProfile();
        Assert.assertEquals(version, profileB.getVersion());
        List<ProfileItem> itemsB = profileB.getProfileItems(null);

        Assert.assertEquals(profileA.getIdentity(), profileB.getIdentity());
        Assert.assertEquals(profileA.getAttributes(), profileB.getAttributes());
        Assert.assertEquals(itemsA, itemsB);
    }
}
