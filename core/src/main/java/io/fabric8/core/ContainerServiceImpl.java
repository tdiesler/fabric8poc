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
import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.Failure;
import io.fabric8.api.Host;
import io.fabric8.api.JoinOptions;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEvent.EventType;
import io.fabric8.api.ProvisionListener;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.spi.ContainerService;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.internal.AttributeSupport;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.scr.AbstractProtectedComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

import org.jboss.gravia.resource.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { ContainerService.class }, configurationPid = Container.CONTAINER_SERVICE_PID, configurationPolicy = ConfigurationPolicy.REQUIRE,  immediate = true)
public final class ContainerServiceImpl extends AbstractProtectedComponent<ContainerService> implements ContainerService {

    private final ValidatingReference<ContainerRegistry> containerRegistry = new ValidatingReference<ContainerRegistry>();
    private final ValidatingReference<ProfileService> profileService = new ValidatingReference<ProfileService>();
    private final ValidatingReference<EventDispatcher> eventDispatcher = new ValidatingReference<EventDispatcher>();

    private String configToken;

    @Activate
    void activate(Map<String, ?> config) {
        configToken = (String) config.get(Container.CNFKEY_CONFIG_TOKEN);
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
        return createContainerInternal(null, options, null);
    }

    @Override
    public Container createContainer(ContainerIdentity parentId, CreateOptions options, ProvisionListener listener) {
        assertValid();
        return createContainerInternal(getRequiredContainerState(parentId), options, listener);
    }

    private Container createContainerInternal(ContainerState parentState, CreateOptions options, ProvisionListener listener) {
        ContainerState cntState = new ContainerState(parentState, options, configToken);
        synchronized (containerRegistry) {
            containerRegistry.get().addContainer(parentState, cntState);
            if (parentState != null) {
                parentState.addChild(cntState);
            }
        }
        Profile defaultProfile = profileService.get().getDefaultProfile();
        setVersionInternal(cntState, defaultProfile.getProfileVersion(), listener);
        addProfilesInternal(cntState, Collections.singletonList(defaultProfile.getIdentity()), listener);
        return new ImmutableContainer(cntState);
    }

    @Override
    public Container getContainer(ContainerIdentity identity) {
        assertValid();
        synchronized (containerRegistry) {
            ContainerState cntState = containerRegistry.get().getContainer(identity);
            return cntState != null ? new ImmutableContainer(cntState) : null;
        }
    }

    @Override
    public Container start(ContainerIdentity identity) {
        assertValid();
        ContainerState cntState = getRequiredContainerState(identity);
        synchronized (cntState) {
            cntState.start();
            return new ImmutableContainer(cntState);
        }
    }

    @Override
    public Container stop(ContainerIdentity identity) {
        assertValid();
        ContainerState cntState = getRequiredContainerState(identity);
        synchronized (cntState) {
            cntState.stop();
            return new ImmutableContainer(cntState);
        }
    }

    @Override
    public Container destroy(ContainerIdentity identity) {
        assertValid();
        synchronized (containerRegistry) {
            ContainerState cntState = getRequiredContainerState(identity);
            synchronized (cntState) {
                if (!cntState.getChildContainers().isEmpty()) {
                    throw new IllegalStateException("Cannot destroy a container that has active child containers: " + identity);
                }
                ContainerState parentState = cntState.getParent();
                if (parentState != null) {
                    parentState.removeChild(identity);
                }
                containerRegistry.get().removeContainer(identity);
                cntState.destroy();
                return new ImmutableContainer(cntState);
            }
        }
    }

    @Override
    public Set<ContainerIdentity> getContainerIds() {
        assertValid();
        synchronized (containerRegistry) {
            return containerRegistry.get().getContainerIds();
        }
    }

