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
package io.fabric8.test.smoke.embedded;

import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.test.embedded.support.EmbeddedTestSupport;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the {@link ProfileVersion} copy.
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Apr-2014
 */
public class ProfileVersionCopyTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        EmbeddedTestSupport.beforeClass();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        EmbeddedTestSupport.afterClass();
    }

    @Test
    public void testProfileVersionCopy() throws Exception {

        ProfileManager prfManager = ProfileManagerLocator.getProfileManager();
        ProfileVersion versionA = prfManager.getDefaultProfileVersion();
        Assert.assertEquals("1.0.0", versionA.getIdentity().toString());

        ProfileVersionBuilder builder = ProfileVersionBuilder.Factory.createFrom(versionA);
        ProfileVersion versionB = builder.getProfileVersion();

        Assert.assertEquals(versionA.getIdentity(), versionB.getIdentity());
        Assert.assertEquals(versionA.getAttributes(), versionB.getAttributes());
        Assert.assertEquals(versionA.getProfileIdentities(), versionB.getProfileIdentities());

    }
}