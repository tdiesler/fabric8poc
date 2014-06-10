package io.fabric8.spi.process;

import io.fabric8.spi.AgentIdentity;
import io.fabric8.spi.AgentRegistration;
import io.fabric8.spi.AgentTopology;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;

/**
 * The process/agent topology
 *
 * @author thomas.diesler@jboss.com
 * @since 07-Jun-2014
 */
public final class MutableAgentTopology implements AgentTopology {

    private final Map<AgentIdentity, AgentRegistration> agentTopology = new ConcurrentHashMap<>();
    private final Map<ProcessIdentity, AgentIdentity> processTopology = new ConcurrentHashMap<>();

    @Override
    public Map<AgentIdentity, AgentRegistration> getAgentRegistrations() {
        return Collections.unmodifiableMap(new HashMap<>(agentTopology));
    }

    @Override
    public AgentRegistration getAgentRegistration(AgentIdentity agentId) {
        return agentTopology.get(agentId);
    }

    @Override
    public AgentRegistration getAgentRegistration(ProcessIdentity processId) {
        AgentIdentity agentId = processTopology.get(processId);
        return agentId != null ? agentTopology.get(agentId) : null;
    }

    @Override
    public Map<ProcessIdentity, AgentIdentity> getProcessMapping() {
        return Collections.unmodifiableMap(new HashMap<>(processTopology));
    }

    public void addAgent(AgentRegistration agentReg) {
        IllegalArgumentAssertion.assertNotNull(agentReg, "agentReg");
        agentTopology.put(agentReg.getIdentity(), agentReg);
    }

    public AgentRegistration removeAgent(AgentIdentity agentId) {
        IllegalArgumentAssertion.assertNotNull(agentId, "agentId");
        return agentTopology.remove(agentId);
    }

    public void addProcess(ProcessIdentity processId, AgentIdentity agentId) {
        IllegalArgumentAssertion.assertNotNull(processId, "processId");
        IllegalArgumentAssertion.assertNotNull(agentId, "agentId");
        getRequiredAgentRegistration(agentId);
        processTopology.put(processId, agentId);
    }

    public void removeProcess(ProcessIdentity processId) {
        processTopology.remove(processId);
    }

    public void updateTopology(AgentTopology topology) {
        agentTopology.putAll(topology.getAgentRegistrations());
        processTopology.putAll(topology.getProcessMapping());
    }

    public AgentTopology immutableTopology() {
        return new ImmutableAgentTopology(agentTopology, processTopology);
    }

    public AgentRegistration getRequiredAgentRegistration(AgentIdentity agentId) {
        IllegalArgumentAssertion.assertNotNull(agentId, "agentId");
        AgentRegistration agentReg = agentTopology.get(agentId);
        IllegalStateAssertion.assertNotNull(agentReg, "Cannot find agent registration for: " + agentId);
        return agentReg;
    }

    public AgentRegistration getRequiredAgentRegistration(ProcessIdentity processId) {
        IllegalArgumentAssertion.assertNotNull(processId, "processId");
        AgentIdentity agentId = processTopology.get(processId);
        IllegalStateAssertion.assertNotNull(agentId, "Cannot find agent identity for: " + processId);
        return getRequiredAgentRegistration(agentId);
    }

    private static final class ImmutableAgentTopology implements AgentTopology, Serializable {

        private static final long serialVersionUID = 7107739953897138959L;

        private final Map<AgentIdentity, AgentRegistration> agentTopology = new HashMap<>();
        private final Map<ProcessIdentity, AgentIdentity> processTopology = new HashMap<>();

        ImmutableAgentTopology(Map<AgentIdentity, AgentRegistration> agents, Map<ProcessIdentity, AgentIdentity> processes) {
            agentTopology.putAll(agents);
            processTopology.putAll(processes);
        }

        @Override
        public Map<AgentIdentity, AgentRegistration> getAgentRegistrations() {
            return Collections.unmodifiableMap(new HashMap<>(agentTopology));
        }

        @Override
        public AgentRegistration getAgentRegistration(AgentIdentity agentId) {
            return agentTopology.get(agentId);
        }

        @Override
        public AgentRegistration getAgentRegistration(ProcessIdentity processId) {
            AgentIdentity agentId = processTopology.get(processId);
            return agentId != null ? agentTopology.get(agentId) : null;
        }

        @Override
        public Map<ProcessIdentity, AgentIdentity> getProcessMapping() {
            return Collections.unmodifiableMap(new HashMap<>(processTopology));
        }

        @Override
        public int hashCode() {
            return agentTopology.hashCode() + processTopology.hashCode();
        }

        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof ImmutableAgentTopology)) return false;
            ImmutableAgentTopology other = (ImmutableAgentTopology) obj;
            return agentTopology.equals(other.agentTopology) && processTopology.equals(other.processTopology);
        }

        public String toString() {
            return "AgentTopology" + processTopology;
        }
    }
}