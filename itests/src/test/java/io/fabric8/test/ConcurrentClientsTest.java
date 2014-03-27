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

import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ServiceLocator;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test basic runtime functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public class ConcurrentClientsTest extends AbstractEmbeddedTest {

    private static Logger LOGGER = LoggerFactory.getLogger(ConcurrentClientsTest.class);

    private ExecutorService executor = Executors.newFixedThreadPool(2);
    private volatile Exception lastException;

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
            for (int i = 0; lastException == null && i < 20; i++) {
                ConfigurationAdmin configAdmin = ServiceLocator.getRequiredService(syscontext, ConfigurationAdmin.class);
                Configuration config = configAdmin.getConfiguration(Constants.PID, null);
                Dictionary<String, Object> props = new Hashtable<String, Object>();
                props.put(Constants.KEY_NAME_PREFIX, "config#" + i);
                config.update(props);

                Thread.sleep(50);
            }
            return true;
        }
    }

    class ContainerClient implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            ContainerManager service = ServiceLocator.getRequiredService(ContainerManager.class);
            for (int i = 0; lastException == null && i < 25; i++) {
                try {
                    Container container = createAndStart(service, i);
                    Thread.sleep(10);
                    stopAndDestroy(service, container);
                    Thread.sleep(10);
                } catch (Exception ex) {
                    lastException = ex;
                    LOGGER.error(ex.getMessage(), ex);
                    throw ex;
                }
            }
            return true;
        }

        private Container createAndStart(ContainerManager service, int i) throws InterruptedException {
            Container container = service.createContainer("cnt#" + i);
            Assert.assertSame(State.CREATED, container.getState());
            Thread.sleep(10);

            container = service.startContainer(container.getName());
            Assert.assertSame(State.STARTED, container.getState());
            return container;
        }


        private void stopAndDestroy(ContainerManager service, Container container) throws InterruptedException {
            container = service.stopContainer(container.getName());
            Assert.assertSame(State.STOPPED, container.getState());
            Thread.sleep(10);

            container = service.destroyContainer(container.getName());
            Assert.assertSame(State.DESTROYED, container.getState());
        }
    }
}
