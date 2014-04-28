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

import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.ServiceLocator;
import io.fabric8.spi.ContainerService;
import io.fabric8.spi.DefaultContainerBuilder;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Test concurrent access to {@link ContainerManager}
 *
 * One thread continuesly modifies the configuration of
 * the {@link ContainerService}, which causes the component to deactivate/active
 *
 * Three other threads use {@link ContainerManager} to create/start/stop/destroy
 * containers based on the current configuration
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public abstract class ConcurrentConfigurationTestBase  {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile Exception lastException;

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() throws Exception {
        PrePostConditions.assertPostConditions();
        executor.shutdown();
        Assert.assertTrue("Terminated in time", executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    @Test
    public void testConcurrentClients() throws Exception {

        Future<Boolean> modifyFuture = executor.submit(new ConfigAdminClient());
        Future<Boolean> clientA = executor.submit(new ContainerClient("cntA"));
        Future<Boolean> clientB = executor.submit(new ContainerClient("cntB"));
        Future<Boolean> clientC = executor.submit(new ContainerClient("cntC"));

        Assert.assertTrue("Modify client ok", modifyFuture.get());
        Assert.assertTrue("ClientA ok", clientA.get());
        Assert.assertTrue("ClientB ok", clientB.get());
        Assert.assertTrue("ClientC ok", clientC.get());
    }

    class ConfigAdminClient implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            ConfigurationAdmin configAdmin = ServiceLocator.getRequiredService(ConfigurationAdmin.class);
            for (int i = 0; lastException == null && i < 20; i++) {
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
            ContainerManager manager = ContainerManagerLocator.getContainerManager();
            for (int i = 0; lastException == null && i < 25; i++) {
                try {
                    ContainerIdentity cntId = createAndStart(manager, i);
                    Thread.sleep(10);
                    stopAndDestroy(manager, cntId);
                    Thread.sleep(10);
                } catch (Exception ex) {
                    lastException = ex;
                    throw ex;
                }
            }
            return true;
        }

        private ContainerIdentity createAndStart(ContainerManager manager, int index) throws InterruptedException {
            DefaultContainerBuilder builder = DefaultContainerBuilder.create();
            CreateOptions options = builder.addIdentityPrefix(prefix + "#" + index).build();
            Container cnt = manager.createContainer(options);
            ContainerIdentity cntId = cnt.getIdentity();
            //System.out.println(cnt);
            Assert.assertSame(State.CREATED, cnt.getState());
            Thread.sleep(10);
            cnt = manager.startContainer(cntId, null);
            //System.out.println(cnt);
            Assert.assertSame(State.STARTED, cnt.getState());
            return cntId;
        }


        private void stopAndDestroy(ContainerManager manager, ContainerIdentity cntId) throws InterruptedException {
            Container cnt = manager.stopContainer(cntId);
            //System.out.println(cnt);
            Assert.assertSame(State.STOPPED, cnt.getState());
            Thread.sleep(10);
            cnt = manager.destroyContainer(cntId);
            //System.out.println(cnt);
            Assert.assertSame(State.DESTROYED, cnt.getState());
        }
    }
}
