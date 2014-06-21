/*
 * #%L
 * Fabric8 :: Testsuite :: Smoke :: Common
 * %%
 * Copyright (C) 2014 Red Hat
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
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the Jolokia endpoint
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Jun-2014
 */
public abstract class JolokiaEndpointTestBase  {

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        PrePostConditions.assertPostConditions();
    }

    @Test
    public void testJolokiaEndpoint() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        ContainerIdentity cntId = cntManager.getCurrentContainer().getIdentity();

    }
}
