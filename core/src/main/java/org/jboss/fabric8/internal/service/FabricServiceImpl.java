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
package org.jboss.fabric8.internal.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.fabric8.internal.scr.AbstractComponent;
import org.jboss.fabric8.services.Container;
import org.jboss.fabric8.services.FabricService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

@Component(service = { FabricService.class }, immediate = true)
public final class FabricServiceImpl extends AbstractComponent implements FabricService {

    private static AtomicInteger INSTANCE_COUNT = new AtomicInteger();
    private final String name = getClass().getSimpleName() + "#" + INSTANCE_COUNT.incrementAndGet();

    private final Map<String, Container> containers = new HashMap<String, Container>();

    @Activate
    void activate(Map<String, ?> config) {
        activateComponent();
    }

    @Modified
    void modify(Map<String, ?> config) {
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Container createContainer(String name) {
        assertValid();
        synchronized (containers) {
            MutableContainer container;
            if (containers.containsKey(name))
                throw new IllegalStateException("Container already exists: " + name);

            containers.put(name, container = new MutableContainer(name));
            return container;
        }
    }

    @Override
    public Container getContainerByName(String name) {
        assertValid();
        return getContainerInternal(name);
    }

    @Override
    public void startContainer(Container container) {
        assertValid();
        assertContainerExists(container.getName()).start();
    }

    @Override
    public void stopContainer(Container container) {
        assertValid();
        assertContainerExists(container.getName()).stop();
    }

    @Override
    public void destroyContainer(Container container) {
        assertValid();
        assertContainerExists(container.getName()).destroy();
    }

    private MutableContainer getContainerInternal(String name) {
        synchronized (containers) {
            Container container = containers.get(name);
            return container != null ? MutableContainer.assertMutableContainer(container) : null;
        }
    }

    private MutableContainer assertContainerExists(String name) {
        MutableContainer container = getContainerInternal(name);
        if (container == null)
            throw new IllegalStateException("Container does not exist: " + name);
        return container;
    }

    static final class MutableContainer implements Container {

        private final String name;
        private final AtomicReference<State> state = new AtomicReference<Container.State>();

        MutableContainer(String name) {
            this.name = name;
            this.state.set(State.CREATED);
        }

        static MutableContainer assertMutableContainer(Container container) {
            if (!(container instanceof MutableContainer))
                throw new IllegalArgumentException("Not a mutable container: " + container);
            return (MutableContainer) container;
        }

        @Override
        public String getName() {
            return name;
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
            return "Container[name=" + name + ",state=" + state.get() + "]";
        }
    }
}
