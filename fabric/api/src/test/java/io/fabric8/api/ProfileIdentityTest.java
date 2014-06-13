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


import org.junit.Assert;
import org.junit.Test;

/**
 * Test {@link ProfileIdentity}
 *
 * @author thomas.diesler@jboss.com
 * @since 12-Jun-2014
 */
public class ProfileIdentityTest {

    @Test
    public void testSimpleIdentity() {
        ProfileIdentity idA = ProfileIdentity.create("aaa");
        Assert.assertEquals("aaa", idA.getSymbolicName());
        Assert.assertEquals("aaa", idA.getCanonicalForm());
        Assert.assertEquals(idA, ProfileIdentity.createFrom(idA.getCanonicalForm()));
        Assert.assertNull("Revision is null", idA.getRevision());
    }

    @Test
    public void testRevisonIdentity() {
        ProfileIdentity idA = ProfileIdentity.create("aaa", null);
        Assert.assertEquals("aaa", idA.getSymbolicName());
        Assert.assertEquals("aaa", idA.getCanonicalForm());
        Assert.assertEquals(idA, ProfileIdentity.createFrom(idA.getCanonicalForm()));
        Assert.assertNull("Revision is null", idA.getRevision());

        ProfileIdentity idB = ProfileIdentity.create("aaa", "bbb");
        Assert.assertEquals("aaa", idB.getSymbolicName());
        Assert.assertEquals("aaa,rev=bbb", idB.getCanonicalForm());
        Assert.assertEquals(idB, ProfileIdentity.createFrom(idB.getCanonicalForm()));
        Assert.assertEquals("bbb", idB.getRevision());

        Assert.assertNotEquals(idA, idB);
    }
}