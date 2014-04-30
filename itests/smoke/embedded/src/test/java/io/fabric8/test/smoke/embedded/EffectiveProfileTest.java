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

import io.fabric8.api.ConfigurationProfileItem;
import io.fabric8.api.ConfigurationProfileItemBuilder;
import io.fabric8.api.LinkedProfile;
import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.spi.utils.ProfileUtils;
import io.fabric8.test.embedded.support.EmbeddedTestSupport;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.gravia.resource.Version;
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

    Version version = Version.parseVersion("2.0");
    String identityA = "A";
    String identityB = "B";
    String identityC = "C";

    Map<String, Object>  configA = new HashMap<>();
    Map<String, Object>  configB = new HashMap<>();
    Map<String, Object>  configC = new HashMap<>();

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

        configB.put("keyA", "aaa");
        configB.put("keyB", "bbb");
        configB.put("keyC", "bbb");

        configC.put("keyA", "aaa");
        configC.put("keyB", "bbb");
        configC.put("keyC", "ccc");
    }

    @Test
    public void testEffectiveProfile() {

        ProfileVersionBuilder versionBuilder = ProfileVersionBuilder.Factory.create(version);
        ProfileBuilder builderA = versionBuilder.getProfileBuilder(identityA);
        ConfigurationProfileItemBuilder itemBuilder = builderA.getProfileItemBuilder("confItem", ConfigurationProfileItemBuilder.class);
        builderA.addProfileItem(itemBuilder.setConfiguration(configA).build());
        itemBuilder = builderA.getProfileItemBuilder("confItemA", ConfigurationProfileItemBuilder.class);
        builderA.addProfileItem(itemBuilder.setConfiguration(configA).build());
        Profile profileA = builderA.build();
        versionBuilder.addProfile(profileA);

        ProfileBuilder builderB = versionBuilder.getProfileBuilder(identityB);
        itemBuilder = builderB.getProfileItemBuilder("confItem", ConfigurationProfileItemBuilder.class);
        builderB.addProfileItem(itemBuilder.setConfiguration(configB).build());
        itemBuilder = builderB.getProfileItemBuilder("confItemB", ConfigurationProfileItemBuilder.class);
        builderB.addProfileItem(itemBuilder.setConfiguration(configB).build());
        builderB.addParentProfile(profileA.getIdentity());
        Profile profileB = builderB.build();
        versionBuilder.addProfile(profileB);

        ProfileBuilder builderC = versionBuilder.getProfileBuilder(identityC);
        itemBuilder = builderC.getProfileItemBuilder("confItem", ConfigurationProfileItemBuilder.class);
        builderC.addProfileItem(itemBuilder.setConfiguration(configC).build());
        itemBuilder = builderC.getProfileItemBuilder("confItemC", ConfigurationProfileItemBuilder.class);
        builderC.addProfileItem(itemBuilder.setConfiguration(configC).build());
        builderC.addParentProfile(profileA.getIdentity());
        builderC.addParentProfile(profileB.getIdentity());
        Profile profileC = builderC.build();
        versionBuilder.addProfile(profileC);

        LinkedProfileVersion linkedVersion = versionBuilder.build();
        Map<String, Profile> linkedProfiles = linkedVersion.getLinkedProfiles();

        // Verify effective A
        Profile effectiveA = ProfileUtils.getEffectiveProfile(profileA, linkedProfiles);
        Assert.assertEquals("effective:A", effectiveA.getIdentity());
        Assert.assertTrue("No attributes", effectiveA.getAttributes().isEmpty());
        Assert.assertTrue("No parents", effectiveA.getParents().isEmpty());
        Assert.assertEquals(2, effectiveA.getProfileItems(null).size());
        Assert.assertEquals(configA, effectiveA.getProfileItem("confItem", ConfigurationProfileItem.class).getConfiguration());
        Assert.assertEquals(configA, effectiveA.getProfileItem("confItemA", ConfigurationProfileItem.class).getConfiguration());

        // Verify effective B
        Profile effectiveB = ProfileUtils.getEffectiveProfile(profileB, linkedProfiles);
        Assert.assertEquals("effective:B", effectiveB.getIdentity());
        Assert.assertTrue("No attributes", effectiveB.getAttributes().isEmpty());
        Assert.assertTrue("No parents", effectiveB.getParents().isEmpty());
        Assert.assertEquals(3, effectiveB.getProfileItems(null).size());
        Assert.assertEquals(configB, effectiveB.getProfileItem("confItem", ConfigurationProfileItem.class).getConfiguration());
        Assert.assertEquals(configA, effectiveB.getProfileItem("confItemA", ConfigurationProfileItem.class).getConfiguration());
        Assert.assertEquals(configB, effectiveB.getProfileItem("confItemB", ConfigurationProfileItem.class).getConfiguration());

        // Verify effective C
        Profile effectiveC = ProfileUtils.getEffectiveProfile(profileC, linkedProfiles);
        Assert.assertEquals("effective:C", effectiveC.getIdentity());
        Assert.assertTrue("No attributes", effectiveC.getAttributes().isEmpty());
        Assert.assertTrue("No parents", effectiveC.getParents().isEmpty());
        Assert.assertEquals(4, effectiveC.getProfileItems(null).size());
        Assert.assertEquals(configC, effectiveC.getProfileItem("confItem", ConfigurationProfileItem.class).getConfiguration());
        Assert.assertEquals(configA, effectiveC.getProfileItem("confItemA", ConfigurationProfileItem.class).getConfiguration());
        Assert.assertEquals(configB, effectiveC.getProfileItem("confItemB", ConfigurationProfileItem.class).getConfiguration());
        Assert.assertEquals(configC, effectiveC.getProfileItem("confItemC", ConfigurationProfileItem.class).getConfiguration());

        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        ProfileVersion profileVersion = prfManager.addProfileVersion(linkedVersion);
        Set<String> profileIdentities = profileVersion.getProfileIdentities();
        Assert.assertEquals(3, profileIdentities.size());
        Assert.assertTrue(profileIdentities.contains(identityA));
        Assert.assertTrue(profileIdentities.contains(identityB));
        Assert.assertTrue(profileIdentities.contains(identityC));

        // Verify effective A
        LinkedProfile linkedA = prfManager.getLinkedProfile(version, identityA);
        effectiveA = linkedA.getEffectiveProfile();
        Assert.assertEquals("effective:A", effectiveA.getIdentity());
        Assert.assertTrue("No attributes", effectiveA.getAttributes().isEmpty());
        Assert.assertTrue("No parents", effectiveA.getParents().isEmpty());
        Assert.assertEquals(2, effectiveA.getProfileItems(null).size());
        Assert.assertEquals(configA, effectiveA.getProfileItem("confItem", ConfigurationProfileItem.class).getConfiguration());
        Assert.assertEquals(configA, effectiveA.getProfileItem("confItemA", ConfigurationProfileItem.class).getConfiguration());

        // Verify effective B
        LinkedProfile linkedB = prfManager.getLinkedProfile(version, identityB);
        effectiveB = linkedB.getEffectiveProfile();
        Assert.assertEquals("effective:B", effectiveB.getIdentity());
        Assert.assertTrue("No attributes", effectiveB.getAttributes().isEmpty());
        Assert.assertTrue("No parents", effectiveB.getParents().isEmpty());
        Assert.assertEquals(3, effectiveB.getProfileItems(null).size());
        Assert.assertEquals(configB, effectiveB.getProfileItem("confItem", ConfigurationProfileItem.class).getConfiguration());
        Assert.assertEquals(configA, effectiveB.getProfileItem("confItemA", ConfigurationProfileItem.class).getConfiguration());
        Assert.assertEquals(configB, effectiveB.getProfileItem("confItemB", ConfigurationProfileItem.class).getConfiguration());

        // Verify effective C
        LinkedProfile linkedC = prfManager.getLinkedProfile(version, identityC);
        effectiveC = linkedC.getEffectiveProfile();
        Assert.assertEquals("effective:C", effectiveC.getIdentity());
        Assert.assertTrue("No attributes", effectiveC.getAttributes().isEmpty());
        Assert.assertTrue("No parents", effectiveC.getParents().isEmpty());
        Assert.assertEquals(4, effectiveC.getProfileItems(null).size());
        Assert.assertEquals(configC, effectiveC.getProfileItem("confItem", ConfigurationProfileItem.class).getConfiguration());
        Assert.assertEquals(configA, effectiveC.getProfileItem("confItemA", ConfigurationProfileItem.class).getConfiguration());
        Assert.assertEquals(configB, effectiveC.getProfileItem("confItemB", ConfigurationProfileItem.class).getConfiguration());
        Assert.assertEquals(configC, effectiveC.getProfileItem("confItemC", ConfigurationProfileItem.class).getConfiguration());

        prfManager.removeProfileVersion(version);
    }
}
