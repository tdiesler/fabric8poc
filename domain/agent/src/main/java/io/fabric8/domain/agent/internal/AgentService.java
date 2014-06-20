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

import static io.fabric8.domain.agent.internal.AgentLogger.LOGGER;
import io.fabric8.api.process.ProcessOptions;
import io.fabric8.spi.Agent;
import io.fabric8.spi.AgentIdentity;
import io.fabric8.spi.AgentRegistration;
import io.fabric8.spi.AgentTopology;
import io.fabric8.spi.HttpAttributeProvider;
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

import java.net.InetAddress;
import java.net.URL;
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
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.StandardEmitterMBean;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.runtime.LifecycleException;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pResponse;
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
    @Reference(referenceInterface = HttpAttributeProvider.class)
    private final ValidatingReference<HttpAttributeProvider> httpProvider = new ValidatingReference<>();
    @Reference(referenceInterface = JmxAttributeProvider.class)
    private final ValidatingReference<JmxAttributeProvider> jmxProvider = new ValidatingReference<>();
    @Reference(referenceInterface = JolokiaService.class)
    private final ValidatingReference<JolokiaService> jolokiaService = new ValidatingReference<>();
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
        AgentIdentity agentId = AgentIdentity.create(runtimeId);

        // Register this Agent
        InetAddress targetHost = InetAddress.getByName(networkProvider.get().getIp());
        URL jolokiaAgentUrl = new URL(httpProvider.get().getHttpUrl() + "/jolokia");
        String jolokiaUsername = null;
        String jolokiaPassword = null;
        localAgent = new AgentRegistration(agentId, targetHost, jolokiaAgentUrl, jolokiaUsername, jolokiaPassword);
        agentTopology.addAgent(localAgent);

        LOGGER.info("Bootstrap agent: {}", localAgent);

        // Register the {@link Agent} MBean
        agentMBean = new StandardEmitterMBean(this, Agent.class, new NotificationBroadcasterSupport());
        mbeanServer.get().registerMBean(agentMBean, Agent.OBJECT_NAME);

        // Register this agent with the cluster
        //String jolokiaAgentUrl = runtimeService.get().getProperty(RuntimeService.PROPERTY_JOLOKIA_AGENT_URL);
        AgentTopology topology = registerAgentWithCluster(null, localAgent);
        if (topology != null) {
            agentTopology.updateTopology(topology);
        }
    }

    private void deactivateInternal() throws JMException {

        // Unregister this agent from the cluster
        for (AgentRegistration agentReg : agentTopology.getAgentRegistrations().values()) {
            if (!agentReg.equals(localAgent)) {
                try {
                    J4pClient client = new J4pClient(agentReg.getJolokiaAgentUrl().toExternalForm());
                    J4pExecRequest req = new J4pExecRequest(Agent.OBJECT_NAME, "unregisterAgent", localAgent.getIdentity());
                    client.execute(req);
                } catch (Exception ex) {
                    LOGGER.warn("Cannot unregister agent '" + localAgent + "' from: " + agentReg, ex);
                }
            }
        }

        // Unregister the {@link Agent} MBean
        mbeanServer.get().unregisterMBean(OBJECT_NAME);
    }

    private AgentTopology registerAgentWithCluster(URL jolokiaAgentUrl, AgentRegistration agentReg) throws Exception {

        if (jolokiaAgentUrl == null)
            return null;

        // Remote Jolokia invocation
        J4pClient client = new J4pClient(jolokiaAgentUrl.toExternalForm());
        J4pExecRequest req = new J4pExecRequest(Agent.OBJECT_NAME, "registerAgent", agentReg);
        J4pResponse<J4pExecRequest> res = client.execute(req);
        LOGGER.info("Receive agent topology: {}", res.asJSONObject());
        return res.getValue();
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
            LOGGER.info("Create process: {}", options);
            ProcessHandler handler = getProcessHandler(options, 10, TimeUnit.SECONDS);
            ProcessIdentity processId = getProcessIdentity(options);
            ManagedProcess process = handler.create(options, processId);
            localProcesses.put(processId, new ProcessRegistration(handler, process));
            agentTopology.addProcess(processId, localAgent.getIdentity());
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
        LOGGER.info("Register agent: {}", agentReg);
        agentTopology.addAgent(agentReg);
        long seq = sequenceNumber.incrementAndGet();
        agentMBean.sendNotification(new Notification(NOTIFICATION_TYPE_AGENT_REGISTRATION, agentReg, seq, "Agent registered: " + agentReg));
        return agentTopology.immutableTopology();
    }

    @Override
    public AgentTopology unregisterAgent(AgentIdentity agentId) {
        LOGGER.info("Unregister agent: {}", agentId);
        AgentRegistration agentReg = agentTopology.removeAgent(agentId);
        long seq = sequenceNumber.incrementAndGet();
        agentMBean.sendNotification(new Notification(NOTIFICATION_TYPE_AGENT_DEREGISTRATION, agentId, seq, "Agent deregistered: " + agentReg));
        return agentTopology.immutableTopology();
    }

    @Override
    public Future<ManagedProcess> startProcess(ProcessIdentity processId) throws LifecycleException {
        if (localAgentIsTarget(processId)) {
            ProcessRegistration preg = getRequiredProcessRegistration(processId);
            LOGGER.info("Start process: {}", preg);
            return preg.getProcessHandler().start();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Future<ManagedProcess> stopProcess(ProcessIdentity processId) throws LifecycleException {
        if (localAgentIsTarget(processId)) {
            ProcessRegistration preg = getRequiredProcessRegistration(processId);
            LOGGER.info("Stop process: {}", preg);

            // [TODO] #44 Remove hack that cleans up topology for stopped process
            AgentIdentity agentId = AgentIdentity.create(processId.getName());
            agentTopology.removeAgent(agentId);

            return preg.getProcessHandler().stop();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public ManagedProcess destroyProcess(ProcessIdentity processId) {
        if (localAgentIsTarget(processId)) {
            ProcessRegistration preg = getRequiredProcessRegistration(processId);
            LOGGER.info("Destroy process: {}", preg);
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
        return localAgent.getIdentity().equals(agentReg.getIdentity());
    }

    private ProcessHandler getProcessHandler(ProcessOptions options, long timeout, TimeUnit unit) {
        long now = System.currentTimeMillis();
        long end = now + unit.toMillis(timeout);
        while (now < end) {
            for (ProcessHandlerFactory factory : processHandlerFactories) {
                if (factory.accept(options)) {
                    return factory.createProcessHandler(mbeanServer.get(), localAgent);
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

    void bindHttpProvider(HttpAttributeProvider service) {
        httpProvider.bind(service);
    }
    void unbindHttpProvider(HttpAttributeProvider service) {
        httpProvider.unbind(service);
    }

    void bindJmxProvider(JmxAttributeProvider service) {
        jmxProvider.bind(service);
    }
    void unbindJmxProvider(JmxAttributeProvider service) {
        jmxProvider.unbind(service);
    }

    void bindJolokiaService(JolokiaService service) {
        jolokiaService.bind(service);
    }
    void unbindJolokiaService(JolokiaService service) {
        jolokiaService.unbind(service);
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
