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

import io.fabric8.core.ContainerRegistry.ContainerStateImpl;
import io.fabric8.spi.ContainerState;
import io.fabric8.spi.FabricService;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.scr.AbstractProtectedComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { FabricService.class }, configurationPid = FabricService.FABRIC_SERVICE_PID, immediate = true)
public final class FabricServiceImpl extends AbstractProtectedComponent<FabricService> implements FabricService {

    private static AtomicInteger INSTANCE_COUNT = new AtomicInteger();
    private final String name = getClass().getSimpleName() + "#" + INSTANCE_COUNT.incrementAndGet();

    private final ValidatingReference<ContainerRegistry> containerRegistry = new ValidatingReference<ContainerRegistry>();
    private final ValidatingReference<PermitManager> permitManager = new ValidatingReference<PermitManager>();
    private String prefix;

    @Activate
    void activate(Map<String, ?> config) {
        prefix = (String) config.get(FabricService.KEY_NAME_PREFIX);
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
    public ContainerState createContainer(String name) {
        assertValid();
        synchronized (containerRegistry) {
            ContainerState containerState = containerRegistry.get().getContainer(name);
            if (containerState != null)
                throw new IllegalStateException("Container already exists: " + name);

            String prefixedName = prefix != null ? prefix + "." + name : name;
            return containerRegistry.get().addContainer(prefixedName);
        }
    }

    @Override
    public ContainerState getContainerByName(String name) {
        assertValid();
        return containerRegistry.get().getContainer(name);
    }

    @Override
    public ContainerState startContainer(String name) {
        assertValid();
        synchronized (containerRegistry) {
            ContainerState container = getRequiredContainer(name);
            ((ContainerStateImpl) container).start();
            return container;
        }
    }

    @Override
    public ContainerState stopContainer(String name) {
        assertValid();
        synchronized (containerRegistry) {
            ContainerState container = getRequiredContainer(name);
            ((ContainerStateImpl) container).stop();
            return container;
        }
    }

    @Override
    public ContainerState destroyContainer(String name) {
        assertValid();
        synchronized (containerRegistry) {
            ContainerState container = containerRegistry.get().removeContainer(name);
            if (container == null)
                throw new IllegalStateException("Container does not exist: " + name);
            ((ContainerStateImpl) container).destroy();
            return container;
        }
    }

    private ContainerState getRequiredContainer(String name) {
        synchronized (containerRegistry) {
            return containerRegistry.get().getRequiredContainer(name);
        }
    }

    @Reference
    void bindContainerRegistry(ContainerRegistry registry) {
        this.containerRegistry.bind(registry);
    }

    void unbindContainerRegistry(ContainerRegistry registry) {
        this.containerRegistry.unbind(registry);
    }

    @Reference
    void bindPermitManager(PermitManager stateService) {
        this.permitManager.bind(stateService);
    }

    void unbindPermitManager(PermitManager stateService) {
        this.permitManager.unbind(stateService);
    }

    @Override
    public String toString() {
        return name;
    }

}
