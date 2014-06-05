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

import static io.fabric8.api.Constants.DEFAULT_PROFILE_VERSION;
import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.ProfileVersion;
import io.fabric8.test.embedded.support.EmbeddedContainerBuilder;
import io.fabric8.test.embedded.support.EmbeddedTestSupport;
import io.fabric8.test.smoke.PrePostConditions;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test basic container functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public class BasicContainerLifecycleTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        EmbeddedTestSupport.beforeClass();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        EmbeddedTestSupport.afterClass();
    }

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        PrePostConditions.assertPostConditions();
    }

    @Test
    public void testContainerLifecycle() throws Exception {

        CreateOptions options = EmbeddedContainerBuilder.create("cntA").getCreateOptions();

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Container cntA = cntManager.createContainer(options);
        ContainerIdentity cntIdA = cntA.getIdentity();

        Assert.assertEquals("cntA", cntIdA.getCanonicalForm());
        Assert.assertSame(State.CREATED, cntA.getState());
        Assert.assertEquals(ProfileVersion.DEFAULT_PROFILE_VERSION, cntA.getProfileVersion());

        cntA = cntManager.startContainer(cntIdA, null);
        Assert.assertSame(State.STARTED, cntA.getState());
        Assert.assertEquals(DEFAULT_PROFILE_VERSION, cntA.getProfileVersion());

        cntA = cntManager.stopContainer(cntIdA);
        Assert.assertSame(State.STOPPED, cntA.getState());

        cntA = cntManager.destroyContainer(cntIdA);
        Assert.assertSame(State.DESTROYED, cntA.getState());

        try {
            cntManager.startContainer(cntIdA, null);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // expected
        }
   }
}
