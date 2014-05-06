/*
 * #%L
 * Fabric8 :: Testsuite :: Smoke :: Common
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
package io.fabric8.test.smoke;

import io.fabric8.api.ConfigurationProfileItem;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileItem;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.ServiceLocator;
import io.fabric8.spi.BootstrapComplete;

import java.util.Map;
import java.util.Set;

import org.jboss.gravia.resource.Version;
import org.junit.Assert;

/**
 * Pre/Post conditions for every test
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public final class PrePostConditions {

    public static void assertPreConditions() {
        ServiceLocator.awaitService(BootstrapComplete.class);
        assertConditions();
    }

    public static void assertPostConditions() {
        assertConditions();
        ServiceLocator.awaitService(BootstrapComplete.class);
    }

    private static void assertConditions() {

        // No registered containers
        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Set<Container> containers = cntManager.getContainers(null);
        Assert.assertEquals("One container", 1, containers.size());
        Container currentContainer = containers.iterator().next();
        Assert.assertEquals(cntManager.getCurrentContainer(), currentContainer);

        // One (default) profile version
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        Set<Version> profileVersions = prfManager.getVersions();
        Assert.assertEquals("One profile version", 1, profileVersions.size());
        Version defaultVersion = profileVersions.iterator().next();
        Assert.assertEquals("1.0.0", defaultVersion.toString());

        // Default profile content
        Profile defaultProfile = prfManager.getDefaultProfile();
        Assert.assertEquals("Default version", defaultVersion, defaultProfile.getVersion());
        Assert.assertEquals("default", defaultProfile.getIdentity());
        Assert.assertTrue("No parents", defaultProfile.getParents().isEmpty());
        Set<ProfileItem> profileItems = defaultProfile.getProfileItems(null);
        Assert.assertEquals("One profile item", 1, profileItems.size());
        ProfileItem profileItem = profileItems.iterator().next();
        ConfigurationProfileItem configItem = (ConfigurationProfileItem) profileItem;
        Map<String, Object> config = configItem.getConfiguration();
        Assert.assertEquals("One config entry", 1, config.size());
        Assert.assertEquals("default", config.get(Container.CNFKEY_CONFIG_TOKEN));
    }
}
