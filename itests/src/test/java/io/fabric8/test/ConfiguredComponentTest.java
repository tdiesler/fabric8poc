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
import io.fabric8.api.FabricManager;
import io.fabric8.api.ServiceLocator;
import io.fabric8.test.support.AbstractEmbeddedTest;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.gravia.runtime.Module;
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
 * Test basic runtime functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public class ConfiguredComponentTest extends AbstractEmbeddedTest {

    @Test
    public void testModifyService() throws Exception {

        // Aquire the {@link FabricManager} instance
        FabricManager service = ServiceLocator.getRequiredService(FabricManager.class);

        final CountDownLatch updateLatch = new CountDownLatch(1);
        ConfigurationListener listener = new ConfigurationListener() {
            @Override
            public void configurationEvent(ConfigurationEvent event) {
                String pid = event.getPid();
                int type = event.getType();
                if (Constants.PID.equals(pid) && type == ConfigurationEvent.CM_UPDATED) {
                    updateLatch.countDown();
                }
            }
        };

        // Modify the service configuration
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();
        ServiceRegistration<ConfigurationListener> sreg = syscontext.registerService(ConfigurationListener.class, listener, null);
        try {
            Module module = runtime.getModules("fabric8-core", null).iterator().next();
            ModuleContext moduleContext = module.getModuleContext();

            ConfigurationAdmin configAdmin = ServiceLocator.getRequiredService(moduleContext, ConfigurationAdmin.class);
            Configuration config = configAdmin.getConfiguration(Constants.PID);
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(Constants.KEY_NAME_PREFIX, "foo");
            config.update(props);

            Assert.assertTrue("Config updated", updateLatch.await(2, TimeUnit.SECONDS));
        } finally {
            sreg.unregister();
        }

        // Wait a little for the component to get updated
        Thread.sleep(100);

        Container cntA = service.createContainer("cntA");
        Assert.assertEquals("foo.cntA", cntA.getName());
        Assert.assertSame(State.CREATED, cntA.getState());

        cntA.start();
        Assert.assertSame(State.STARTED, cntA.getState());

        cntA.stop();
        Assert.assertSame(State.STOPPED, cntA.getState());

        cntA.destroy();
        Assert.assertSame(State.DESTROYED, cntA.getState());
    }
}
