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

import io.fabric8.internal.scr.AbstractComponent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = { ContainerRegistry.class }, immediate = true)
public final class ContainerRegistry extends AbstractComponent {

    private final Map<String, ContainerState> containers = new ConcurrentHashMap<String, ContainerState>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    ContainerState getContainer(String name) {
        assertValid();
        return containers.get(name);
    }

    ContainerState getRequiredContainer(String name) {
        assertValid();
        ContainerState container = containers.get(name);
        if (container == null)
            throw new IllegalStateException("Container not registered: " + name);
        return container;
    }

    ContainerState addContainer(String name) {
        assertValid();
        ContainerState container = new ContainerState(name);
        containers.put(name, container);
        return container;
    }

    ContainerState removeContainer(String name) {
        assertValid();
        return containers.remove(name);
    }
}
