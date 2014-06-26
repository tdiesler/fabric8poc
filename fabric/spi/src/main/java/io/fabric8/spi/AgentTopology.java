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
package io.fabric8.spi;

import java.beans.ConstructorProperties;

import io.fabric8.spi.process.ProcessIdentity;

import javax.management.MXBean;
import javax.management.ObjectName;

import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.ObjectNameFactory;

/**
 * The process/agent topology
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Jun-2014
 */
@MXBean
public interface AgentTopology {

    ObjectName OBJECT_NAME = ObjectNameFactory.create("io.fabric8:type=AgentTopology");

    /** The notification type for agent registration */
    String NOTIFICATION_TYPE_AGENT_REGISTRATION = "AgentRegistration";

    /** The notification type for agent deregistration */
    String NOTIFICATION_TYPE_AGENT_DEREGISTRATION = "AgentDeregistration";

    /**
     * Get the agent registrations for this agent topology
     */
    AgentRegistration[] getAgentRegistrations();

    /**
     * Add an agent registrations to this agent topology
     * @return The currently registered agents
     */
    AgentRegistration[] addAgentRegistration(AgentRegistration agentReg);

    /**
     * Remove an agent registrations from this agent topology
     * @return The currently registered agents
     */
    AgentRegistration[] removeAgentRegistration(AgentIdentity agentIdentity);

    /**
     * Get the agent registration for the given agent id.
     */
    AgentRegistration getAgentRegistration(AgentIdentity agentIdentity);

    /**
     * Get the agent registration for the given process id.
     */
    AgentRegistration getProcessAgent(ProcessIdentity processId);

    /**
     * Get the current process mapping for this agent topology
     */
    ProcessMapping[] getProcessMappings();

    /**
     * Add a process mapping to this agent topology
     */
    ProcessMapping[] addProcessMapping(ProcessMapping processMapping);

    /**
     * Remove a process mapping from this agent topology
     */
    ProcessMapping[] removeProcessMapping(ProcessIdentity processId);

    public final class ProcessMapping {

        private final ProcessIdentity processIdentity;
        private final AgentIdentity agentIdentity;

        @ConstructorProperties( { "processIdentity", "agentIdentity" })
        public ProcessMapping(ProcessIdentity processIdentity, AgentIdentity agentIdentity) {
            IllegalArgumentAssertion.assertNotNull(processIdentity, "processIdentity");
            IllegalArgumentAssertion.assertNotNull(agentIdentity, "agentIdentity");
            this.processIdentity = processIdentity;
            this.agentIdentity = agentIdentity;
        }

        public ProcessIdentity getProcessIdentity() {
           return processIdentity;
        }

        public AgentIdentity getAgentIdentity() {
            return agentIdentity;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof ProcessMapping)) return false;
            ProcessMapping other = (ProcessMapping) obj;
            return processIdentity.equals(other.processIdentity) && agentIdentity.equals(other.agentIdentity);
        }

        @Override
        public String toString() {
            return "ProcessMapping[procId=" + processIdentity + ",agentId=" + agentIdentity + "]";
        }
    }
}