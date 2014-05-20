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

import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.LinkedProfile;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
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

        Profile prfA = ProfileBuilder.Factory.create(identityA)
                .addConfigurationItem("confItem", configA).addConfigurationItem("confItemA", configA)
                .build();

        Profile prfB = ProfileBuilder.Factory.create(identityB)
                .addParentProfile(identityA)
                .addConfigurationItem("confItem", configB)
                .addConfigurationItem("confItemB", configB)
                .build();

        Profile prfC = ProfileBuilder.Factory.create(identityC)
                .addParentProfile(identityA)
                .addParentProfile(identityB)
                .addConfigurationItem("confItem", configC)
                .addConfigurationItem("confItemC", configC)
                .build();

        ProfileVersion linkedVersion = ProfileVersionBuilder.Factory.create(version)
                .addProfile(prfA)
                .addProfile(prfB)
                .addProfile(prfC)
                .build();


        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        ProfileVersion profileVersion = prfManager.addProfileVersion(linkedVersion);
        Set<String> profileIdentities = profileVersion.getProfileIdentities();
        Assert.assertEquals(3, profileIdentities.size());
        Assert.assertTrue(profileIdentities.contains(identityA));
        Assert.assertTrue(profileIdentities.contains(identityB));
        Assert.assertTrue(profileIdentities.contains(identityC));

        // Verify effective A
        LinkedProfile linkedA = prfManager.getLinkedProfile(version, identityA);
        Profile effectiveA = linkedA.getEffectiveProfile();
        Assert.assertEquals("effective:A", effectiveA.getIdentity());
        Assert.assertTrue("No attributes", effectiveA.getAttributes().isEmpty());
        Assert.assertTrue("No parents", effectiveA.getParents().isEmpty());
        Assert.assertEquals(2, effectiveA.getProfileItems(null).size());
        Assert.assertEquals(configA, effectiveA.getProfileItem("confItem", ConfigurationItem.class).getConfiguration());
        Assert.assertEquals(configA, effectiveA.getProfileItem("confItemA", ConfigurationItem.class).getConfiguration());

        // Verify effective B
        LinkedProfile linkedB = prfManager.getLinkedProfile(version, identityB);
        Profile effectiveB = linkedB.getEffectiveProfile();
        Assert.assertEquals("effective:B", effectiveB.getIdentity());
        Assert.assertTrue("No attributes", effectiveB.getAttributes().isEmpty());
        Assert.assertTrue("No parents", effectiveB.getParents().isEmpty());
        Assert.assertEquals(3, effectiveB.getProfileItems(null).size());
        Assert.assertEquals(configB, effectiveB.getProfileItem("confItem", ConfigurationItem.class).getConfiguration());
        Assert.assertEquals(configA, effectiveB.getProfileItem("confItemA", ConfigurationItem.class).getConfiguration());
        Assert.assertEquals(configB, effectiveB.getProfileItem("confItemB", ConfigurationItem.class).getConfiguration());

        // Verify effective C
        LinkedProfile linkedC = prfManager.getLinkedProfile(version, identityC);
        Profile effectiveC = linkedC.getEffectiveProfile();
        Assert.assertEquals("effective:C", effectiveC.getIdentity());
        Assert.assertTrue("No attributes", effectiveC.getAttributes().isEmpty());
        Assert.assertTrue("No parents", effectiveC.getParents().isEmpty());
        Assert.assertEquals(4, effectiveC.getProfileItems(null).size());
        Assert.assertEquals(configC, effectiveC.getProfileItem("confItem", ConfigurationItem.class).getConfiguration());
        Assert.assertEquals(configA, effectiveC.getProfileItem("confItemA", ConfigurationItem.class).getConfiguration());
        Assert.assertEquals(configB, effectiveC.getProfileItem("confItemB", ConfigurationItem.class).getConfiguration());
        Assert.assertEquals(configC, effectiveC.getProfileItem("confItemC", ConfigurationItem.class).getConfiguration());

        prfManager.removeProfileVersion(version);
    }
}
