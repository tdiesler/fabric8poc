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
import io.fabric8.api.ContainerBuilder;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.ServiceLocator;
import io.fabric8.test.support.AbstractEmbeddedTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test basic container functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public class BasicContainerLifecycleTest extends AbstractEmbeddedTest {

    @Test
    public void testContainerLifecycle() throws Exception {

        ContainerBuilder builder = ContainerBuilder.Factory.create(ContainerBuilder.class);
        CreateOptions options = builder.addIdentity("cntA").getCreateOptions();

        ContainerManager manager = ServiceLocator.getRequiredService(ContainerManager.class);
        Container cnt = manager.createContainer(options);

        ContainerIdentity cntId = cnt.getIdentity();

        Assert.assertEquals("default.cntA", cntId.getSymbolicName());
        Assert.assertSame(State.CREATED, cnt.getState());

        cnt = manager.start(cntId);
        Assert.assertSame(State.STARTED, cnt.getState());

        cnt = manager.stop(cntId);
        Assert.assertSame(State.STOPPED, cnt.getState());

        cnt = manager.destroy(cntId);
        Assert.assertSame(State.DESTROYED, cnt.getState());

        try {
            manager.start(cntId);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // expected
        }
    }
}
