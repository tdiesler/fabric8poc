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

import static io.fabric8.api.Configuration.DELETED_MARKER;
import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.LinkedProfile;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.VersionIdentity;
import io.fabric8.test.embedded.support.EmbeddedTestSupport;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test effective profile functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public class EffectiveProfileTest {

    VersionIdentity version = VersionIdentity.createFrom("2.0");
    ProfileIdentity identityA = ProfileIdentity.createFrom("A");
    ProfileIdentity identityB = ProfileIdentity.createFrom("B");
    ProfileIdentity identityC = ProfileIdentity.createFrom("C");

    Map<String, Object> configA = new HashMap<>();
    Map<String, Object> configB = new HashMap<>();
    Map<String, Object> configC = new HashMap<>();
    Map<String, Object> configC1 = new HashMap<>();
    Map<String, Object> effectB = new HashMap<>();
    Map<String, Object> effectC = new HashMap<>();

    @BeforeClass
    public static void beforeClass() throws Exception {
        EmbeddedTestSupport.beforeClass();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        EmbeddedTestSupport.afterClass();
    }

    @Before
    public void setUp() {
        configA.put("keyA", "aaa");
        configA.put("keyB", "aaa");
        configA.put("keyC", "aaa");

        configB.put("keyA", DELETED_MARKER);
        configB.put("keyB", "bbb");
        configB.put("keyC", "bbb");

        configC.put("keyA", "aaa");
        configC.put("keyB", DELETED_MARKER);
        configC.put("keyC", "ccc");

        configC1.put(DELETED_MARKER, "dummy");

        effectB.put("keyB", "bbb");
        effectB.put("keyC", "bbb");

        effectC.put("keyA", "aaa");
        effectC.put("keyC", "ccc");
    }

    @Test
    public void testEffectiveProfile() {

        Profile prfA = ProfileBuilder.Factory.create(identityA).addConfigurationItem("confItem", configA).addConfigurationItem("confItemA", configA)
                .addConfigurationItem("confItemA1", configA).getProfile();

        Profile prfB = ProfileBuilder.Factory.create(identityB).addParentProfile(identityA).addConfigurationItem("confItem", configB)
                .addConfigurationItem("confItemB", configB).getProfile();

        Profile prfC = ProfileBuilder.Factory.create(identityC).addParentProfile(identityA).addParentProfile(identityB).addConfigurationItem("confItem", configC)
                .addConfigurationItem("confItemC", configC).addConfigurationItem("confItemA1", configC1).getProfile();

        ProfileVersion linkedVersion = ProfileVersionBuilder.Factory.create(version).addProfile(prfA).addProfile(prfB).addProfile(prfC).getProfileVersion();

        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        ProfileVersion profileVersion = prfManager.addProfileVersion(linkedVersion);
        Set<ProfileIdentity> profileIdentities = profileVersion.getProfileIdentities();
        Assert.assertEquals(3, profileIdentities.size());
        Assert.assertTrue(profileIdentities.contains(identityA));
        Assert.assertTrue(profileIdentities.contains(identityB));
        Assert.assertTrue(profileIdentities.contains(identityC));

        // Verify effective A
        LinkedProfile linkedA = prfManager.getLinkedProfile(version, identityA);
        Profile effectiveA = linkedA.getEffectiveProfile();
        Assert.assertEquals("effective#A", effectiveA.getIdentity().getCanonicalForm());
        Assert.assertTrue("No attributes", effectiveA.getAttributes().isEmpty());
        Assert.assertTrue("No parents", effectiveA.getParents().isEmpty());
        Assert.assertEquals(3, effectiveA.getProfileItems(null).size());
        Assert.assertEquals(configA, effectiveA.getProfileItem("confItem", ConfigurationItem.class).getDefaultAttributes());
        Assert.assertEquals(configA, effectiveA.getProfileItem("confItemA", ConfigurationItem.class).getDefaultAttributes());
        Assert.assertEquals(configA, effectiveA.getProfileItem("confItemA1", ConfigurationItem.class).getDefaultAttributes());

        // Verify effective B
        LinkedProfile linkedB = prfManager.getLinkedProfile(version, identityB);
        Profile effectiveB = linkedB.getEffectiveProfile();
        Assert.assertEquals("effective#B", effectiveB.getIdentity().getCanonicalForm());
        Assert.assertTrue("No attributes", effectiveB.getAttributes().isEmpty());
        Assert.assertTrue("No parents", effectiveB.getParents().isEmpty());
        Assert.assertEquals(4, effectiveB.getProfileItems(null).size());
        Assert.assertEquals(effectB, effectiveB.getProfileItem("confItem", ConfigurationItem.class).getDefaultAttributes());
        Assert.assertEquals(configA, effectiveB.getProfileItem("confItemA", ConfigurationItem.class).getDefaultAttributes());
        Assert.assertEquals(configA, effectiveB.getProfileItem("confItemA1", ConfigurationItem.class).getDefaultAttributes());
        Assert.assertEquals(configB, effectiveB.getProfileItem("confItemB", ConfigurationItem.class).getDefaultAttributes());

        // Verify effective C
        LinkedProfile linkedC = prfManager.getLinkedProfile(version, identityC);
        Profile effectiveC = linkedC.getEffectiveProfile();
        Assert.assertEquals("effective#C", effectiveC.getIdentity().getCanonicalForm());
        Assert.assertTrue("No attributes", effectiveC.getAttributes().isEmpty());
        Assert.assertTrue("No parents", effectiveC.getParents().isEmpty());
        Assert.assertEquals(4, effectiveC.getProfileItems(null).size());
        Assert.assertEquals(effectC, effectiveC.getProfileItem("confItem", ConfigurationItem.class).getDefaultAttributes());
        Assert.assertEquals(configA, effectiveC.getProfileItem("confItemA", ConfigurationItem.class).getDefaultAttributes());
        Assert.assertEquals(configB, effectiveC.getProfileItem("confItemB", ConfigurationItem.class).getDefaultAttributes());
        Assert.assertEquals(configC, effectiveC.getProfileItem("confItemC", ConfigurationItem.class).getDefaultAttributes());

        prfManager.removeProfileVersion(version);
    }
}
