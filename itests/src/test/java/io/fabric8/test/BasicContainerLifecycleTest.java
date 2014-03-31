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

import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.FabricManager;
import io.fabric8.api.ServiceLocator;
import io.fabric8.test.support.AbstractEmbeddedTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test basic runtime functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public class BasicContainerLifecycleTest extends AbstractEmbeddedTest {

    @Test
    public void testContainerLifecycle() throws Exception {

        FabricManager service = ServiceLocator.getRequiredService(FabricManager.class);
        Container cntA = service.newContainerBuilder().addIdentity("cntA").createContainer();
        Assert.assertEquals("cntA", cntA.getIdentity().getName());
        Assert.assertSame(State.CREATED, cntA.getState());

        cntA.start();
        Assert.assertSame(State.STARTED, cntA.getState());

        cntA.stop();
        Assert.assertSame(State.STOPPED, cntA.getState());

        cntA.destroy();
        Assert.assertSame(State.DESTROYED, cntA.getState());

        try {
            cntA.start();
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // expected
        }
    }
}
