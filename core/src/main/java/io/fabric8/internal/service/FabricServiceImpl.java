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
package io.fabric8.internal.service;

import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.internal.api.FabricService;
import io.fabric8.internal.api.PermitManager;
import io.fabric8.internal.scr.AbstractProtectedComponent;
import io.fabric8.internal.scr.ValidatingReference;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { FabricService.class }, configurationPid = Constants.PID, immediate = true)
public final class FabricServiceImpl extends AbstractProtectedComponent<FabricService> implements FabricService {

    private static AtomicInteger INSTANCE_COUNT = new AtomicInteger();
    private final String name = getClass().getSimpleName() + "#" + INSTANCE_COUNT.incrementAndGet();

    private final ValidatingReference<ContainerRegistry> containerRegistry = new ValidatingReference<ContainerRegistry>();
    private String prefix;

    @Activate
    void activate(Map<String, ?> config) {
        prefix = (String) config.get(Constants.KEY_NAME_PREFIX);
        activateComponent(PROTECTED_STATE, this);
    }

    // @Modified not implemented - we get a new compoennt with every config change

    @Deactivate
    void deactivate() {
        deactivateComponent(PROTECTED_STATE);
    }

    @Override
    public String getContainerPrefix() {
        assertValid();
        return prefix;
    }

    @Override
    public Container createContainer(String name) {
        assertValid();
        synchronized (containerRegistry) {
            ContainerState containerState = containerRegistry.get().getContainer(name);
            if (containerState != null)
                throw new IllegalStateException("Container already exists: " + name);

            String prefixedName = prefix != null ? prefix + "." + name : name;
            containerState = containerRegistry.get().addContainer(prefixedName);
            return new ImmutableContainer(containerState);
        }
    }

    @Override
    public Container getContainerByName(String name) {
        assertValid();
        ContainerState containerState = containerRegistry.get().getContainer(name);
        return containerState != null ? new ImmutableContainer(containerState) : null;
    }

    @Override
    public Container startContainer(String name) {
        assertValid();
        ContainerState containerState = assertContainerExists(name);
        containerState.start();
        return new ImmutableContainer(containerState);
    }

    @Override
    public Container stopContainer(String name) {
        assertValid();
        ContainerState containerState = assertContainerExists(name);
        containerState.stop();
        return new ImmutableContainer(containerState);
    }

    @Override
    public Container destroyContainer(String name) {
        assertValid();
        synchronized (containerRegistry) {
            ContainerState containerState = containerRegistry.get().removeContainer(name);
            if (containerState == null)
                throw new IllegalStateException("Container does not exist: " + name);
            containerState.destroy();
            return new ImmutableContainer(containerState);
        }
    }

    private ContainerState assertContainerExists(String name) {
        synchronized (containerRegistry) {
            ContainerState container = containerRegistry.get().getContainer(name);
            if (container == null)
                throw new IllegalStateException("Container does not exist: " + name);
            return container;
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
    protected void bindStateService(PermitManager stateService) {
        super.bindStateService(stateService);
    }
    protected void unbindStateService(PermitManager stateService) {
        super.unbindStateService(stateService);
    }

    @Override
    public String toString() {
        return name;
    }
}
