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
package io.fabric8.test.smoke;


import static io.fabric8.api.Constants.DEFAULT_PROFILE_VERSION;
import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.ServiceLocator;
import io.fabric8.spi.DefaultContainerBuilder;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test basic container functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public abstract class BasicContainerLifecycleTests extends PortableTestConditionsTests {

    @Test
    public void testContainerLifecycle() throws Exception {

        DefaultContainerBuilder builder = new DefaultContainerBuilder();
        CreateOptions options = builder.addIdentity("cntA").getCreateOptions();

        ContainerManager cntManager = ServiceLocator.getRequiredService(ContainerManager.class);
        Container cntA = cntManager.createContainer(options);
        ContainerIdentity idA = cntA.getIdentity();

        Assert.assertEquals("cntA", idA.getSymbolicName());
        Assert.assertSame(State.CREATED, cntA.getState());
        Assert.assertEquals("default", cntA.getAttribute(Container.ATTKEY_CONFIG_TOKEN));
        Assert.assertNull("Null profile version", cntA.getProfileVersion());

        cntA = cntManager.startContainer(idA, null);
        Assert.assertSame(State.STARTED, cntA.getState());
        Assert.assertEquals(DEFAULT_PROFILE_VERSION, cntA.getProfileVersion());

        cntA = cntManager.stopContainer(idA);
        Assert.assertSame(State.STOPPED, cntA.getState());

        cntA = cntManager.destroyContainer(idA);
        Assert.assertSame(State.DESTROYED, cntA.getState());

        try {
            cntManager.startContainer(idA, null);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // expected
        }
   }
}
