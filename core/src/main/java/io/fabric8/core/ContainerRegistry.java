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

import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.spi.ContainerState;
import io.fabric8.spi.scr.AbstractComponent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = { ContainerRegistry.class }, immediate = true)
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

    ContainerState getContainer(ContainerIdentity identity) {
        assertValid();
        return containers.get(identity);
    }

    ContainerState getRequiredContainer(ContainerIdentity identity) {
        assertValid();
        ContainerState container = containers.get(identity);
        if (container == null)
            throw new IllegalStateException("Container not registered: " + identity);
        return container;
    }

    ContainerState addContainer(ContainerIdentity identity) {
        assertValid();
        ContainerState container = new ContainerStateImpl(identity);
        containers.put(identity, container);
        return container;
    }

    ContainerState removeContainer(ContainerIdentity identity) {
        assertValid();
        return containers.remove(identity);
    }

    static final class ContainerStateImpl implements ContainerState {

        private final ContainerIdentity identity;
        private final AtomicReference<State> state = new AtomicReference<State>();

        public ContainerStateImpl(ContainerIdentity identity) {
            this.identity = identity;
            this.state.set(State.CREATED);
        }

        @Override
        public ContainerIdentity getIdentity() {
            return identity;
        }

        @Override
        public State getState() {
            return state.get();
        }

        void start() {
            synchronized (state) {
                assertNotDestroyed();
                state.set(State.STARTED);
            }
        }

        void stop() {
            synchronized (state) {
                assertNotDestroyed();
                state.set(State.STOPPED);
            }
        }

        void destroy() {
            synchronized (state) {
                assertNotDestroyed();
                state.set(State.DESTROYED);
            }
        }

        private void assertNotDestroyed() {
            if (state.get() == State.DESTROYED)
                throw new IllegalStateException("Container already destroyed: " + this);
        }

        @Override
        public String toString() {
            return "Container[name=" + identity + ",state=" + state.get() + "]";
        }
    }
}
