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

import io.fabric8.api.AttributeKey.ValueFactory;
import io.fabric8.api.OptionsProvider;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.management.ProfileVersionManagement;
import io.fabric8.spi.management.ProfileVersionOpenType;
import io.fabric8.spi.utils.ManagementUtils;
import io.fabric8.test.embedded.support.EmbeddedTestSupport;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.openmbean.CompositeData;

import org.jboss.gravia.resource.Version;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the {@link ProfileVersion}.
 *
 * @author thomas.diesler@jboss.com
 * @since 05-Mar-2014
 */
public class ProfileVersionOpenTypeTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        EmbeddedTestSupport.beforeClass();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        EmbeddedTestSupport.afterClass();
    }

    @Test
    public void testComposisteData() throws Exception {

        Version version = Version.parseVersion("2.0");

        Profile prfA = ProfileBuilder.Factory.create("foo").getProfile();

        ProfileVersion prfvA = ProfileVersionBuilder.Factory.create(version).addProfile(prfA).getProfileVersion();

        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        prfvA = prfManager.addProfileVersion(prfvA);

        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ProfileVersionManagement prfvManagement = ManagementUtils.getMBeanProxy(mbeanServer, ProfileVersionManagement.OBJECT_NAME, ProfileVersionManagement.class);
        CompositeData cdata = prfvManagement.getProfileVersion(prfvA.getIdentity().toString());
        ProfileVersion prfvB = ProfileVersionOpenType.getProfileVersion(cdata);
        Assert.assertEquals(version, prfvB.getIdentity());

        prfManager.removeProfileVersion(version);

        // Test the {@link ProfileVersionOptionsProvider}
        ProfileVersionBuilder versionBuilder = ProfileVersionBuilder.Factory.create();
        versionBuilder.addOptions(new CompositeDataOptionsProvider(cdata));
        ProfileVersion prfvC = versionBuilder.getProfileVersion();

        prfvC = prfManager.addProfileVersion(prfvC);
        Assert.assertEquals(prfvA.getIdentity(), prfvC.getIdentity());
        Assert.assertEquals(prfvA.getProfileIdentities(), prfvC.getProfileIdentities());

        prfManager.removeProfileVersion(version);
    }

    public static class StringValueFactory implements ValueFactory<String> {
        @Override
        public String createFrom(Object source) {
            return (String) source;
        }
    }

    static class CompositeDataOptionsProvider implements OptionsProvider<ProfileVersionBuilder> {

        private final CompositeData cdata;

        CompositeDataOptionsProvider(CompositeData cdata) {
            this.cdata = cdata;
        }

        @Override
        public ProfileVersionBuilder addBuilderOptions(ProfileVersionBuilder builder) {
            ProfileVersion profileVersion = ProfileVersionOpenType.getProfileVersion(cdata);
            builder.identity(profileVersion.getIdentity());
            for (String prfid : profileVersion.getProfileIdentities()) {
                Profile prf = ProfileBuilder.Factory.create(prfid).getProfile();
                builder.addProfile(prf);
            }
            return builder;
        }
    }
}
