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

import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileItem;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.spi.BootstrapComplete;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.gravia.resource.Capability;
import org.jboss.gravia.resource.IdentityNamespace;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.Module;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.runtime.ServiceLocator;
import org.junit.Assert;

/**
 * Pre/Post conditions for every test
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public final class PrePostConditions {

    private static ThreadLocal<Set<Module>> modulesAssociation = new ThreadLocal<>();

    public static void assertPreConditions() {
        ServiceLocator.awaitService(BootstrapComplete.class);

        modulesAssociation.set(getCurrentRuntimeModules());
        assertContainers();
        assertProfiles();
    }

    public static void assertPostConditions() {

        assertContainers();
        assertProfiles();
        assertRuntimeModules();

        ServiceLocator.awaitService(BootstrapComplete.class);
    }

    private static void assertContainers() {

        // No registered containers
        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Set<Container> containers = cntManager.getContainers(null);
        Assert.assertEquals("One container", 1, containers.size());
        Container currentContainer = containers.iterator().next();
        Assert.assertEquals(cntManager.getCurrentContainer(), currentContainer);
    }

    private static void assertProfiles() {

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
        List<ProfileItem> profileItems = defaultProfile.getProfileItems(null);
        Assert.assertEquals("One profile item", 1, profileItems.size());
        ProfileItem profileItem = profileItems.iterator().next();
        ConfigurationItem configItem = (ConfigurationItem) profileItem;
        Map<String, Object> config = configItem.getConfiguration();
        Assert.assertEquals("One config entry", 1, config.size());
        Assert.assertEquals("default", config.get("config.token"));
    }

    private static void assertRuntimeModules() {
        Set<Module> current = getCurrentRuntimeModules();
        current.removeAll(modulesAssociation.get());

        // [TODO] Runtime types that do not support uninstall of shared modules
        if (RuntimeType.WILDFLY == RuntimeType.getRuntimeType()) {
            Iterator<Module> itmod = current.iterator();
            while (itmod.hasNext()) {
                Resource res = itmod.next().adapt(Resource.class);
                Capability icap = res.getIdentityCapability();
                Object attval = icap.getAttribute(IdentityNamespace.CAPABILITY_SHARED_ATTRIBUTE);
                if (attval != null && Boolean.parseBoolean(attval.toString())) {
                    itmod.remove();
                }
            }
        }
        Assert.assertEquals("Uninstalled modules left: " + current, 0, current.size());
    }

    private static Set<Module> getCurrentRuntimeModules() {
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        return runtime.getModules();
    }
}
