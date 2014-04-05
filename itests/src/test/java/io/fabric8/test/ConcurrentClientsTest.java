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
 * Test concurrent access to container functionality.
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
                Configuration config = configAdmin.getConfiguration(Container.CONTAINER_SERVICE_PID, null);
                Dictionary<String, Object> props = new Hashtable<String, Object>();
                props.put(Container.CNFKEY_CONFIG_TOKEN, "config#" + i);
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
            ContainerManager manager = ServiceLocator.getRequiredService(ContainerManager.class);
            for (int i = 0; lastException == null && i < 25; i++) {
                try {
                    ContainerIdentity cntId = createAndStart(manager, i);
                    Thread.sleep(10);
                    stopAndDestroy(manager, cntId);
                    Thread.sleep(10);
                } catch (Exception ex) {
                    lastException = ex;
                    LOGGER.error(ex.getMessage(), ex);
                    throw ex;
                }
            }
            return true;
        }

        private ContainerIdentity createAndStart(ContainerManager manager, int index) throws InterruptedException {
            ContainerBuilder builder = ContainerBuilder.Factory.create(ContainerBuilder.class);
            CreateOptions options = builder.addIdentity(prefix + "#" + index).getCreateOptions();
            Container cnt = manager.createContainer(options);
            ContainerIdentity cntId = cnt.getIdentity();
            //System.out.println(cnt);
            Assert.assertSame(State.CREATED, cnt.getState());
            Thread.sleep(10);
            cnt = manager.start(cntId);
            //System.out.println(cnt);
            Assert.assertSame(State.STARTED, cnt.getState());
            return cntId;
        }


        private void stopAndDestroy(ContainerManager manager, ContainerIdentity cntId) throws InterruptedException {
            Container cnt = manager.stop(cntId);
            //System.out.println(cnt);
            Assert.assertSame(State.STOPPED, cnt.getState());
            Thread.sleep(10);
            cnt = manager.destroy(cntId);
            //System.out.println(cnt);
            Assert.assertSame(State.DESTROYED, cnt.getState());
        }
    }
}
