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

import io.fabric8.api.process.ProcessOptions;
import io.fabric8.spi.Agent;
import io.fabric8.spi.AgentIdentity;
import io.fabric8.spi.AgentRegistration;
import io.fabric8.spi.AgentTopology;
import io.fabric8.spi.JmxAttributeProvider;
import io.fabric8.spi.NetworkAttributeProvider;
import io.fabric8.spi.RuntimeIdentity;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.process.ImmutableManagedProcess;
import io.fabric8.spi.process.ManagedProcess;
import io.fabric8.spi.process.MutableAgentTopology;
import io.fabric8.spi.process.ProcessHandler;
import io.fabric8.spi.process.ProcessHandlerFactory;
import io.fabric8.spi.process.ProcessIdentity;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;
import io.fabric8.spi.utils.ManagementUtils;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.JMException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.StandardEmitterMBean;
import javax.management.remote.JMXConnector;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.runtime.LifecycleException;
import org.jboss.gravia.utils.IOUtils;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * The agent controller
 *
 * @author thomas.diesler@jboss.com
 * @since 29-May-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(Agent.class)
public final class AgentService extends AbstractComponent implements Agent {

    private final AtomicInteger processCount = new AtomicInteger();

    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<>();
    @Reference(referenceInterface = JmxAttributeProvider.class)
    private final ValidatingReference<JmxAttributeProvider> jmxProvider = new ValidatingReference<>();
    @Reference(referenceInterface = MBeanServer.class)
    private final ValidatingReference<MBeanServer> mbeanServer = new ValidatingReference<>();
    @Reference(referenceInterface = NetworkAttributeProvider.class)
    private final ValidatingReference<NetworkAttributeProvider> networkProvider = new ValidatingReference<>();
    @Reference(referenceInterface = RuntimeService.class)
    private final ValidatingReference<RuntimeService> runtimeService = new ValidatingReference<>();

    // The agent JMX topology
    private final MutableAgentTopology agentTopology = new MutableAgentTopology();

    // The {@link ManagedProcess} registrations for this Agent
    private final Map<ProcessIdentity, ProcessRegistration> localProcesses = new ConcurrentHashMap<>();

