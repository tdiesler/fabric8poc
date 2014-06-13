/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
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
 */

package io.fabric8.api;


import org.jboss.gravia.resource.Version;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test {@link VersionIdentity}
 *
 * @author thomas.diesler@jboss.com
 * @since 12-Jun-2014
 */
public class VersionIdentityTest {

    @Test
    public void testSimpleIdentity() {
        VersionIdentity idA = VersionIdentity.create("1.0");
        Assert.assertEquals(Version.parseVersion("1.0"), idA.getVersion());
        Assert.assertEquals("1.0.0", idA.getCanonicalForm());
        Assert.assertEquals(idA, VersionIdentity.createFrom(idA.getCanonicalForm()));
        Assert.assertNull("Revision is null", idA.getRevision());

        try {
            VersionIdentity.createFrom("foo");
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            // expected
        }

        try {
            VersionIdentity.createFrom("1.0,rev=invalid@char");
            Assert.fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testRevisonIdentity() {
        VersionIdentity idA = VersionIdentity.create("1.0", null);
        Assert.assertEquals(Version.parseVersion("1.0"), idA.getVersion());
        Assert.assertEquals("1.0.0", idA.getCanonicalForm());
        Assert.assertEquals(idA, VersionIdentity.createFrom(idA.getCanonicalForm()));
        Assert.assertNull("Revision is null", idA.getRevision());

        VersionIdentity idB = VersionIdentity.create("1.0", "bbb");
        Assert.assertEquals(Version.parseVersion("1.0"), idB.getVersion());
        Assert.assertEquals("1.0.0,rev=bbb", idB.getCanonicalForm());
        Assert.assertEquals(idB, VersionIdentity.createFrom(idB.getCanonicalForm()));
        Assert.assertEquals("bbb", idB.getRevision());

        Assert.assertNotEquals(idA, idB);
    }
}