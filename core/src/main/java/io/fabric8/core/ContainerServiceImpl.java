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
import io.fabric8.api.Container;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.FabricException;
import io.fabric8.api.Failure;
import io.fabric8.api.HostIdentity;
import io.fabric8.api.JoinOptions;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProvisionListener;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.spi.ContainerService;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.ProfileState;
import io.fabric8.spi.internal.AttributeSupport;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.scr.AbstractProtectedComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.gravia.resource.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { ContainerService.class }, configurationPid = ContainerService.CONTAINER_SERVICE_PID, configurationPolicy = ConfigurationPolicy.REQUIRE,  immediate = true)
public final class ContainerServiceImpl extends AbstractProtectedComponent<ContainerService> implements ContainerService {


    private String prefix;
    private final ValidatingReference<ContainerRegistry> containerRegistry = new ValidatingReference<ContainerRegistry>();
    private final ValidatingReference<ProfileService> profileService = new ValidatingReference<ProfileService>();

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
    public Container createContainer(CreateOptions options) {
        assertValid();
        String prefixedName = prefix + "." + options.getSymbolicName();
        ContainerIdentity cntIdentity = ContainerIdentity.create(prefixedName);
        ContainerState cntState = new ContainerState(null, cntIdentity);
        containerRegistry.get().addContainer(null, cntState);
        return new ImmutableContainer(cntState);
    }

    @Override
    public Container createChildContainer(ContainerIdentity parentId, CreateOptions options) {
        assertValid();
        ContainerState cntParent = getRequiredContainer(parentId);
        String prefixedName = prefix + "." + options.getSymbolicName();
        ContainerIdentity cntIdentity = ContainerIdentity.create(parentId.getSymbolicName() + ":" + prefixedName);
        ContainerState cntState = new ContainerState(cntParent, cntIdentity);
        containerRegistry.get().addContainer(parentId, cntState);
        return new ImmutableContainer(cntState);
    }

    @Override
    public Container getContainer(ContainerIdentity identity) {
        assertValid();
        ContainerState cntState = containerRegistry.get().getContainer(identity);
        return cntState != null ? new ImmutableContainer(cntState) : null;
    }

    @Override
    public Container start(ContainerIdentity identity) {
        assertValid();
        ContainerState cntState = getRequiredContainer(identity);
        synchronized (cntState) {
            cntState.start();
            return new ImmutableContainer(cntState);
        }
    }

    @Override
    public Container stop(ContainerIdentity identity) {
        assertValid();
        ContainerState cntState = getRequiredContainer(identity);
        synchronized (cntState) {
            cntState.stop();
            return new ImmutableContainer(cntState);
        }
    }

    @Override
    public Container destroy(ContainerIdentity identity) {
        assertValid();
        ContainerState cntState = getRequiredContainer(identity);
        synchronized (cntState) {
            if (!cntState.getChildContainers().isEmpty()) {
                throw new FabricException("Cannot destroy a container that has active child containers: " + identity);
            }
            containerRegistry.get().removeContainer(identity);
            cntState.destroy();
            return new ImmutableContainer(cntState);
        }
    }