    // The  {@link ProcessHandler}s with this Agent
    @Reference(referenceInterface = ProcessHandlerFactory.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Set<ProcessHandlerFactory> processHandlerFactories = new CopyOnWriteArraySet<>();

    private final AtomicLong sequenceNumber = new AtomicLong();
    private StandardEmitterMBean agentMBean;

    private AgentRegistration localAgent;
    private AgentIdentity agentId;

    @Activate
    void activate(Map<String, Object> config) throws Exception {
        activateInternal();
        activateComponent();
    }

    @Deactivate
    void deactivate() throws Exception {
        deactivateInternal();
        deactivateComponent();
    }

    private void activateInternal() throws Exception {

        String runtimeId = RuntimeIdentity.getIdentity();
        agentId = AgentIdentity.create(runtimeId);

        // Register this Agent
        InetAddress targetHost = InetAddress.getByName(networkProvider.get().getIp());
        String jmxServerUrl = jmxProvider.get().getJmxServerUrl();
        String jmxUsername = jmxProvider.get().getJmxUsername();
        String jmxPassword = jmxProvider.get().getJmxPassword();
        localAgent = new AgentRegistration(agentId, targetHost, jmxServerUrl, jmxUsername, jmxPassword);
        agentTopology.addAgent(localAgent);

        // Register the {@link Agent} MBean
        agentMBean = new StandardEmitterMBean(this, Agent.class, new NotificationBroadcasterSupport());
        mbeanServer.get().registerMBean(agentMBean, Agent.OBJECT_NAME);

        // Register this agent with the cluster
        AgentTopology topology = registerAgentWithCluster(localAgent);
        if (topology != null) {
            agentTopology.updateTopology(topology);
        }
    }

    private void deactivateInternal() throws JMException {
        mbeanServer.get().unregisterMBean(OBJECT_NAME);
    }

    private AgentTopology registerAgentWithCluster(AgentRegistration agentReg) throws Exception {

        String jmxRemoteServerUrl = runtimeService.get().getProperty(RuntimeService.PROPERTY_AGENT_JMX_SERVER_URL);
        if (jmxRemoteServerUrl == null)
            return null;

        String jmxRemoteUsername = runtimeService.get().getProperty(RuntimeService.PROPERTY_AGENT_JMX_USERNAME);
        String jmxRemotePassword = runtimeService.get().getProperty(RuntimeService.PROPERTY_AGENT_JMX_PASSWORD);
        JMXConnector jmxConnector = ManagementUtils.getJMXConnector(jmxRemoteServerUrl, jmxRemoteUsername, jmxRemotePassword, 200, TimeUnit.MILLISECONDS);
        try {
            MBeanServerConnection server = jmxConnector.getMBeanServerConnection();
            Agent agent = ManagementUtils.getMBeanProxy(server, Agent.OBJECT_NAME, Agent.class);
            return agent.registerAgent(agentReg);
        } finally {
            IOUtils.safeClose(jmxConnector);
        }
    }

    @Override
    public Set<String> getProcessHandlers() {
        Set<String> fqnames = new HashSet<>();
        for (ProcessHandlerFactory factories : processHandlerFactories) {
            fqnames.add(factories.getClass().getName());
        }
        return fqnames;
    }

    @Override
    public Set<ProcessIdentity> getProcessIdentities() {
        return localProcesses.keySet();
    }

    @Override
    public ManagedProcess getManagedProcess(ProcessIdentity processId) {
        ProcessRegistration preg = getRequiredProcessRegistration(processId);
        return new ImmutableManagedProcess(preg.getManagedProcess());
    }

    @Override
    public ManagedProcess createProcess(ProcessOptions options) {
        if (localAgentIsTarget(options)) {
            ProcessHandler handler = getProcessHandler(options, 10, TimeUnit.SECONDS);
            ProcessIdentity processId = getProcessIdentity(options);
            ManagedProcess process = handler.create(localAgent, options, processId);
            localProcesses.put(processId, new ProcessRegistration(handler, process));
            agentTopology.addProcess(processId, agentId);
            return new ImmutableManagedProcess(process);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private ProcessIdentity getProcessIdentity(ProcessOptions options) {
        String identitySpec = options.getIdentityPrefix();
        if (identitySpec.endsWith("#")) {
            identitySpec += processCount.incrementAndGet();
        }
        return ProcessIdentity.create(identitySpec);
    }

    @Override
    public AgentTopology getAgentTopology() {
        return agentTopology.immutableTopology();
    }

    @Override
    public AgentTopology registerAgent(AgentRegistration agentReg) {
        agentTopology.addAgent(agentReg);
        long seq = sequenceNumber.incrementAndGet();
        agentMBean.sendNotification(new Notification(NOTIFICATION_TYPE_AGENT_REGISTRATION, agentReg, seq, agentReg.toString()));
        return agentTopology.immutableTopology();
    }

    @Override
    public Future<ManagedProcess> startProcess(ProcessIdentity processId) throws LifecycleException {
        if (localAgentIsTarget(processId)) {
            ProcessRegistration preg = getRequiredProcessRegistration(processId);
            return preg.getProcessHandler().start();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Future<ManagedProcess> stopProcess(ProcessIdentity processId) throws LifecycleException {
        if (localAgentIsTarget(processId)) {
            ProcessRegistration preg = getRequiredProcessRegistration(processId);
            return preg.getProcessHandler().stop();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public ManagedProcess destroyProcess(ProcessIdentity processId) {
        if (localAgentIsTarget(processId)) {
            ProcessRegistration preg = getRequiredProcessRegistration(processId);
            ManagedProcess result = preg.getProcessHandler().destroy();
            agentTopology.removeProcess(processId);
            localProcesses.remove(processId);
            return new ImmutableManagedProcess(result);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private ProcessRegistration getRequiredProcessRegistration(ProcessIdentity processId) {
        ProcessRegistration preg = localProcesses.get(processId);
        IllegalStateAssertion.assertNotNull(preg, "Process not registered: " + processId);
        return preg;
    }

    private boolean localAgentIsTarget(ProcessOptions options) {
        InetAddress targetHost = options.getTargetHost();
        return targetHost == null || targetHost.equals(localAgent.getTargetHost());
    }

    private boolean localAgentIsTarget(ProcessIdentity processId) {
        AgentRegistration agentReg = agentTopology.getRequiredAgentRegistration(processId);
        return agentId.equals(agentReg.getIdentity());
    }

    private ProcessHandler getProcessHandler(ProcessOptions options, long timeout, TimeUnit unit) {
        long now = System.currentTimeMillis();
        long end = now + unit.toMillis(timeout);
        while (now < end) {
            for (ProcessHandlerFactory factory : processHandlerFactories) {
                ProcessHandler handler = factory.accept(options);
                if (handler != null) {
                    return handler;
                }
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                // ignore
            }
            now = System.currentTimeMillis();
        }
        throw new IllegalStateException("No handler for: " + options);
    }

    void bindConfigAdmin(ConfigurationAdmin service) {
        configAdmin.bind(service);
    }
    void unbindConfigAdmin(ConfigurationAdmin service) {
        configAdmin.unbind(service);
    }

    void bindJmxProvider(JmxAttributeProvider service) {
        jmxProvider.bind(service);
    }
    void unbindJmxProvider(JmxAttributeProvider service) {
        jmxProvider.unbind(service);
    }

    void bindMbeanServer(MBeanServer service) {
        mbeanServer.bind(service);
    }
    void unbindMbeanServer(MBeanServer service) {
        mbeanServer.unbind(service);
    }

    void bindNetworkProvider(NetworkAttributeProvider service) {
        networkProvider.bind(service);
    }
    void unbindNetworkProvider(NetworkAttributeProvider service) {
        networkProvider.unbind(service);
    }

    void bindProcessHandlerFactory(ProcessHandlerFactory service) {
        processHandlerFactories.add(service);
    }
    void unbindProcessHandlerFactory(ProcessHandlerFactory service) {
        processHandlerFactories.remove(service);
    }

    void bindRuntimeService(RuntimeService service) {
        runtimeService.bind(service);
    }
    void unbindRuntimeService(RuntimeService service) {
        runtimeService.unbind(service);
    }

    static class ProcessRegistration {

        private final ProcessHandler handler;
        private final ManagedProcess process;

        ProcessRegistration(ProcessHandler handler, ManagedProcess process) {
            this.handler = handler;
            this.process = process;
        }

        ProcessHandler getProcessHandler() {
            return handler;
        }

        ManagedProcess getManagedProcess() {
            return process;
        }
    }

}
