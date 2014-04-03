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

@Component(service = { ContainerService.class }, configurationPid = ContainerService.FABRIC_SERVICE_PID, configurationPolicy = ConfigurationPolicy.REQUIRE,  immediate = true)
public final class ContainerServiceImpl extends AbstractProtectedComponent<ContainerService> implements ContainerService {

    private final ValidatingReference<ContainerRegistry> containerRegistry = new ValidatingReference<ContainerRegistry>();
    private final ValidatingReference<PermitManager> permitManager = new ValidatingReference<PermitManager>();
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
    protected PermitManager getPermitManager() {
        return permitManager.get();
    }

    @Override
    public ContainerState createContainer(CreateOptions options) {
        assertValid();
        synchronized (containerRegistry) {
            String name = options.getSymbolicName();
            ContainerIdentity identity = ContainerIdentity.create(prefix != null ? prefix + "." + name : name);
            ContainerState containerState = containerRegistry.get().getContainer(identity);
            if (containerState != null)
                throw new IllegalStateException("Container already exists: " + identity);

            return containerRegistry.get().addContainer(identity);
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
            ContainerState container = containerRegistry.get().removeContainer(identity);
            if (container == null)
                throw new IllegalStateException("Container does not exist: " + identity);
            ((ContainerStateImpl) container).destroy();
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