    @Override
    public Set<ContainerIdentity> getContainerIdentities() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Container> getContainers(Set<ContainerIdentity> identities) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Container getCurrentContainer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVersion(ContainerIdentity identity, Version version, ProvisionListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean ping(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void joinFabric(ContainerIdentity identity, JoinOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void leaveFabric(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addProfiles(ContainerIdentity identity, List<ProfileIdentity> profiles, ProvisionListener listener) {
        assertValid();
        ContainerState cntState = getRequiredContainer(identity);
        synchronized (cntState) {
            Version profVersion = cntState.getProfileVersion();
            List<ProfileState> profileStates = new ArrayList<ProfileState>();
            for (ProfileIdentity profid : profiles) {
                ProfileState profState = profileService.get().getProfile(profVersion, profid);
                profileStates.add(profState);
            }
            cntState.addProfiles(profileStates);
        }
    }

    @Override
    public void removeProfiles(ContainerIdentity identity, List<ProfileIdentity> profiles, ProvisionListener listener) {
        assertValid();
        ContainerState cntState = getRequiredContainer(identity);
        synchronized (cntState) {
            cntState.removeProfiles(profiles);
        }
    }

    @Override
    public List<Failure> getFailures(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Failure> clearFailures(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    private ContainerState getRequiredContainer(ContainerIdentity identity) {
        return containerRegistry.get().getRequiredContainer(identity);
    }

    @Reference
    void bindContainerRegistry(ContainerRegistry service) {
        this.containerRegistry.bind(service);
    }

    void unbindContainerRegistry(ContainerRegistry service) {
        this.containerRegistry.unbind(service);
    }

    @Reference
    void bindProfileService(ProfileService service) {
        this.profileService.bind(service);
    }

    void unbindProfileService(ProfileService service) {
        this.profileService.unbind(service);
    }

    @Reference
    void bindPermitManager(PermitManager service) {
        this.permitManager.bind(service);
    }

    void unbindPermitManager(PermitManager service) {
        this.permitManager.unbind(service);
    }

    static final class ContainerState implements Container {

        private final ContainerIdentity identity;
        private final AttributeSupport attributes = new AttributeSupport();
        private final Map<ContainerIdentity, ContainerState> children = new HashMap<ContainerIdentity, ContainerState>();
        private final Map<ProfileIdentity, ProfileState> profiles = new HashMap<ProfileIdentity, ProfileState>();
        private final AtomicReference<Version> profileVersion = new AtomicReference<Version>();
        private final AtomicReference<State> state = new AtomicReference<Container.State>();
        private ContainerState parent;

        ContainerState(ContainerState parent, ContainerIdentity identity) {
            this.parent = parent;
            this.identity = identity;
            this.profileVersion.set(Constants.DEFAULT_PROFILE_VERSION);
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

        Map<AttributeKey<?>, Object> getAttributes() {
            return attributes.getAttributes();
        }

        @Override
        public Version getProfileVersion() {
            return profileVersion.get();
        }

        @Override
        public Set<AttributeKey<?>> getAttributeKeys() {
            return attributes.getAttributeKeys();
        }

        @Override
        public <T> T getAttribute(AttributeKey<T> key) {
            return attributes.getAttribute(key);
        }

        @Override
        public <T> boolean hasAttribute(AttributeKey<T> key) {
            return attributes.hasAttribute(key);
        }

        @Override
        public HostIdentity getHost() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ContainerIdentity getParent() {
            return parent != null ? parent.getIdentity() : null;
        }

        @Override
        public Set<ContainerIdentity> getChildContainers() {
            return children.keySet();
        }

        @Override
        public Set<String> getManagementDomains() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<ServiceEndpointIdentity> getServiceEndpoints() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<ProfileIdentity> getProfiles() {
            return profiles.keySet();
        }

        // Package protected. Adding/Removing a container and setting the parent/child relationship is an atomic operation
        ContainerState getParentState() {
            return parent;
        }

        // Package protected. Adding/Removing a container and setting the parent/child relationship is an atomic operation
        void addChild(ContainerState childState) {
            children.put(childState.getIdentity(), childState);
        }

        // Package protected. Adding/Removing a container and setting the parent/child relationship is an atomic operation
        void removeChild(ContainerIdentity childIdentity) {
            children.remove(childIdentity);
        }

        private void addProfiles(List<ProfileState> profileStates) {
            for (ProfileState profState : profileStates) {
                profiles.put(profState.getIdentity(), profState);
            }
        }

        private void removeProfiles(List<ProfileIdentity> profileIdentitites) {
            for (ProfileIdentity profId : profileIdentitites) {
                profiles.remove(profId);
            }
        }

        private void start() {
            assertNotDestroyed();
            state.set(State.STARTED);
        }

        private void stop() {
            assertNotDestroyed();
            state.set(State.STOPPED);
        }

        private void destroy() {
            assertNotDestroyed();
            state.set(State.DESTROYED);
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
