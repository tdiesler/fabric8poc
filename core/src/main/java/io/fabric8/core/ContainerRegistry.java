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
package io.fabric8.core;

import io.fabric8.api.ContainerIdentity;
import io.fabric8.core.ContainerServiceImpl.ContainerState;
import io.fabric8.spi.scr.AbstractComponent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = { ContainerRegistry.class }, immediate = true)
public final class ContainerRegistry extends AbstractComponent {

    private final Map<ContainerIdentity, ContainerState> containers = new HashMap<ContainerIdentity, ContainerState>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    Set<ContainerIdentity> getContainerIds() {
        assertValid();
        synchronized (containers) {
            Set<ContainerIdentity> snapshot = new HashSet<ContainerIdentity>(containers.keySet());
            return Collections.unmodifiableSet(snapshot);
        }
    }

    Set<ContainerState> getContainers(Set<ContainerIdentity> identities) {
        assertValid();
        synchronized (containers) {
            Set<ContainerState> result = new HashSet<ContainerState>();
            for (ContainerState aux : containers.values()) {
                if (identities == null || identities.contains(aux.getIdentity())) {
                    result.add(aux);
                }
            }
            return Collections.unmodifiableSet(result);
        }
    }

    ContainerState getContainer(ContainerIdentity identity) {
        assertValid();
        return getContainerInternal(identity);
    }

    ContainerState getRequiredContainer(ContainerIdentity identity) {
        assertValid();
        ContainerState container = getContainerInternal(identity);
        if (container == null)
            throw new IllegalStateException("Container not registered: " + identity);
        return container;
    }

    ContainerState addContainer(ContainerState parentState, ContainerState cntState) {
        assertValid();
        synchronized (containers) {
            ContainerIdentity cntIdentity = cntState.getIdentity();
            if (getContainerInternal(cntIdentity) != null)
                throw new IllegalStateException("Container already exists: " + cntIdentity);

            if (parentState != null) {
                parentState.addChild(cntState);
            }
            containers.put(cntIdentity, cntState);
            return cntState;
        }
    }

    ContainerState removeContainer(ContainerIdentity identity) {
        assertValid();
        synchronized (containers) {
            ContainerState cntState = getRequiredContainer(identity);
            containers.remove(identity);
            ContainerState parentState = cntState.getParent();
            if (parentState != null) {
                parentState.removeChild(identity);
            }
            return cntState;
        }
    }

    private ContainerState getContainerInternal(ContainerIdentity identity) {
        synchronized (containers) {
            return containers.get(identity);
        }
    }
}
