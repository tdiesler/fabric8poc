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

import static io.fabric8.api.Constants.DEFAULT_PROFILE_VERSION;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.ResourceItem;
import io.fabric8.test.smoke.container.ProvisionerTest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jboss.gravia.provision.Provisioner;
import org.jboss.gravia.resource.Capability;
import org.jboss.gravia.resource.ContentCapability;
import org.jboss.gravia.resource.ContentNamespace;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceBuilder;
import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.gravia.runtime.ServiceLocator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test reference {@link ResourceItem} functionality
 *
 * @author thomas.diesler@jboss.com
 * @since 22-May-2014
 */
public abstract class ReferenceItemTestBase {

    protected static final String RESOURCE_A = "refitemA";

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        PrePostConditions.assertPostConditions();
    }

    /**
     * @see ProvisionerTest#testStreamResource()
     */
    @Test
    public void testStreamResource() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        Provisioner provisioner = ServiceLocator.getRequiredService(Provisioner.class);

        // Build the resitem
        ResourceIdentity identityA = ResourceIdentity.fromString(RESOURCE_A);
        ResourceBuilder builderA = provisioner.getContentResourceBuilder(identityA, new ByteArrayInputStream("Hello".getBytes()));
        Resource resourceA = builderA.getResource();

        // Build a profile
        ProfileIdentity idFoo = ProfileIdentity.createFrom("foo");
        Profile prfFoo = ProfileBuilder.Factory.create(idFoo)
                .profileVersion(DEFAULT_PROFILE_VERSION)
                .addReferenceResourceItem(resourceA)
                .getProfile();

        // Add the profile and verify the item URL
        prfFoo = prfManager.addProfile(DEFAULT_PROFILE_VERSION, prfFoo);
        Assert.assertEquals(1, prfFoo.getProfileItems(ResourceItem.class).size());
        ResourceItem itemA = prfFoo.getProfileItem(identityA);
        Assert.assertEquals("profile://1.0.0/foo/refitemA?version=0.0.0", getItemURL(itemA, 0).toExternalForm());
        InputStream input = new URL("profile://1.0.0/foo/refitemA").openStream();
        Assert.assertNotNull("URL stream not null", input);
        Assert.assertEquals("Hello", new BufferedReader(new InputStreamReader(input)).readLine());

        // Add the profile to the current coontainer
        Container cnt = cntManager.getCurrentContainer();
        cntManager.addProfiles(cnt.getIdentity(), Collections.singletonList(idFoo), null);

        input = new URL("container://refitemA").openStream();
        Assert.assertNotNull("URL stream not null", input);
        Assert.assertEquals("Hello", new BufferedReader(new InputStreamReader(input)).readLine());

        cntManager.removeProfiles(cnt.getIdentity(), Collections.singletonList(idFoo), null);
        prfManager.removeProfile(DEFAULT_PROFILE_VERSION, idFoo);
    }

    private URL getItemURL(ResourceItem item, int index) {
        List<Capability> ccaps = item.getResource().getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        return ccaps.get(index).adapt(ContentCapability.class).getContentURL();
    }
}
