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

        Profile prfA = ProfileBuilder.Factory.create(identityA)
                .addConfigurationItem("confItem", configA)
                .addConfigurationItem("confItemA", configA)
                .getProfile();

        Profile prfB = ProfileBuilder.Factory.create(identityB)
                .addParentProfile(identityA)
                .addConfigurationItem("confItem", configB)
                .addConfigurationItem("confItemB", configB)
                .getProfile();

        Profile prfC = ProfileBuilder.Factory.create(identityC)
                .addParentProfile(identityA)
                .addParentProfile(identityB)
                .addConfigurationItem("confItem", configC)
                .addConfigurationItem("confItemC", configC)
                .getProfile();

        ProfileVersion linkedVersion = ProfileVersionBuilder.Factory.create(version)
                .addProfile(prfA)
                .addProfile(prfB)
                .addProfile(prfC)
                .getProfileVersion();

        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        prfManager.addProfileVersion(linkedVersion);

        LinkedProfile linkedA = prfManager.getLinkedProfile(version, identityA);
        Assert.assertTrue("No attributes", linkedA.getAttributes().isEmpty());
        Assert.assertTrue("No parents", linkedA.getParents().isEmpty());
        Assert.assertTrue("No linked parents", linkedA.getLinkedParents().isEmpty());
        Assert.assertEquals(2, linkedA.getProfileItems(null).size());
        Assert.assertEquals(configA, linkedA.getProfileItem("confItem", ConfigurationItem.class).getConfiguration());
        Assert.assertEquals(configA, linkedA.getProfileItem("confItemA", ConfigurationItem.class).getConfiguration());

        LinkedProfile linkedB = prfManager.getLinkedProfile(version, identityB);
        Assert.assertTrue("No attributes", linkedB.getAttributes().isEmpty());
        Assert.assertEquals(1, linkedB.getParents().size());
        Assert.assertEquals(1, linkedB.getLinkedParents().size());
        Assert.assertTrue(linkedB.getParents().contains(identityA));
        Assert.assertNotNull(linkedB.getLinkedParents().get(identityA));
        Assert.assertEquals(2, linkedB.getProfileItems(null).size());
        Assert.assertEquals(configB, linkedB.getProfileItem("confItem", ConfigurationItem.class).getConfiguration());
        Assert.assertEquals(configB, linkedB.getProfileItem("confItemB", ConfigurationItem.class).getConfiguration());

        LinkedProfile linkedC = prfManager.getLinkedProfile(version, identityC);
        Assert.assertTrue("No attributes", linkedC.getAttributes().isEmpty());
        Assert.assertEquals(2, linkedC.getParents().size());
        Assert.assertEquals(2, linkedC.getLinkedParents().size());
        Assert.assertTrue(linkedC.getParents().contains(identityA));
        Assert.assertTrue(linkedC.getParents().contains(identityB));
        Assert.assertNotNull(linkedC.getLinkedParents().get(identityA));
        Assert.assertNotNull(linkedC.getLinkedParents().get(identityB));
        Assert.assertEquals(2, linkedC.getProfileItems(null).size());
        Assert.assertEquals(configC, linkedC.getProfileItem("confItem", ConfigurationItem.class).getConfiguration());
        Assert.assertEquals(configC, linkedC.getProfileItem("confItemC", ConfigurationItem.class).getConfiguration());

        Map<String, LinkedProfile> linkedParents = linkedC.getLinkedParents();
        Assert.assertEquals(2, linkedParents.size());
        Assert.assertSame(linkedParents.get(identityA), linkedParents.get(identityB).getLinkedParents().get(identityA));

        prfManager.removeProfileVersion(version);
    }
}
