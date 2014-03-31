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
import io.fabric8.spi.FabricService;
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

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile Exception lastException;

    @After
    public void tearDown() throws Exception {
        executor.shutdown();
        Assert.assertTrue("Terminated in time", executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    public void testConcurrentClients() throws Exception {

        Future<Boolean> modifyFuture = executor.submit(new ModifyClient());
        Future<Boolean> clientA = executor.submit(new ContainerClient("cntA"));
        Future<Boolean> clientB = executor.submit(new ContainerClient("cntB"));
        Future<Boolean> clientC = executor.submit(new ContainerClient("cntC"));

        Assert.assertTrue("Modify client ok", modifyFuture.get());
        Assert.assertTrue("ClientA ok", clientA.get());
        Assert.assertTrue("ClientB ok", clientB.get());
        Assert.assertTrue("ClientC ok", clientC.get());
    }

    class ModifyClient implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            Runtime runtime = RuntimeLocator.getRequiredRuntime();
            ModuleContext syscontext = runtime.getModuleContext();
            for (int i = 0; lastException == null && i < 20; i++) {
                ConfigurationAdmin configAdmin = ServiceLocator.getRequiredService(syscontext, ConfigurationAdmin.class);
                Configuration config = configAdmin.getConfiguration(FabricService.FABRIC_SERVICE_PID, null);
                Dictionary<String, Object> props = new Hashtable<String, Object>();
                props.put(FabricService.KEY_NAME_PREFIX, "config#" + i);
                config.update(props);
                Thread.sleep(50);
            }
            return true;
        }
    }

    class ContainerClient implements Callable<Boolean> {

        private final String prefix;

        ContainerClient(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Boolean call() throws Exception {
            FabricManager service = ServiceLocator.getRequiredService(FabricManager.class);
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

        private Container createAndStart(FabricManager service, int index) throws InterruptedException {
            Container container = service.createContainer(prefix + "#" + index);
            //System.out.println(container);
            Assert.assertSame(State.CREATED, container.getState());
            Thread.sleep(10);
            container.start();
            //System.out.println(container);
            Assert.assertSame(State.STARTED, container.getState());
            return container;
        }


        private void stopAndDestroy(FabricManager service, Container container) throws InterruptedException {
            container.stop();
            //System.out.println(container);
            Assert.assertSame(State.STOPPED, container.getState());
            Thread.sleep(10);
            container.destroy();
            //System.out.println(container);
            Assert.assertSame(State.DESTROYED, container.getState());
        }
    }
}
