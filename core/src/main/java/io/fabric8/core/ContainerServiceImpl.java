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
import io.fabric8.api.CreateOptions;
import io.fabric8.api.FabricException;
import io.fabric8.core.ContainerRegistry.ContainerStateImpl;
import io.fabric8.spi.ContainerState;
import io.fabric8.spi.ContainerService;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.scr.AbstractProtectedComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { ContainerService.class }, configurationPid = ContainerService.CONTAINER_SERVICE_PID, configurationPolicy = ConfigurationPolicy.REQUIRE,  immediate = true)
public final class ContainerServiceImpl extends AbstractProtectedComponent<ContainerService> implements ContainerService {

    private final ValidatingReference<ContainerRegistry> containerRegistry = new ValidatingReference<ContainerRegistry>();
    private String prefix;

    @Activate
    void activate(Map<String, ?> config) {
        prefix = (String) config.get(ContainerService.KEY_NAME_PREFIX);
        activateComponent(PERMIT, this);
    }

    // @Modified not implemented - we get a new component with every config change

    @Deactivate
    void deactivate() {
        deactivateComponent(PERMIT);
    }

    @Override
    public ContainerState createContainer(CreateOptions options) {
        assertValid();
        synchronized (containerRegistry) {
            String prefixedName = prefix + "." + options.getSymbolicName();
            ContainerIdentity identity = ContainerIdentity.create(prefixedName);
            return containerRegistry.get().addContainer(identity, null);
        }
    }

    @Override
    public ContainerState createChildContainer(ContainerIdentity parentId, CreateOptions options) {
        assertValid();
        synchronized (containerRegistry) {
            String prefixedName = prefix + "." + options.getSymbolicName();
            ContainerIdentity childId = ContainerIdentity.create(parentId.getSymbolicName() + ":" + prefixedName);
            return containerRegistry.get().addContainer(childId, parentId);
        }
    }

    @Override
    public ContainerState getContainerByName(ContainerIdentity identity) {
        assertValid();
        return containerRegistry.get().getContainer(identity);
    }

    @Override
    public ContainerState startContainer(ContainerIdentity identity) {
        assertValid();
        synchronized (containerRegistry) {
            ContainerState container = getRequiredContainer(identity);
            ((ContainerStateImpl) container).start();
            return container;
        }
    }

    @Override
    public ContainerState stopContainer(ContainerIdentity identity) {
        assertValid();
        synchronized (containerRegistry) {
            ContainerState container = getRequiredContainer(identity);
            ((ContainerStateImpl) container).stop();
            return container;
        }
    }

    @Override
    public ContainerState destroyContainer(ContainerIdentity identity) {
        assertValid();
        synchronized (containerRegistry) {
            ContainerStateImpl container = (ContainerStateImpl) getRequiredContainer(identity);
            if (!container.getChildren().isEmpty()) {
                throw new FabricException("Cannot destroy a container that has active child containers: " + identity);
            }
            containerRegistry.get().removeContainer(identity);
            container.destroy();
            return container;
        }
    }

    private ContainerState getRequiredContainer(ContainerIdentity identity) {
        synchronized (containerRegistry) {
            return containerRegistry.get().getRequiredContainer(identity);
        }
    }

    @Reference
    void bindContainerRegistry(ContainerRegistry service) {
        this.containerRegistry.bind(service);
    }

    void unbindContainerRegistry(ContainerRegistry service) {
        this.containerRegistry.unbind(service);
    }

    @Reference
    void bindPermitManager(PermitManager service) {
        this.permitManager.bind(service);
    }

    void unbindPermitManager(PermitManager service) {
        this.permitManager.unbind(service);
    }
}
