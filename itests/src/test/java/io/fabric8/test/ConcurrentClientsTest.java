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

import io.fabric8.api.ServiceLocator;
import io.fabric8.api.services.Container;
import io.fabric8.api.services.FabricService;
import io.fabric8.api.services.Container.State;
import io.fabric8.api.state.StateService;
import io.fabric8.api.state.StateService.Permit;
import io.fabric8.test.support.AbstractEmbeddedTest;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Test basic runtime functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public class ConcurrentClientsTest extends AbstractEmbeddedTest {

    ExecutorService executor = Executors.newFixedThreadPool(2);

    @After
    public void setUp() throws Exception {
        executor.shutdown();
        Assert.assertTrue("Terminated in time", executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    public void testConcurrentClients() throws Exception {

        Future<Boolean> modifyFuture = executor.submit(new ModifyClient());
        Future<Boolean> containerFuture = executor.submit(new ContainerClient());

        Assert.assertTrue("Modify client ok", modifyFuture.get());
        Assert.assertTrue("Container client ok", containerFuture.get());
    }

    class ModifyClient implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            Runtime runtime = RuntimeLocator.getRequiredRuntime();
            ModuleContext syscontext = runtime.getModuleContext();
            for (int i = 0; i < 20; i++) {
                ConfigurationAdmin configAdmin = ServiceLocator.getRequiredService(syscontext, ConfigurationAdmin.class);
                Configuration config = configAdmin.getConfiguration(FabricService.PID, null);
                Dictionary<String, Object> props = new Hashtable<String, Object>();
                props.put(Container.KEY_NAME_PREFIX, "config#" + i);
                config.update(props);

                Thread.sleep(50);
            }
            return true;
        }
    }

    class ContainerClient implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            StateService stateService = ServiceLocator.getRequiredService(StateService.class);
            for (int i = 0; i < 25; i++) {
                Container container = createAndStart(stateService, i);
                Thread.sleep(10);
                stopAndDestroy(stateService, container);
                Thread.sleep(10);
            }
            return true;
        }

        private Container createAndStart(StateService stateService, int i) throws InterruptedException {
            Container container;
            Permit permit = stateService.aquirePermit(FabricService.PERMIT_NAME, false);
            try {
                FabricService service = ServiceLocator.getRequiredService(FabricService.class);
                container = service.createContainer("cnt#" + i);
                Assert.assertSame(State.CREATED, container.getState());
                Thread.sleep(10);

                service.startContainer(container);
                Assert.assertSame(State.STARTED, container.getState());
            } finally {
                permit.release();
            }
            return container;
        }


        private void stopAndDestroy(StateService stateService, Container container) throws InterruptedException {
            Permit permit = stateService.aquirePermit(FabricService.PERMIT_NAME, false);
            try {
                FabricService service = ServiceLocator.getRequiredService(FabricService.class);
                service.stopContainer(container);
                Assert.assertSame(State.STOPPED, container.getState());
                Thread.sleep(10);

                service.destroyContainer(container);
                Assert.assertSame(State.DESTROYED, container.getState());
            } finally {
                permit.release();
            }
        }
    }
}
