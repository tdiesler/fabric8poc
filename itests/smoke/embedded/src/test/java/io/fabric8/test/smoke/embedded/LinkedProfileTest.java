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
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.test.embedded.support.EmbeddedTestSupport;

import java.util.HashMap;
import java.util.Map;

import org.jboss.gravia.resource.Version;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test {@link LinkedProfile} functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public class LinkedProfileTest {

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
    public void testLinkedProfile() {

        ProfileVersionBuilder versionBuilder = ProfileVersionBuilder.Factory.create(version);
        ProfileBuilder builderA = versionBuilder.getProfileBuilder(identityA);
        ConfigurationProfileItemBuilder itemBuilder = builderA.getProfileItemBuilder("confItem", ConfigurationProfileItemBuilder.class);
        builderA.addProfileItem(itemBuilder.configuration(configA).build());
        itemBuilder = builderA.getProfileItemBuilder("confItemA", ConfigurationProfileItemBuilder.class);
        builderA.addProfileItem(itemBuilder.configuration(configA).build());
        Profile profileA = builderA.build();
        versionBuilder.addProfile(profileA);

        ProfileBuilder builderB = versionBuilder.getProfileBuilder(identityB);
        itemBuilder = builderB.getProfileItemBuilder("confItem", ConfigurationProfileItemBuilder.class);
        builderB.addProfileItem(itemBuilder.configuration(configB).build());
        itemBuilder = builderB.getProfileItemBuilder("confItemB", ConfigurationProfileItemBuilder.class);
        builderB.addProfileItem(itemBuilder.configuration(configB).build());
        builderB.addParentProfile(profileA.getIdentity());
        Profile profileB = builderB.build();
        versionBuilder.addProfile(profileB);

        ProfileBuilder builderC = versionBuilder.getProfileBuilder(identityC);
        itemBuilder = builderC.getProfileItemBuilder("confItem", ConfigurationProfileItemBuilder.class);
        builderC.addProfileItem(itemBuilder.configuration(configC).build());
        itemBuilder = builderC.getProfileItemBuilder("confItemC", ConfigurationProfileItemBuilder.class);
        builderC.addProfileItem(itemBuilder.configuration(configC).build());
        builderC.addParentProfile(profileA.getIdentity());
        builderC.addParentProfile(profileB.getIdentity());
        Profile profileC = builderC.build();
        versionBuilder.addProfile(profileC);

        LinkedProfileVersion linkedVersion = versionBuilder.build();

        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        prfManager.addProfileVersion(linkedVersion);

        LinkedProfile linkedA = prfManager.getLinkedProfile(version, identityA);
        Assert.assertTrue("No attributes", linkedA.getAttributes().isEmpty());
        Assert.assertTrue("No parents", linkedA.getParents().isEmpty());
        Assert.assertTrue("No linked parents", linkedA.getLinkedParents().isEmpty());
        Assert.assertEquals(2, linkedA.getProfileItems(null).size());
        Assert.assertEquals(configA, linkedA.getProfileItem("confItem", ConfigurationProfileItem.class).getConfiguration());
        Assert.assertEquals(configA, linkedA.getProfileItem("confItemA", ConfigurationProfileItem.class).getConfiguration());

        LinkedProfile linkedB = prfManager.getLinkedProfile(version, identityB);
        Assert.assertTrue("No attributes", linkedB.getAttributes().isEmpty());
        Assert.assertEquals(1, linkedB.getParents().size());
        Assert.assertEquals(1, linkedB.getLinkedParents().size());
        Assert.assertTrue(linkedB.getParents().contains(identityA));
        Assert.assertNotNull(linkedB.getLinkedParents().get(identityA));
        Assert.assertEquals(2, linkedB.getProfileItems(null).size());
        Assert.assertEquals(configB, linkedB.getProfileItem("confItem", ConfigurationProfileItem.class).getConfiguration());
        Assert.assertEquals(configB, linkedB.getProfileItem("confItemB", ConfigurationProfileItem.class).getConfiguration());

        LinkedProfile linkedC = prfManager.getLinkedProfile(version, identityC);
        Assert.assertTrue("No attributes", linkedC.getAttributes().isEmpty());
        Assert.assertEquals(2, linkedC.getParents().size());
        Assert.assertEquals(2, linkedC.getLinkedParents().size());
        Assert.assertTrue(linkedC.getParents().contains(identityA));
        Assert.assertTrue(linkedC.getParents().contains(identityB));
        Assert.assertNotNull(linkedC.getLinkedParents().get(identityA));
        Assert.assertNotNull(linkedC.getLinkedParents().get(identityB));
        Assert.assertEquals(2, linkedC.getProfileItems(null).size());
        Assert.assertEquals(configC, linkedC.getProfileItem("confItem", ConfigurationProfileItem.class).getConfiguration());
        Assert.assertEquals(configC, linkedC.getProfileItem("confItemC", ConfigurationProfileItem.class).getConfiguration());

        Map<String, LinkedProfile> linkedParents = linkedC.getLinkedParents();
        Assert.assertEquals(2, linkedParents.size());
        Assert.assertSame(linkedParents.get(identityA), linkedParents.get(identityB).getLinkedParents().get(identityA));

        prfManager.removeProfileVersion(version);
    }
}
