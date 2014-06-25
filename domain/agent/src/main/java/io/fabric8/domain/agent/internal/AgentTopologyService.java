/*
 * #%L
 * Gravia :: Resolver
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
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
package io.fabric8.domain.agent.internal;

import io.fabric8.spi.AgentIdentity;
import io.fabric8.spi.AgentRegistration;
import io.fabric8.spi.AgentTopology;
import io.fabric8.spi.process.ProcessIdentity;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.Map;
import java.util.Set;

import javax.management.JMException;
import javax.management.MBeanServer;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

/**
 * The agent controller
 *
 * @author thomas.diesler@jboss.com
 * @since 29-May-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(AgentTopology.class)
public final class AgentTopologyService extends AbstractComponent implements AgentTopology {

    @Reference(referenceInterface = MBeanServer.class)
    private final ValidatingReference<MBeanServer> mbeanServer = new ValidatingReference<>();

    private AgentTopology mbeanDelegate;

    @Activate
    void activate() throws Exception {
        activateInternal();
        activateComponent();
    }

    @Deactivate
    void deactivate() throws Exception {
        deactivateInternal();
        deactivateComponent();
    }

    private void activateInternal() throws JMException {
        mbeanDelegate = new AgentTopologyMBean();
        mbeanServer.get().registerMBean(mbeanDelegate, OBJECT_NAME);
    }

    private void deactivateInternal() throws JMException {
        mbeanServer.get().unregisterMBean(OBJECT_NAME);
    }

    @Override
    public Set<AgentRegistration> getAgentRegistrations() {
        return mbeanDelegate.getAgentRegistrations();
    }

    @Override
    public Set<AgentRegistration> addAgentRegistration(AgentRegistration agentReg) {
        return mbeanDelegate.addAgentRegistration(agentReg);
    }

    @Override
    public Set<AgentRegistration> removeAgentRegistration(AgentIdentity agentId) {
        return mbeanDelegate.removeAgentRegistration(agentId);
    }

    @Override
    public AgentRegistration getAgentRegistration(AgentIdentity agentId) {
        return mbeanDelegate.getAgentRegistration(agentId);
    }

    @Override
    public AgentRegistration getProcessAgent(ProcessIdentity processId) {
        return mbeanDelegate.getProcessAgent(processId);
    }

    @Override
    public Map<ProcessIdentity, AgentIdentity> getProcessMapping() {
        return mbeanDelegate.getProcessMapping();
    }

    @Override
    public void addProcessMapping(ProcessIdentity processId, AgentIdentity agentId) {
        mbeanDelegate.addProcessMapping(processId, agentId);
    }

    @Override
    public void removeProcessMapping(ProcessIdentity processId) {
        mbeanDelegate.removeProcessMapping(processId);
    }

    void bindMbeanServer(MBeanServer service) {
        mbeanServer.bind(service);
    }
    void unbindMbeanServer(MBeanServer service) {
        mbeanServer.unbind(service);
    }
}