    @Override
    public Set<Container> getContainers(Set<ContainerIdentity> identities) {
        assertValid();
        Set<Container> result = new HashSet<Container>();
        synchronized (containerRegistry) {
            for (ContainerState aux : containerRegistry.get().getContainers(identities)) {
                if (identities == null || identities.contains(aux.getIdentity())) {
                    result.add(new ImmutableContainer(aux));
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Container getCurrentContainer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVersion(ContainerIdentity identity, Version version, ProvisionListener listener) {
        assertValid();
        ContainerState cntState = getRequiredContainerState(identity);
        setVersionInternal(cntState, version, listener);
    }

    private void setVersionInternal(ContainerState cntState, Version version, ProvisionListener listener) {
        synchronized (cntState) {
            Lock lock = profileService.get().aquireProfileVersionLock(version);
            try {
                Container cnt = new ImmutableContainer(cntState);
                Set<ProfileIdentity> cntProfiles = cntState.getProfiles();
                for (Profile profile : profileService.get().getProfiles(version, cntProfiles)) {
                    ProvisionEvent event = new ProvisionEvent(cnt, profile, EventType.PROVISIONING);
                    eventDispatcher.get().dispatchEvent(event, listener);

                    // do provisioning stuff

                    event = new ProvisionEvent(cnt, profile, EventType.PROVISIONED);
                    eventDispatcher.get().dispatchEvent(event, listener);
                }
                cntState.setProfileVersion(version);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void addProfiles(ContainerIdentity identity, List<ProfileIdentity> profileIds, ProvisionListener listener) {
        assertValid();
        ContainerState cntState = getRequiredContainerState(identity);
        addProfilesInternal(cntState, profileIds, listener);
    }

    private void addProfilesInternal(ContainerState cntState, List<ProfileIdentity> profileIds, ProvisionListener listener) {
        synchronized (cntState) {
            Lock lock = profileService.get().aquireProfileVersionLock(cntState.getProfileVersion());
            try {
                cntState.addProfiles(profileIds);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void removeProfiles(ContainerIdentity identity, List<ProfileIdentity> profiles, ProvisionListener listener) {
        assertValid();
        ContainerState cntState = getRequiredContainerState(identity);
        removeProfilesInternal(cntState, profiles, listener);
    }

    private void removeProfilesInternal(ContainerState cntState, List<ProfileIdentity> profileIds, ProvisionListener listener) {
        synchronized (cntState) {
            Lock lock = profileService.get().aquireProfileVersionLock(cntState.getProfileVersion());
            try {
                cntState.removeProfiles(profileIds);
            } finally {
                lock.unlock();
            }
        }
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
    public List<Failure> getFailures(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Failure> clearFailures(ContainerIdentity identity) {
        throw new UnsupportedOperationException();
    }

    private ContainerState getRequiredContainerState(ContainerIdentity identity) {
        return containerRegistry.get().getRequiredContainer(identity);
    }

    @Reference
    void bindContainerRegistry(ContainerRegistry service) {
        containerRegistry.bind(service);
    }

    void unbindContainerRegistry(ContainerRegistry service) {
        containerRegistry.unbind(service);
    }

    @Reference
    void bindProfileService(ProfileService service) {
        profileService.bind(service);
    }

    void unbindProfileService(ProfileService service) {
        profileService.unbind(service);
    }

    @Reference
    void bindEventDispatcher(EventDispatcher service) {
        eventDispatcher.bind(service);
    }

    void unbindEventDispatcher(EventDispatcher service) {
        eventDispatcher.unbind(service);
    }

    @Reference
    void bindPermitManager(PermitManager service) {
        permitManager.bind(service);
    }

    void unbindPermitManager(PermitManager service) {
        permitManager.unbind(service);
    }

    static final class ContainerState {

        private final ContainerState parent;
        private final ContainerIdentity identity;
        private final AttributeSupport attributes;
        private final Map<ContainerIdentity, ContainerState> children = new HashMap<ContainerIdentity, ContainerState>();
        private final Set<ProfileIdentity> profiles = new HashSet<ProfileIdentity>();
        private final AtomicReference<Version> profileVersion = new AtomicReference<Version>();
        private final AtomicReference<State> state = new AtomicReference<State>();

        ContainerState(ContainerState parent, CreateOptions options, String configToken) {
            this.parent = parent;
            this.state.set(State.CREATED);
            String parentName = parent != null ? parent.getIdentity().getSymbolicName() + ":" : "";
            this.identity = ContainerIdentity.create(parentName + options.getSymbolicName());
            this.attributes = new AttributeSupport(options.getAttributes());
            this.attributes.putAttribute(Container.ATTKEY_CONFIG_TOKEN, configToken);
        }

        ContainerIdentity getIdentity() {
            return identity;
        }

        State getState() {
            return state.get();
        }

        Version getProfileVersion() {
            return profileVersion.get();
        }

        Map<AttributeKey<?>, Object> getAttributes() {
            return attributes.getAttributes();
        }

        Set<AttributeKey<?>> getAttributeKeys() {
            return attributes.getAttributeKeys();
        }

        <T> T getAttribute(AttributeKey<T> key) {
            return attributes.getAttribute(key);
        }

        <T> boolean hasAttribute(AttributeKey<T> key) {
            return attributes.hasAttribute(key);
        }

        Host getHost() {
            throw new UnsupportedOperationException();
        }

        ContainerState getParent() {
            return parent != null ? parent : null;
        }

        Set<ContainerIdentity> getChildContainers() {
            return Collections.unmodifiableSet(new HashSet<ContainerIdentity>(children.keySet()));
        }

        Set<String> getManagementDomains() {
            throw new UnsupportedOperationException();
        }

        Set<ServiceEndpointIdentity> getServiceEndpoints() {
            throw new UnsupportedOperationException();
        }

        Set<ProfileIdentity> getProfiles() {
            return Collections.unmodifiableSet(profiles);
        }

        // NOTE - Methods that mutate this objects should be private
        // Only the {@link ContainerService} is supposed to mutate the ContainerState

        private void addChild(ContainerState childState) {
            assertNotDestroyed();
            children.put(childState.getIdentity(), childState);
        }

        private void removeChild(ContainerIdentity childIdentity) {
            assertNotDestroyed();
            children.remove(childIdentity);
        }

        private void setProfileVersion(Version version) {
            assertNotDestroyed();
            profileVersion.set(version);
        }

        private void addProfiles(List<ProfileIdentity> profileIds) {
            assertNotDestroyed();
            profiles.addAll(profileIds);
        }

        private void removeProfiles(List<ProfileIdentity> profileIdentitites) {
            assertNotDestroyed();
            profiles.removeAll(profiles);
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
