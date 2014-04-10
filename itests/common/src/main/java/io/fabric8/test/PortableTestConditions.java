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
import io.fabric8.core.api.Container;
import io.fabric8.core.api.ContainerManager;
import io.fabric8.core.api.Profile;
import io.fabric8.core.api.ProfileItem;
import io.fabric8.core.api.ProfileManager;
import io.fabric8.core.api.ServiceLocator;
import io.fabric8.core.spi.BootstrapComplete;

import java.util.Map;
import java.util.Set;

import org.jboss.gravia.resource.Version;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

/**
 * Portable test condition
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public abstract class PortableTestConditions {

    @Before
    public void preConditions() {
        ServiceLocator.getRequiredService(BootstrapComplete.class);
        assertConditions();
    }

    @After
    public void postConditions() {
        assertConditions();
        ServiceLocator.awaitService(BootstrapComplete.class);
    }

    private void assertConditions() {
        ContainerManager cntManager = ServiceLocator.getRequiredService(ContainerManager.class);
        Assert.assertTrue("No containers", cntManager.getContainers(null).isEmpty());

        ProfileManager prfManager = ServiceLocator.getRequiredService(ProfileManager.class);
        Set<Version> profileVersions = prfManager.getProfileVersionIds();
        Assert.assertEquals("One profile version", 1, profileVersions.size());
        Version defaultVersion = profileVersions.iterator().next();
        Assert.assertEquals("1.0.0", defaultVersion.toString());

        Profile defaultProfile = prfManager.getDefaultProfile();
        Assert.assertEquals("Default version", defaultVersion, defaultProfile.getProfileVersion());
        Assert.assertEquals("default", defaultProfile.getIdentity().getSymbolicName());
        Assert.assertTrue("No parents", defaultProfile.getParents().isEmpty());
        Assert.assertTrue("No associated containers", defaultProfile.getContainers().isEmpty());
        Set<ProfileItem> profileItems = defaultProfile.getProfileItems(null);
        Assert.assertEquals("One profile item", 1, profileItems.size());
        ProfileItem profileItem = profileItems.iterator().next();
        ConfigurationProfileItem configItem = (ConfigurationProfileItem) profileItem;
        Map<String, Object> config = configItem.getConfiguration();
        Assert.assertEquals("One config entry", 1, config.size());
        Assert.assertEquals("default", config.get(Container.CNFKEY_CONFIG_TOKEN));
    }
}
