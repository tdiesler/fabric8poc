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
package io.fabric8.test.embedded;

import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.ConfigurationItemBuilder;
import io.fabric8.api.Constants;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.api.ServiceLocator;
import io.fabric8.test.embedded.support.AbstractEmbeddedTest;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test profile items functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public class ProfileItemsTest extends AbstractEmbeddedTest {

    @Test
    public void testConfigurationItem() throws Exception {

        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();

        ProfileManager prfManager = ServiceLocator.getRequiredService(ProfileManager.class);

        ProfileBuilder profileBuilder = ProfileBuilder.Factory.create();
        ConfigurationItemBuilder configBuilder = profileBuilder.getItemBuilder(ConfigurationItemBuilder.class);
        configBuilder.addIdentity("some.pid");
        configBuilder.setConfiguration(Collections.singletonMap("foo", (Object) "bar"));
        ConfigurationItem item = configBuilder.getConfigurationItem();
        Set<ConfigurationItem> items = Collections.singleton(item);

        // Setup the profile listener
        final CountDownLatch latchA = new CountDownLatch(1);
        ProfileEventListener profileListener = new ProfileEventListener() {
            @Override
            public void processEvent(ProfileEvent event) {
                String symbolicName = event.getSource().getIdentity().getSymbolicName();
                if (event.getType() == ProfileEvent.EventType.UPDATED && "default".equals(symbolicName)) {
                    latchA.countDown();
                }
            }
        };

        // Setup the provision listener
        final CountDownLatch latchB = new CountDownLatch(2);
        ProvisionEventListener provisionListener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                String symbolicName = event.getProfile().getIdentity().getSymbolicName();
                if (event.getType() == ProvisionEvent.EventType.REMOVED && "default".equals(symbolicName)) {
                    latchB.countDown();
                }
                if (event.getType() == ProvisionEvent.EventType.PROVISIONED && "default".equals(symbolicName)) {
                    latchB.countDown();
                }
            }
        };
        syscontext.registerService(ProvisionEventListener.class, provisionListener, null);

        prfManager.updateProfile(Constants.DEFAULT_PROFILE_VERSION, Constants.DEFAULT_PROFILE_IDENTITY, items, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.await(200, TimeUnit.MILLISECONDS));
        Assert.assertTrue("ProvisionEvent received", latchB.await(200, TimeUnit.MILLISECONDS));
    }
}
