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

import io.fabric8.api.process.ManagedProcess;
import io.fabric8.api.process.MutableManagedProcess;
import io.fabric8.api.process.ProcessIdentity;
import io.fabric8.api.process.ProcessOptions;
import io.fabric8.spi.Agent;
import io.fabric8.spi.process.ImmutableManagedProcess;
import io.fabric8.spi.process.ProcessHandler;
import io.fabric8.spi.process.SelfRegistrationHandler;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;

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
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * The agent controller
 *
 * @author thomas.diesler@jboss.com
 * @since 29-May-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(Agent.class)
public final class AgentService implements Agent {

    private final AtomicInteger processCount = new AtomicInteger();
    private final Map<ProcessIdentity, Registration> registrations = new ConcurrentHashMap<>();

    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<>();
    @Reference(referenceInterface = MBeanServer.class)
    private final ValidatingReference<MBeanServer> mbeanServer = new ValidatingReference<>();
    @Reference(referenceInterface = ProcessHandler.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private final Set<ProcessHandler> processHandlers = new CopyOnWriteArraySet<>();

    @Activate
    void activate(Map<String, Object> config) {
        activateInternal();
    }

    @Deactivate
    void deactivate() {
    }

    private void activateInternal() {

        // Register the self registration handler
        processHandlers.add(new SelfRegistrationHandler());
    }

    @Override
    public Set<String> getProcessHandlers() {
        Set<String> fqnames = new HashSet<>();
        for (ProcessHandler handler : processHandlers) {
            fqnames.add(handler.getClass().getName());
        }
        return fqnames;
    }

    @Override
    public Set<ProcessIdentity> getProcessIdentities() {
        return registrations.keySet();
    }

    @Override
    public ManagedProcess getManagedProcess(ProcessIdentity processId) {
        Registration preg = getRequiredRegistration(processId);
        return new ImmutableManagedProcess(preg.getManagedProcess());
    }

    @Override
    public ManagedProcess createProcess(ProcessOptions options) {
        MutableManagedProcess process = null;
        for (ProcessHandler handler : processHandlers) {
            if (handler.accept(options)) {
                String identitySpec = options.getIdentityPrefix();
                if (identitySpec.endsWith("#")) {
                    identitySpec += processCount.incrementAndGet();
                }
                process = handler.create(options, ProcessIdentity.create(identitySpec));
                registrations.put(process.getIdentity(), new Registration(handler, process));
                break;
            }
        }
        IllegalStateAssertion.assertNotNull(process, "No handler for: " + options);
        return process;
    }

    @Override
    public void startProcess(ProcessIdentity processId) throws LifecycleException {
        Registration preg = getRequiredRegistration(processId);
        MutableManagedProcess process = preg.getManagedProcess();
        preg.getProcessHandler().start(process);
    }

    @Override
    public void stopProcess(ProcessIdentity processId) throws LifecycleException {
        Registration preg = getRequiredRegistration(processId);
        MutableManagedProcess process = preg.getManagedProcess();
        preg.getProcessHandler().stop(process);
    }

    @Override
    public void destroyProcess(ProcessIdentity processId) {
        Registration preg = getRequiredRegistration(processId);
        MutableManagedProcess process = preg.getManagedProcess();
        preg.getProcessHandler().destroy(process);
    }

    private Registration getRequiredRegistration(ProcessIdentity processId) {
        Registration preg = registrations.get(processId);
        IllegalStateAssertion.assertNotNull(preg, "Process not registered: " + processId);
        return preg;
    }

    void bindConfigAdmin(ConfigurationAdmin service) {
        configAdmin.bind(service);
    }
    void unbindConfigAdmin(ConfigurationAdmin service) {
        configAdmin.unbind(service);
    }

    void bindMbeanServer(MBeanServer service) {
        mbeanServer.bind(service);
    }
    void unbindMbeanServer(MBeanServer service) {
        mbeanServer.unbind(service);
    }

    void bindProcessHandler(ProcessHandler service) {
        processHandlers.add(service);
    }
    void unbindProcessHandler(ProcessHandler service) {
        processHandlers.remove(service);
    }

    static class Registration {

        private final ProcessHandler handler;
        private MutableManagedProcess process;

        Registration(ProcessHandler handler, MutableManagedProcess process) {
            this.handler = handler;
            this.process = process;
        }

        ProcessHandler getProcessHandler() {
            return handler;
        }

        MutableManagedProcess getManagedProcess() {
            return process;
        }
    }
}
