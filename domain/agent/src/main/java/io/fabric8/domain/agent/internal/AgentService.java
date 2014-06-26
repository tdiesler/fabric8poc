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

import static io.fabric8.api.URLServiceEndpoint.JMX_SERVICE_ENDPOINT_IDENTITY;
import static io.fabric8.api.URLServiceEndpoint.JOLOKIA_SERVICE_ENDPOINT_IDENTITY;
import static io.fabric8.domain.agent.AgentLogger.LOGGER;
import io.fabric8.api.Container;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.URLServiceEndpoint;
import io.fabric8.api.process.ProcessOptions;
import io.fabric8.spi.Agent;
import io.fabric8.spi.AgentIdentity;
import io.fabric8.spi.AgentRegistration;
import io.fabric8.spi.AgentTopology;
import io.fabric8.spi.AgentTopology.ProcessMapping;
import io.fabric8.spi.CurrentContainer;
import io.fabric8.spi.NetworkAttributeProvider;
import io.fabric8.spi.RuntimeIdentity;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.process.AbstractProcessHandler;
import io.fabric8.spi.process.ImmutableManagedProcess;
import io.fabric8.spi.process.ManagedProcess;
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

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
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
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.utils.IllegalStateAssertion;

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

    @Reference(referenceInterface = AgentTopology.class)
    private final ValidatingReference<AgentTopology> agentTopology = new ValidatingReference<>();
    @Reference(referenceInterface = CurrentContainer.class)
    private final ValidatingReference<CurrentContainer> currentContainer = new ValidatingReference<>();
    @Reference(referenceInterface = MBeanServer.class)
    private final ValidatingReference<MBeanServer> mbeanServer = new ValidatingReference<>();
    @Reference(referenceInterface = NetworkAttributeProvider.class)
    private final ValidatingReference<NetworkAttributeProvider> networkProvider = new ValidatingReference<>();
    @Reference(referenceInterface = RuntimeService.class)
    private final ValidatingReference<RuntimeService> runtimeService = new ValidatingReference<>();

    // The  {@link ProcessHandlerFactory}s registered with this Agent
    @Reference(referenceInterface = ProcessHandlerFactory.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Set<ProcessHandlerFactory> processHandlerFactories = new CopyOnWriteArraySet<>();

    // The process registrations for this Agent
    private final Map<ProcessIdentity, ProcessRegistration> localProcesses = new ConcurrentHashMap<>();

    private AgentRegistration localAgent;

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

    private void activateInternal() throws Exception {

        String runtimeId = RuntimeIdentity.getIdentity();
        AgentIdentity agentId = AgentIdentity.create(runtimeId);

        // Register this Agent
        Container cnt = currentContainer.get().getCurrentContainer();
        ServiceEndpoint jmxsep = cnt.getServiceEndpoint(JMX_SERVICE_ENDPOINT_IDENTITY);
        String jmxEndpoint = jmxsep.adapt(URLServiceEndpoint.class).getServiceURL();
        ServiceEndpoint jolokiasep = cnt.getServiceEndpoint(JOLOKIA_SERVICE_ENDPOINT_IDENTITY);
        String jolokiaEndpoint = jolokiasep.adapt(URLServiceEndpoint.class).getServiceURL();
        String targetHost = networkProvider.get().getIp();
        String runtimeType = RuntimeType.getRuntimeType().toString();
        localAgent = new AgentRegistration(agentId, runtimeType, targetHost, jmxEndpoint, jolokiaEndpoint);
        agentTopology.get().addAgentRegistration(localAgent);

        LOGGER.info("Bootstrap agent: {}", localAgent);

        // Register this agent with the cluster
        registerAgentWithCluster(localAgent);
    }

    private void deactivateInternal() throws JMException {

        // Unregister this agent from the cluster
        for (AgentRegistration agentReg : agentTopology.get().getAgentRegistrations()) {
            if (!agentReg.equals(localAgent)) {
                try {
                    String remoteAgentUrl = agentReg.getJmxEndpoint();
                    String remoteRuntimeType = agentReg.getRuntimeType();
                    String[] userpass = getJMXCredentials(RuntimeType.valueOf(remoteRuntimeType));
                    JMXConnector jmxConnector = ManagementUtils.getJMXConnector(remoteAgentUrl, userpass[0], userpass[1], 100, TimeUnit.MILLISECONDS);
                    try {
                        MBeanServerConnection con = jmxConnector.getMBeanServerConnection();
                        AgentTopology proxy = ManagementUtils.getMXBeanProxy(con, AgentTopology.OBJECT_NAME, AgentTopology.class);
                        proxy.removeAgentRegistration(localAgent.getIdentity());
                    } finally {
                        jmxConnector.close();
                    }
                } catch (Exception ex) {
                    LOGGER.warn("Cannot unregister agent '" + localAgent + "' from: " + agentReg, ex);
                }
            }
        }
    }

    private void registerAgentWithCluster(AgentRegistration agentReg) throws Exception {
        String remoteAgentUrl = runtimeService.get().getProperty(RuntimeService.PROPERTY_REMOTE_AGENT_URL);
        String remoteRuntimeType = runtimeService.get().getProperty(RuntimeService.PROPERTY_REMOTE_AGENT_TYPE);
        if (remoteAgentUrl != null && remoteRuntimeType != null) {
            String[] userpass = getJMXCredentials(RuntimeType.valueOf(remoteRuntimeType));
            JMXConnector jmxConnector = ManagementUtils.getJMXConnector(remoteAgentUrl, userpass[0], userpass[1], 100, TimeUnit.MILLISECONDS);
            try {
                AgentTopology localTopology = agentTopology.get();
                MBeanServerConnection con = jmxConnector.getMBeanServerConnection();
                AgentTopology proxy = ManagementUtils.getMXBeanProxy(con, AgentTopology.OBJECT_NAME, AgentTopology.class);
                for (AgentRegistration areg : proxy.addAgentRegistration(agentReg)) {
                    localTopology.addAgentRegistration(areg);
                }
                for (ProcessMapping mapping : proxy.getProcessMappings()) {
                    localTopology.addProcessMapping(mapping);
                }
            } finally {
                jmxConnector.close();
            }
        }
    }

    private String[] getJMXCredentials(RuntimeType runtimeType) {
        return RuntimeType.KARAF == runtimeType ? AbstractProcessHandler.karafJmx : AbstractProcessHandler.otherJmx;
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
            agentTopology.get().addProcessMapping(new ProcessMapping(processId, localAgent.getIdentity()));
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

            Future<ManagedProcess> future = preg.getProcessHandler().stop();

            // [TODO] #44 Remove hack that cleans up topology for stopped process
            // [TODO] #40 Make sure that destroying the java process performs an orderly shutdown
            //AgentIdentity agentId = AgentIdentity.create(processId.getName());
            //agentTopology.get().removeAgentRegistration(agentId);

            return future;
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
            agentTopology.get().removeProcessMapping(processId);
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
        AgentRegistration agentReg = agentTopology.get().getProcessAgent(processId);
        IllegalStateAssertion.assertNotNull(agentReg, "Cannot obtain agent registration for: " + processId);
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

    void bindAgentTopology(AgentTopology service) {
        agentTopology.bind(service);
    }

    void unbindAgentTopology(AgentTopology service) {
        agentTopology.unbind(service);
    }

    void bindCurrentContainer(CurrentContainer service) {
        currentContainer.bind(service);
    }

    void unbindCurrentContainer(CurrentContainer service) {
        currentContainer.unbind(service);
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
