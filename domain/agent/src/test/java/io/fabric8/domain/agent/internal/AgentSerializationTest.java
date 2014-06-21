package io.fabric8.domain.agent.internal;

/*
 * #%L
 * Fabric8 :: SPI
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

import io.fabric8.spi.AgentIdentity;
import io.fabric8.spi.AgentRegistration;
import io.fabric8.spi.AgentTopology;
import io.fabric8.spi.process.MutableAgentTopology;
import io.fabric8.spi.process.ProcessIdentity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test the agent types serialization.
 *
 * @author thomas.diesler@jboss.com
 * @since 08-Jun-2014
 */
public class AgentSerializationTest {

    @Test
    public void testAgentIdentity() throws Exception {
        AgentIdentity agentId = AgentIdentity.create("agentId");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(agentId);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Object readObj = new ObjectInputStream(bais).readObject();
        Assert.assertEquals(agentId, readObj);
    }

    @Test
    public void testAgentRegistration() throws Exception {
        AgentIdentity agentId = AgentIdentity.create("agentId");
        AgentRegistration agentReg = new AgentRegistration(agentId, InetAddress.getLocalHost(), "http://jmxServerUrl", "jmxUsername", "jmxPassword");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(agentReg);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Object readObj = new ObjectInputStream(bais).readObject();
        Assert.assertEquals(agentReg, readObj);
    }

    @Test
    public void testProcessIdentity() throws Exception {
        ProcessIdentity procId = ProcessIdentity.create("procId");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(procId);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Object readObj = new ObjectInputStream(bais).readObject();
        Assert.assertEquals(procId, readObj);
    }

    @Test
    public void testAgentTopology() throws Exception {
        MutableAgentTopology mutableTopology = new MutableAgentTopology();
        AgentIdentity agentId = AgentIdentity.create("foo");
        AgentRegistration agentReg = new AgentRegistration(agentId, InetAddress.getLocalHost(), "http://jmxServerUrl", "jmxUsername", "jmxPassword");
        mutableTopology.addAgent(agentReg);
        ProcessIdentity procId = ProcessIdentity.create("procId");
        mutableTopology.addProcess(procId, agentId);
        AgentTopology topology = mutableTopology.immutableTopology();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(topology);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Object readObj = new ObjectInputStream(bais).readObject();
        Assert.assertEquals(topology, readObj);
    }
}
