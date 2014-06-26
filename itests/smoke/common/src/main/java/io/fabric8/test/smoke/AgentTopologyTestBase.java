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
import io.fabric8.api.ContainerManager;
import io.fabric8.api.ContainerManagerLocator;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.URLServiceEndpoint;
import io.fabric8.jolokia.JSONTypeGenerator;
import io.fabric8.jolokia.JolokiaMXBeanProxy;
import io.fabric8.spi.AgentIdentity;
import io.fabric8.spi.AgentRegistration;
import io.fabric8.spi.AgentTopology;
import io.fabric8.spi.AgentTopology.ProcessMapping;
import io.fabric8.spi.process.ProcessIdentity;
import io.fabric8.spi.utils.OpenTypeGenerator;

import java.util.Arrays;

import javax.management.openmbean.CompositeData;

import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the {@link AgentTopology} MXBean access over Jolokia.
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Jun-2014
 */
public abstract class AgentTopologyTestBase {

    @Before
    public void preConditions() {
        PrePostConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        PrePostConditions.assertPostConditions();
    }

    @Test
    public void testOpenTypes() throws Exception {

        ProcessIdentity procId = ProcessIdentity.create("procId");
        CompositeData cdata = OpenTypeGenerator.toCompositeData(procId);
        Assert.assertEquals(procId, OpenTypeGenerator.fromCompositeData(ProcessIdentity.class, cdata));

        AgentIdentity agentId = AgentIdentity.create("agentId");
        cdata = OpenTypeGenerator.toCompositeData(agentId);
        Assert.assertEquals(agentId, OpenTypeGenerator.fromCompositeData(AgentIdentity.class, cdata));

        ProcessMapping mapping = new ProcessMapping(procId, agentId);
        cdata = OpenTypeGenerator.toCompositeData(mapping);
        Assert.assertEquals(mapping, OpenTypeGenerator.fromCompositeData(ProcessMapping.class, cdata));

        AgentRegistration agentReg = new AgentRegistration(agentId, "OTHER", "localhost", "jmx://endpoint", "jolokia://endpoint");
        cdata = OpenTypeGenerator.toCompositeData(agentReg);
        AgentRegistration wasAgent = OpenTypeGenerator.fromCompositeData(AgentRegistration.class, cdata);
        Assert.assertEquals(agentReg, wasAgent);
        Assert.assertEquals(agentReg.getRuntimeType(), wasAgent.getRuntimeType());
        Assert.assertEquals(agentReg.getTargetHost(), wasAgent.getTargetHost());
        Assert.assertEquals(agentReg.getJmxEndpoint(), wasAgent.getJmxEndpoint());
        Assert.assertEquals(agentReg.getJolokiaEndpoint(), wasAgent.getJolokiaEndpoint());
    }

    @Test
    public void testJSONTypes() throws Exception {

        ProcessIdentity procId = ProcessIdentity.create("procId");
        JSONObject jsonObject = JSONTypeGenerator.toJSONObject(procId);
        Assert.assertEquals(procId, JSONTypeGenerator.fromJSONObject(ProcessIdentity.class, jsonObject));

        AgentIdentity agentId = AgentIdentity.create("agentId");
        jsonObject = JSONTypeGenerator.toJSONObject(agentId);
        Assert.assertEquals(agentId, JSONTypeGenerator.fromJSONObject(AgentIdentity.class, jsonObject));

        ProcessMapping mapping = new ProcessMapping(procId, agentId);
        jsonObject = JSONTypeGenerator.toJSONObject(mapping);
        Assert.assertEquals(mapping, JSONTypeGenerator.fromJSONObject(ProcessMapping.class, jsonObject));

        AgentRegistration agentReg = new AgentRegistration(agentId, "OTHER", "localhost", "jmx://endpoint", "jolokia://endpoint");
        jsonObject = JSONTypeGenerator.toJSONObject(agentReg);
        AgentRegistration wasAgent = JSONTypeGenerator.fromJSONObject(AgentRegistration.class, jsonObject);
        Assert.assertEquals(agentReg, wasAgent);
        Assert.assertEquals(agentReg.getRuntimeType(), wasAgent.getRuntimeType());
        Assert.assertEquals(agentReg.getTargetHost(), wasAgent.getTargetHost());
        Assert.assertEquals(agentReg.getJmxEndpoint(), wasAgent.getJmxEndpoint());
        Assert.assertEquals(agentReg.getJolokiaEndpoint(), wasAgent.getJolokiaEndpoint());
    }

    @Test
    @Ignore
    public void testJolokiaProxy() throws Exception {

        ContainerManager cntManager = ContainerManagerLocator.getContainerManager();
        Container cnt = cntManager.getCurrentContainer();

        ServiceEndpoint sep = cnt.getServiceEndpoint(URLServiceEndpoint.JOLOKIA_SERVICE_ENDPOINT_IDENTITY);
        URLServiceEndpoint urlsep = sep.adapt(URLServiceEndpoint.class);
        String serviceURL = urlsep.getServiceURL();
        Assert.assertNotNull("Jolokia URL not null", serviceURL);

        AgentTopology proxy = JolokiaMXBeanProxy.getMXBeanProxy(serviceURL, AgentTopology.OBJECT_NAME, AgentTopology.class);
        AgentRegistration[] agentRegs = proxy.getAgentRegistrations();
        System.out.println(Arrays.asList(agentRegs));
    }
}
