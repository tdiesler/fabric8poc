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

import io.fabric8.api.ComponentEvent;
import io.fabric8.api.ComponentEventListener;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerBuilder;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.Container.State;
import io.fabric8.spi.ContainerService;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

/**
 * Test configured container functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public abstract class ConfiguredComponentTests extends PortableTestConditionsTests {

    @Test
    public void testModifyService() throws Exception {

        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();

        final AtomicReference<CountDownLatch> latchA = new AtomicReference<CountDownLatch>(new CountDownLatch(1));
        ConfigurationListener configListener = new ConfigurationListener() {
            @Override
            public void configurationEvent(ConfigurationEvent event) {
                String pid = event.getPid();
                int type = event.getType();
                if (Container.CONTAINER_SERVICE_PID.equals(pid) && type == ConfigurationEvent.CM_UPDATED) {
                    latchA.get().countDown();
                }
            }
        };
        ServiceRegistration<ConfigurationListener> sregA = syscontext.registerService(ConfigurationListener.class, configListener, null);

        // Setup the component listener
        final AtomicReference<CountDownLatch> latchB = new AtomicReference<CountDownLatch>(new CountDownLatch(2));
        ComponentEventListener componentListener = new ComponentEventListener() {
            @Override
            public void processEvent(ComponentEvent event) {
                Class<?> compType = event.getSource();
                if (event.getType() == ComponentEvent.EventType.DEACTIVATED && ContainerService.class.isAssignableFrom(compType)) {
                    latchB.get().countDown();
                }
                if (event.getType() == ComponentEvent.EventType.ACTIVATED && ContainerService.class.isAssignableFrom(compType)) {
                    latchB.get().countDown();
                }
            }
        };
        ServiceRegistration<ComponentEventListener> sregB = syscontext.registerService(ComponentEventListener.class, componentListener, null);

        // Modify the service configuration
        ConfigurationAdmin configAdmin = ServiceLocator.getRequiredService(syscontext, ConfigurationAdmin.class);
        Configuration config = configAdmin.getConfiguration(Container.CONTAINER_SERVICE_PID, null);
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Container.CNFKEY_CONFIG_TOKEN, "foo");
        config.update(props);

        // Wait a little for the component to get updated
        Assert.assertTrue("ConfigurationEvent received", latchA.get().await(200, TimeUnit.MILLISECONDS));
        Assert.assertTrue("ComponentEvent received", latchB.get().await(200, TimeUnit.MILLISECONDS));
        sregB.unregister();

        ContainerBuilder builder = ContainerBuilder.Factory.create(ContainerBuilder.class);
        CreateOptions options = builder.addIdentity("cntA").getCreateOptions();

        ContainerManager cntManager = ServiceLocator.getRequiredService(ContainerManager.class);
        Container cntA = cntManager.createContainer(options);

        ContainerIdentity cntId = cntA.getIdentity();
        Assert.assertEquals("cntA", cntId.getSymbolicName());
        Assert.assertEquals("foo", cntA.getAttribute(Container.ATTKEY_CONFIG_TOKEN));
        Assert.assertSame(State.CREATED, cntA.getState());

        cntA = cntManager.startContainer(cntId, null);
        Assert.assertSame(State.STARTED, cntA.getState());

        cntA = cntManager.stopContainer(cntId);
        Assert.assertSame(State.STOPPED, cntA.getState());

        cntA = cntManager.destroyContainer(cntId);
        Assert.assertSame(State.DESTROYED, cntA.getState());

        // Reset the default configuration
        latchA.set(new CountDownLatch(1));
        config = configAdmin.getConfiguration(Container.CONTAINER_SERVICE_PID, null);
        props = new Hashtable<String, Object>();
        props.put(Container.CNFKEY_CONFIG_TOKEN, "default");
        config.update(props);

        // Wait a little for the component to get updated
        Assert.assertTrue("ConfigurationEvent received", latchA.get().await(200, TimeUnit.MILLISECONDS));
        sregA.unregister();
    }
}
