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

import io.fabric8.api.AttributeKey;
import io.fabric8.api.Constants;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.spi.ContainerState;
import io.fabric8.spi.ProfileState;
import io.fabric8.spi.internal.AttributeSupport;
import io.fabric8.spi.scr.AbstractComponent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.gravia.resource.Version;
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

    ContainerState getContainer(ContainerIdentity identity) {
        assertValid();
        return getContainerInternal(identity);
    }

    private ContainerState getContainerInternal(ContainerIdentity identity) {
        synchronized (containers) {
            return containers.get(identity);
        }
    }

    ContainerState getRequiredContainer(ContainerIdentity identity) {
        assertValid();
        ContainerState container = getContainerInternal(identity);
        if (container == null)
            throw new IllegalStateException("Container not registered: " + identity);
        return container;
    }

    ContainerState addContainer(ContainerIdentity identity, ContainerIdentity parentId) {
        assertValid();
        synchronized (containers) {
            ContainerStateImpl container = (ContainerStateImpl) getContainerInternal(identity);
            if (container != null)
                throw new IllegalStateException("Container already exists: " + identity);

            ContainerStateImpl parent = parentId != null ? (ContainerStateImpl) getRequiredContainer(parentId) : null;
            container = new ContainerStateImpl(identity);
            containers.put(identity, container);
            if (parent != null) {
                container.setParent(parent);
                parent.addChild(container);
            }
            return container;
        }
    }

    ContainerState removeContainer(ContainerIdentity identity) {
        assertValid();
        synchronized (containers) {
            ContainerState child = getRequiredContainer(identity);
            ContainerStateImpl parent = (ContainerStateImpl) child.getParent();
            if (parent != null) {
                parent.removeChild(child);
            }
            containers.remove(identity);
            return child;
        }
    }

    static final class ContainerStateImpl implements ContainerState {

        private final ContainerIdentity identity;
        private final AttributeSupport attributes = new AttributeSupport();
        private final Version profileVersion = Constants.DEFAULT_PROFILE_VERSION;
        private final AtomicReference<State> state = new AtomicReference<State>();
        private final Set<ContainerState> children = new HashSet<ContainerState>();
        private final AtomicReference<ContainerState> parent = new AtomicReference<ContainerState>();
        private final Set<ProfileState> profiles = new HashSet<ProfileState>();

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

        @Override
        public Map<AttributeKey<?>, Object> getAttributes() {
            return attributes.getAttributes();
        }

        @Override
        public Version getProfileVersion() {
            return profileVersion;
        }

        @Override
        public ContainerState getParent() {
            return parent.get();
        }

        void setParent(ContainerState parent) {
            this.parent.set(parent);
        }

        @Override
        public Set<ContainerState> getChildren() {
            synchronized (children) {
                return Collections.unmodifiableSet(children);
            }
        }

        void addChild(ContainerState child) {
            synchronized (children) {
                children.add(child);
            }
        }

        void removeChild(ContainerState child) {
            synchronized (children) {
                children.remove(child);
            }
        }

        @Override
        public Set<ProfileState> getProfiles() {
            synchronized (profiles) {
                return Collections.unmodifiableSet(profiles);
            }
        }

        void addProfile(ProfileState profile) {
            synchronized (profiles) {
                profiles.add(profile);
            }
        }

        void removeProfile(ProfileState profile) {
            synchronized (profiles) {
                profiles.remove(profile);
            }
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
