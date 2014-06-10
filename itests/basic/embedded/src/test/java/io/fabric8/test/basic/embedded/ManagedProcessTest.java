/*
 * #%L
 * Fabric8 :: Testsuite :: Basic :: Embedded
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

package io.fabric8.test.basic.embedded;

import io.fabric8.api.process.ProcessOptions;
import io.fabric8.container.karaf.KarafProcessBuilder;
import io.fabric8.container.tomcat.TomcatProcessBuilder;
import io.fabric8.container.wildfly.WildFlyProcessBuilder;
import io.fabric8.spi.Agent;
import io.fabric8.spi.AgentIdentity;
import io.fabric8.spi.AgentRegistration;
import io.fabric8.spi.AgentTopology;
import io.fabric8.spi.RuntimeIdentity;
import io.fabric8.spi.process.ManagedProcess;
import io.fabric8.spi.process.ManagedProcess.State;
import io.fabric8.spi.process.ProcessIdentity;
import io.fabric8.spi.utils.ManagementUtils;
import io.fabric8.test.embedded.support.EmbeddedTestSupport;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

import org.jboss.gravia.runtime.ServiceLocator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test basic {@link ManagedProcess} functionality
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2014
 */
public class ManagedProcessTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        EmbeddedTestSupport.beforeClass();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        EmbeddedTestSupport.afterClass();
    }

    @Test
    public void testManagedKaraf() throws Exception {

        ProcessOptions options = KarafProcessBuilder.create()
                .targetPath(Paths.get("target", "managed-process"))
                .identityPrefix("ManagedKaraf")
                .outputToConsole(true)
                .getProcessOptions();

        Agent agent = ServiceLocator.getRequiredService(Agent.class);
        ManagedProcess process = agent.createProcess(options);
        try {
            verifyProcess(agent, process);
        } finally {
            agent.destroyProcess(process.getIdentity());
        }
    }

    @Test
    @Ignore
    public void testManagedTomcat() throws Exception {

        ProcessOptions options = TomcatProcessBuilder.create()
                .targetPath(Paths.get("target", "managed-process"))
                 //.jvmArguments("-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=y")
                .identityPrefix("ManagedTomcat")
                .outputToConsole(true)
                .getProcessOptions();

        Agent agent = ServiceLocator.getRequiredService(Agent.class);
        ManagedProcess process = agent.createProcess(options);
        try {
            verifyProcess(agent, process);
        } finally {
            agent.destroyProcess(process.getIdentity());
        }
    }

    @Test
    public void testManagedWildFly() throws Exception {

        ProcessOptions options = WildFlyProcessBuilder.create()
                .targetPath(Paths.get("target", "managed-process"))
                .identityPrefix("ManagedWildFly")
                .outputToConsole(true)
                .getProcessOptions();

        Agent agent = ServiceLocator.getRequiredService(Agent.class);
        ManagedProcess process = agent.createProcess(options);
        try {
            verifyProcess(agent, process);
        } finally {
            agent.destroyProcess(process.getIdentity());
        }
    }

    private void verifyProcess(Agent agent, ManagedProcess process) throws Exception {

        Assert.assertNotNull("ManagedProcess not null", process);
        Path containerHome = process.getHomePath();
        Assert.assertNotNull("Container home not null", containerHome);
        Assert.assertTrue("Container home is dir", containerHome.toFile().isDirectory());

        ProcessIdentity procId = process.getIdentity();
        process = agent.startProcess(procId).get(10, TimeUnit.SECONDS);
        Assert.assertEquals(State.STARTED, process.getState());

        AgentTopology localTopology = agent.getAgentTopology();
        verifyAgentTopologies(localTopology, process);

        process = agent.stopProcess(procId).get(10, TimeUnit.SECONDS);
        Assert.assertEquals(State.STOPPED, process.getState());
    }

    private void verifyAgentTopologies(AgentTopology localTopology, ManagedProcess process) throws Exception {

        /*
        String runtimeId = RuntimeIdentity.getIdentity();
        ProcessIdentity processId = process.getIdentity();
        AgentRegistration localAgentReg = localTopology.getAgentRegistration(AgentIdentity.create(runtimeId));
        AgentRegistration procAgentReg = localTopology.getAgentRegistration(processId);
        Assert.assertSame("Process managed by the local agent", localAgentReg, procAgentReg);

        // Remote agent should be registered
        Assert.assertEquals(2, localTopology.getAgentRegistrations().size());
        AgentRegistration remoteAgentReg = localTopology.getAgentRegistration(AgentIdentity.create(processId.getName()));

        AgentTopology remoteTopology;
        JMXConnector connector = remoteAgentReg.getJMXConnector(200, TimeUnit.MILLISECONDS);
        try {
            MBeanServerConnection server = connector.getMBeanServerConnection();
            Agent remoteAgent = ManagementUtils.getMBeanProxy(server, Agent.OBJECT_NAME, Agent.class);
            remoteTopology = remoteAgent.getAgentTopology();
        } finally {
            connector.close();
        }

        Assert.assertEquals(localTopology, remoteTopology);
        */
    }
}
