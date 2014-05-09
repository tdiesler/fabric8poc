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
package io.fabric8.core.internal;

import io.fabric8.api.Container;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.core.internal.ContainerServiceImpl.ContainerState;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.utils.IllegalStateAssertion;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


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

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
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
