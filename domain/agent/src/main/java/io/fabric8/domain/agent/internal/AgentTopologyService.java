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

import io.fabric8.api.LockHandle;
import io.fabric8.spi.AgentIdentity;
import io.fabric8.spi.AgentRegistration;
import io.fabric8.spi.AgentTopology;
import io.fabric8.spi.process.ProcessIdentity;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.management.JMException;
import javax.management.MBeanServer;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;

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

    private final Map<AgentIdentity, AgentRegistration> agentMapping = new HashMap<>();
    private final Map<ProcessIdentity, AgentIdentity> processMapping = new HashMap<>();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

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
        mbeanServer.get().registerMBean(this, OBJECT_NAME);
    }

    private void deactivateInternal() throws JMException {
        mbeanServer.get().unregisterMBean(OBJECT_NAME);
    }

    private LockHandle aquireWriteLock() {
        final WriteLock writeLock = readWriteLock.writeLock();
        boolean success;
        try {
            success = writeLock.tryLock() || writeLock.tryLock(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            success = false;
        }
        IllegalStateAssertion.assertTrue(success, "Cannot obtain topology write lock in time");
        return new LockHandle() {
            @Override
            public void unlock() {
                writeLock.unlock();
            }
        };
    }

    private LockHandle aquireReadLock() {
        final ReadLock readLock = readWriteLock.readLock();
        boolean success;
        try {
            success = readLock.tryLock() || readLock.tryLock(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            success = false;
        }
        IllegalStateAssertion.assertTrue(success, "Cannot obtain topology read lock in time");
        return new LockHandle() {
            @Override
            public void unlock() {
                readLock.unlock();
            }
        };
    }

    @Override
    public Set<AgentRegistration> getAgentRegistrations() {
        LockHandle readLock = aquireReadLock();
        try {
            return Collections.unmodifiableSet(new HashSet<>(agentMapping.values()));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Set<AgentRegistration> addAgentRegistration(AgentRegistration agentReg) {
        IllegalArgumentAssertion.assertNotNull(agentReg, "agentReg");
        LockHandle writeLock = aquireWriteLock();
        try {
            agentMapping.put(agentReg.getIdentity(), agentReg);
            return Collections.unmodifiableSet(new HashSet<>(agentMapping.values()));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Set<AgentRegistration> removeAgentRegistration(AgentIdentity agentId) {
        IllegalArgumentAssertion.assertNotNull(agentId, "agentId");
        LockHandle writeLock = aquireWriteLock();
        try {
            agentMapping.remove(agentId);
            return Collections.unmodifiableSet(new HashSet<>(agentMapping.values()));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public AgentRegistration getAgentRegistration(AgentIdentity agentId) {
        LockHandle readLock = aquireReadLock();
        try {
            return agentMapping.get(agentId);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public AgentRegistration getAgentRegistration(ProcessIdentity processId) {
        LockHandle readLock = aquireReadLock();
        try {
            AgentIdentity agentId = processMapping.get(processId);
            return agentId != null ? agentMapping.get(agentId) : null;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Map<ProcessIdentity, AgentIdentity> getProcessMapping() {
        LockHandle readLock = aquireReadLock();
        try {
            return Collections.unmodifiableMap(new HashMap<>(processMapping));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void addProcess(ProcessIdentity processId, AgentIdentity agentId) {
        IllegalArgumentAssertion.assertNotNull(processId, "processId");
        IllegalArgumentAssertion.assertNotNull(agentId, "agentId");
        LockHandle writeLock = aquireWriteLock();
        try {
            getRequiredAgentRegistration(agentId);
            processMapping.put(processId, agentId);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void removeProcess(ProcessIdentity processId) {
        LockHandle writeLock = aquireWriteLock();
        try {
            processMapping.remove(processId);
        } finally {
            writeLock.unlock();
        }
    }

    private AgentRegistration getRequiredAgentRegistration(AgentIdentity agentId) {
        IllegalArgumentAssertion.assertNotNull(agentId, "agentId");
        AgentRegistration agentReg = agentMapping.get(agentId);
        IllegalStateAssertion.assertNotNull(agentReg, "Cannot find agent registration for: " + agentId);
        return agentReg;
    }

    void bindMbeanServer(MBeanServer service) {
        mbeanServer.bind(service);
    }
    void unbindMbeanServer(MBeanServer service) {
        mbeanServer.unbind(service);
    }
}
