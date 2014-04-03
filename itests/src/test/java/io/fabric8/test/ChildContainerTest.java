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
import io.fabric8.api.ContainerBuilder;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.FabricException;
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.Container.State;
import io.fabric8.test.support.AbstractEmbeddedTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test child container functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public class ChildContainerTest extends AbstractEmbeddedTest {

    @Test
    public void testChildContainers() throws Exception {

        ContainerBuilder builder = ContainerBuilder.Factory.create(ContainerBuilder.class);
        CreateOptions options = builder.addIdentity("cntA").getCreateOptions();

        ContainerManager manager = ServiceLocator.getRequiredService(ContainerManager.class);
        Container parent = manager.createContainer(options);
        ContainerIdentity parentId = parent.getIdentity();

        builder = ContainerBuilder.Factory.create(ContainerBuilder.class);
        options = builder.addIdentity("cntB").getCreateOptions();

        Container child = manager.createChildContainer(parentId, options);
        ContainerIdentity childId = child.getIdentity();
        Assert.assertEquals("default.cntA:default.cntB", childId.getSymbolicName());

        try {
            manager.destroy(parentId);
            Assert.fail("FabricException expected");
        } catch (FabricException ex) {
            // expected
        }

        child = manager.destroy(childId);
        Assert.assertSame(State.DESTROYED, child.getState());
        parent = manager.destroy(parentId);
        Assert.assertSame(State.DESTROYED, parent.getState());
    }
}
