/*
 * #%L
 * Fabric8 :: Core
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
package io.fabric8.core;

import io.fabric8.api.Container;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.LockHandle;
import io.fabric8.core.ContainerServiceImpl.ContainerState;
import io.fabric8.spi.scr.AbstractComponent;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;


/**
 * A registry of stateful {@link Container} instances
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 */
@Component(immediate = true)
@Service(ContainerRegistry.class)
public final class ContainerRegistry extends AbstractComponent {

    private final Map<ContainerIdentity, ContainerState> containers = new ConcurrentHashMap<ContainerIdentity, ContainerState>();
    private final Map<ContainerIdentity, ReentrantReadWriteLock> containerLocks = new HashMap<>();
    private final static ThreadLocal<Stack<ContainerIdentity>> readLockAssociation = new ThreadLocal<>();
    private final static ThreadLocal<Stack<ContainerIdentity>> writeLockAssociation = new ThreadLocal<>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    LockHandle aquireWriteLock(ContainerIdentity identity) {
        IllegalArgumentAssertion.assertNotNull(identity, "identity");
        ReentrantReadWriteLock readWriteLock = getReadWriteLock(identity);
        final WriteLock writeLock = readWriteLock.writeLock();

        boolean success;
        try {
            success = writeLock.tryLock(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            success = false;
        }
        IllegalStateAssertion.assertTrue(success, "Cannot obtain write lock in time for: " + identity);

        getLockStack(writeLockAssociation).push(identity);
        return new LockHandle() {
            @Override
            public void unlock() {
                writeLock.unlock();
                getLockStack(writeLockAssociation).pop();
            }
        };
    }

    LockHandle aquireReadLock(ContainerIdentity identity) {
        IllegalArgumentAssertion.assertNotNull(identity, "identity");
        ReentrantReadWriteLock readWriteLock = getReadWriteLock(identity);
        final ReadLock readLock = readWriteLock.readLock();

        boolean success;
        try {
            success = readLock.tryLock(10, TimeUnit.SECONDS);
        } catch (Exception ex) {
            success = false;
        }
        IllegalStateAssertion.assertTrue(success, "Cannot obtain read lock in time for: " + identity);

        getLockStack(readLockAssociation).push(identity);
        return new LockHandle() {
            @Override
            public void unlock() {
                readLock.unlock();
                getLockStack(readLockAssociation).pop();
            }
        };
    }

    static void assertReadLock(ContainerIdentity identity) {
        Stack<ContainerIdentity> stack = getLockStack(writeLockAssociation);
        ContainerIdentity locked = stack.empty() ? null : stack.peek();
        if (locked == null) {
            stack = getLockStack(readLockAssociation);
            locked = stack.empty() ? null : stack.peek();
        }
        IllegalStateAssertion.assertEquals(identity, locked, "Container not locked for read access: " + identity);
    }

    static void assertWriteLock(ContainerIdentity identity) {
        Stack<ContainerIdentity> stack = getLockStack(writeLockAssociation);
        ContainerIdentity locked = stack.empty() ? null : stack.peek();
        IllegalStateAssertion.assertEquals(identity, locked, "Container not locked for write access: " + identity);
    }

    private static Stack<ContainerIdentity> getLockStack(ThreadLocal<Stack<ContainerIdentity>> threadLocal) {
        Stack<ContainerIdentity> stack = threadLocal.get();
        if (stack == null) {
            stack = new Stack<ContainerIdentity>();
            threadLocal.set(stack);
        }
        return stack;
    }

    private ReentrantReadWriteLock getReadWriteLock(ContainerIdentity identity) {
        synchronized (containerLocks) {
            ReentrantReadWriteLock readWriteLock = containerLocks.get(identity);
            if (readWriteLock == null) {
                readWriteLock = new ReentrantReadWriteLock();
                containerLocks.put(identity, readWriteLock);
            }
            return readWriteLock;
        }
    }

    Set<ContainerIdentity> getContainerIdentities() {
        assertValid();
        return Collections.unmodifiableSet(containers.keySet());
    }

    Set<ContainerState> getContainers(Set<ContainerIdentity> identities) {
        assertValid();
        Set<ContainerState> result = new HashSet<ContainerState>();
        if (identities == null) {
            result.addAll(containers.values());
        } else {
            for (ContainerState cntState : containers.values()) {
                if (identities.contains(cntState.getIdentity())) {
                    result.add(cntState);
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    ContainerState getContainer(ContainerIdentity identity) {
        assertValid();
        return getContainerInternal(identity);
    }

    void addContainer(ContainerState cntState) {
        assertValid();
        ContainerIdentity identity = cntState.getIdentity();
        IllegalStateAssertion.assertTrue(getContainerInternal(identity) == null, "Container already exists: " + identity);
        containers.put(identity, cntState);
    }

    ContainerState removeContainer(ContainerIdentity identity) {
        assertValid();
        ContainerState cntState = getRequiredContainer(identity);
        containers.remove(identity);
        return cntState;
    }

    ContainerState getRequiredContainer(ContainerIdentity identity) {
        assertValid();
        ContainerState container = getContainerInternal(identity);
        IllegalStateAssertion.assertNotNull(container, "Container not registered: " + identity);
        return container;
    }

    private ContainerState getContainerInternal(ContainerIdentity identity) {
        return containers.get(identity);
    }
}
