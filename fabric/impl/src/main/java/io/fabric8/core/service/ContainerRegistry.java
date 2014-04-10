/*
 * #%L
 * Gravia :: Integration Tests :: Common
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
package io.fabric8.core.service;

import io.fabric8.core.api.ContainerIdentity;
import io.fabric8.core.api.LockHandle;
import io.fabric8.core.service.ContainerServiceImpl.ContainerState;
import io.fabric8.core.spi.ProfileService;
import io.fabric8.core.spi.scr.AbstractComponent;
import io.fabric8.core.spi.scr.ValidatingReference;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.jboss.gravia.resource.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { ContainerRegistry.class }, immediate = true)
public final class ContainerRegistry extends AbstractComponent {

    private final ValidatingReference<ProfileService> profileService = new ValidatingReference<ProfileService>();

    private final Map<ContainerIdentity, ContainerState> containers = new ConcurrentHashMap<ContainerIdentity, ContainerState>();
    private final Map<ContainerIdentity, ReentrantReadWriteLock> containerLocks = new HashMap<ContainerIdentity, ReentrantReadWriteLock>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    LockHandle aquireWriteLock(ContainerIdentity identity) {
        final WriteLock writeLock;
        final ContainerState cntState;
        synchronized (containers) {
            cntState = getRequiredContainer(identity);
            ReentrantReadWriteLock lock = containerLocks.get(identity);
            if (lock == null)
                throw new IllegalStateException("Cannot obtain write lock for: " + identity);

            writeLock = lock.writeLock();
        }

        boolean success;
        try {
            success = writeLock.tryLock() || writeLock.tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            success = false;
        }
        if (!success)
            throw new IllegalStateException("Cannot obtain write lock in time for: " + identity);

        final LockHandle versionLock;
        Version version = cntState.getProfileVersion();
        if (version != null) {
            try {
                versionLock = profileService.get().aquireProfileVersionLock(version);
            } catch (RuntimeException ex) {
                writeLock.unlock();
                throw ex;
            }
        } else {
            versionLock = null;
        }

        return new LockHandle() {
            @Override
            public void unlock() {
                if (versionLock != null) {
                    versionLock.unlock();
                }
                writeLock.unlock();
            }
        };
    }

    LockHandle aquireReadLock(ContainerIdentity identity) {
        final ReadLock readLock;
        synchronized (containers) {
            ReentrantReadWriteLock lock = containerLocks.get(identity);
            if (lock == null)
                throw new IllegalStateException("Cannot obtain read lock for: " + identity);

            readLock = lock.readLock();
        }

        boolean success;
        try {
            success = readLock.tryLock() || readLock.tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            success = false;
        }
        if (!success)
            throw new IllegalStateException("Cannot obtain read lock in time for: " + identity);

        return new LockHandle() {
            @Override
            public void unlock() {
                readLock.unlock();
            }
        };
    }

    Set<ContainerIdentity> getContainerIds() {
        assertValid();
        return Collections.unmodifiableSet(containers.keySet());
    }

    Set<ContainerState> getContainers(Set<ContainerIdentity> identities) {
        assertValid();
        Set<ContainerState> result = new HashSet<ContainerState>();
        for (ContainerState aux : containers.values()) {
            if (identities == null || identities.contains(aux.getIdentity())) {
                result.add(aux);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    ContainerState getContainer(ContainerIdentity identity) {
        assertValid();
        return getContainerInternal(identity);
    }

    void addContainer(ContainerState parentState, ContainerState cntState) {
        assertValid();
        synchronized (containers) {
            ContainerIdentity identity = cntState.getIdentity();
            if (getContainerInternal(identity) != null)
                throw new IllegalStateException("Container already exists: " + identity);

            if (parentState != null) {
                parentState.addChild(cntState);
            }
            containerLocks.put(identity, new ReentrantReadWriteLock());
            containers.put(identity, cntState);
        }
    }

    ContainerState removeContainer(ContainerIdentity identity) {
        assertValid();
        synchronized (containers) {
            ContainerState cntState = getRequiredContainer(identity);
            containers.remove(identity);
            containerLocks.remove(identity);
            ContainerState parentState = cntState.getParent();
            if (parentState != null) {
                parentState.removeChild(identity);
            }
            return cntState;
        }
    }

    ContainerState getRequiredContainer(ContainerIdentity identity) {
        assertValid();
        ContainerState container = getContainerInternal(identity);
        if (container == null)
            throw new IllegalStateException("Container not registered: " + identity);
        return container;
    }

    private ContainerState getContainerInternal(ContainerIdentity identity) {
        return containers.get(identity);
    }

    @Reference
    void bindProfileService(ProfileService service) {
        profileService.bind(service);
    }

    void unbindProfileService(ProfileService service) {
        profileService.unbind(service);
    }
}
