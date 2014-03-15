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
import io.fabric8.internal.scr.InvalidComponentException;
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

        // Aquire the {@link FabricService} instance
        FabricService service = ServiceLocator.getRequiredService(FabricService.class);

        final CountDownLatch updateLatch = new CountDownLatch(1);
        ConfigurationListener listener = new ConfigurationListener() {
            @Override
            public void configurationEvent(ConfigurationEvent event) {
                String pid = event.getPid();
                int type = event.getType();
                if (FabricService.PID.equals(pid) && type == ConfigurationEvent.CM_UPDATED) {
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
            Configuration config = configAdmin.getConfiguration(FabricService.PID);
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(Container.KEY_NAME_PREFIX, "foo");
            config.update(props);

            Assert.assertTrue("Config updated", updateLatch.await(2, TimeUnit.SECONDS));
        } finally {
            sreg.unregister();
        }

        // Wait a little for the component to get updated
        Thread.sleep(100);

        try {
            service.createContainer("dummy");
            Assert.fail("InvalidComponentException expected");
        } catch (InvalidComponentException ex) {
            // expected
        }

        service = ServiceLocator.getRequiredService(FabricService.class);

        Container cntA = service.createContainer("cntA");
        Assert.assertEquals("foo.cntA", cntA.getName());
        Assert.assertSame(State.CREATED, cntA.getState());

        service.startContainer(cntA);
        Assert.assertSame(State.STARTED, cntA.getState());

        service.stopContainer(cntA);
        Assert.assertSame(State.STOPPED, cntA.getState());

        service.destroyContainer(cntA);
        Assert.assertSame(State.DESTROYED, cntA.getState());
    }
}
