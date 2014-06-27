/*
 * #%L
 * Fabric8 :: Testsuite :: Basic :: Common
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
package io.fabric8.test.basic;


import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.CreateOptions;
import io.fabric8.container.karaf.KarafContainerBuilder;
import io.fabric8.container.tomcat.TomcatContainerBuilder;
import io.fabric8.container.wildfly.WildFlyContainerBuilder;
import io.fabric8.container.wildfly.WildFlyCreateOptions;
import io.fabric8.spi.RuntimeService;
import io.fabric8.test.smoke.PrePostConditions;

import java.io.File;
import java.io.IOException;

import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test basic container functionality.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public abstract class ManagedContainerTestBase  {

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        PrePostConditions.assertPostConditions();
    }

    @Test
    public void testManagedKaraf() throws Exception {

        // Build the {@link CreateOptions}
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        File dataDir = new File((String) runtime.getProperty(RuntimeService.RUNTIME_DATA_DIR));

        CreateOptions options = KarafContainerBuilder.create()
                .identity("ManagedKaraf")
                .outputToConsole(true)
                .targetPath(dataDir.toPath())
                .getCreateOptions();

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Container cnt = cntManager.createContainer(options);
        ContainerIdentity cntId = cnt.getIdentity();
        try {
            // Start the container
            cnt = cntManager.startContainer(cntId, null);
            verifyContainer(cnt);
        } finally {
            cnt = cntManager.stopContainer(cntId);
            Assert.assertEquals(State.STOPPED, cnt.getState());
        }
    }

    @Test
    public void testManagedTomcat() throws Exception {

        // Build the {@link CreateOptions}
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        File dataDir = new File((String) runtime.getProperty(RuntimeService.RUNTIME_DATA_DIR));

        CreateOptions options = TomcatContainerBuilder.create()
                .identity("ManagedTomcat")
                .outputToConsole(true)
                //.jvmArguments("-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n")
                .targetPath(dataDir.toPath())
                .getCreateOptions();

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Container cnt = cntManager.createContainer(options);
        ContainerIdentity cntId = cnt.getIdentity();
        try {
            // Start the container
            cnt = cntManager.startContainer(cntId, null);
            verifyContainer(cnt);
        } finally {
            cnt = cntManager.stopContainer(cntId);
            Assert.assertEquals(State.STOPPED, cnt.getState());
        }
    }

    @Test
    public void testManagedWildFly() throws Exception {

        // Build the {@link CreateOptions}
        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        File dataDir = new File((String) runtime.getProperty(RuntimeService.RUNTIME_DATA_DIR));

        // [TODO] #49 Default port of the running WildFly server is available, why?
        CreateOptions options = WildFlyContainerBuilder.create()
                .identity("ManagedWildFly")
                .outputToConsole(true)
                .targetPath(dataDir.toPath())
                .managementNativePort(WildFlyCreateOptions.DEFAULT_MANAGEMENT_NATIVE_PORT + 1)
                .managementHttpPort(WildFlyCreateOptions.DEFAULT_MANAGEMENT_HTTP_PORT + 1)
                .getCreateOptions();

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Container cnt = cntManager.createContainer(options);
        ContainerIdentity cntId = cnt.getIdentity();
        try {
            // Start the container
            cnt = cntManager.startContainer(cntId, null);
            verifyContainer(cnt);
        } finally {
            cnt = cntManager.stopContainer(cntId);
            Assert.assertEquals(State.STOPPED, cnt.getState());
        }
    }

    private void verifyContainer(Container cnt) throws IOException {
        Assert.assertEquals(State.STARTED, cnt.getState());
    }
}
