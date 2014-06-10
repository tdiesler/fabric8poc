/*
 * #%L
 * Fabric8 :: Core
 * %%
 * Copyright (C) 2014 Red Hat
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
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.core.ProfileServiceImpl.ProfileVersionState;
import io.fabric8.spi.AttributeSupport;
import io.fabric8.spi.ImmutableContainer;
import io.fabric8.spi.scr.AbstractComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.provision.ResourceHandle;
import org.jboss.gravia.resource.ResourceIdentity;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A registry of stateful {@link Container} instances
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 */
@Component(immediate = true)
@Service(ContainerRegistry.class)
public final class ContainerRegistry extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerRegistry.class);

    private final Map<ContainerIdentity, Registration> containers = new ConcurrentHashMap<ContainerIdentity, Registration>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    Container createContainer(ContainerIdentity parentId, ContainerIdentity identity, CreateOptions options, ProfileVersionState versionState, List<String> profiles, Set<ServiceEndpoint> endpoints) {
        IllegalStateAssertion.assertTrue(getContainerState(identity) == null, "Container already exists: " + identity);
        ContainerState parentState = parentId != null ? getRequiredContainerState(parentId) : null;
        ContainerState cntState = new ContainerState(parentState, identity, options, versionState, profiles, getEndpointIds(endpoints));
        containers.put(identity, new Registration(cntState, endpoints));
        return cntState.immutableContainer();
    }

    private Set<ServiceEndpointIdentity<?>> getEndpointIds(Set<ServiceEndpoint> endpoints) {
        Set<ServiceEndpointIdentity<?>> result = new HashSet<>();
        if (endpoints != null) {
            for (ServiceEndpoint ep : endpoints) {
                result.add(ep.getIdentity());
            }
        }
        return result;
    }

    Set<ContainerIdentity> getContainerIdentities() {
        assertValid();
        return Collections.unmodifiableSet(containers.keySet());
    }

    void addChildToParent(ContainerIdentity parentId, ContainerIdentity childId) {
        ContainerState parentState = getRequiredContainerState(parentId);
        ContainerState childState = getRequiredContainerState(childId);
        parentState.addChild(childState);
    }

    void removeChildFromParent(ContainerIdentity parentId, ContainerIdentity childId) {
        ContainerState parentState = getRequiredContainerState(parentId);
        parentState.removeChild(childId);
    }

    Container getContainer(ContainerIdentity identity) {
        ContainerState cntState = getContainerState(identity);
        return cntState != null ? cntState.immutableContainer() : null;
    }

    boolean hasContainer(ContainerIdentity identity) {
        return containers.containsKey(identity);
    }

    Container getRequiredContainer(ContainerIdentity identity) {
        return getRequiredContainerState(identity).immutableContainer();
    }

    Container startContainer(ContainerIdentity identity) {
        ContainerState cntState = getRequiredContainerState(identity);
        return cntState.start().immutableContainer();
    }

    Container stopContainer(ContainerIdentity identity) {
        ContainerState cntState = getRequiredContainerState(identity);
        return cntState.stop().immutableContainer();
    }

    Container destroyContainer(ContainerIdentity identity) {
        ContainerState cntState = getRequiredContainerState(identity);
        containers.remove(identity);
        return cntState.destroy().immutableContainer();
    }

    Container setProfileVersion(ContainerIdentity identity, ProfileVersionState versionState) {
        ContainerState cntState = getRequiredContainerState(identity);
        cntState.setProfileVersion(versionState);
        return cntState.immutableContainer();
    }

    Container addProfiles(ContainerIdentity identity, List<String> profiles) {
        ContainerState cntState = getRequiredContainerState(identity);
        cntState.addProfiles(profiles);
        return cntState.immutableContainer();
    }

    Container removeProfiles(ContainerIdentity identity, List<String> profiles) {
        ContainerState cntState = getRequiredContainerState(identity);
        cntState.removeProfiles(profiles);
        return cntState.immutableContainer();
    }

    <T extends ServiceEndpoint> T getServiceEndpoint(ContainerIdentity identity, Class<T> type) {
        Registration creg = getRequiredRegistration(identity);
        return creg.getServiceEndpoint(type);
    }

    ServiceEndpoint getServiceEndpoint(ContainerIdentity identity, ServiceEndpointIdentity<?> endpointId) {
        Registration creg = getRequiredRegistration(identity);
        return creg.getServiceEndpoint(endpointId);
    }

    private ContainerState getContainerState(ContainerIdentity identity) {
        Registration creg = containers.get(identity);
        return creg != null ? creg.getContainerState() : null;
    }

    private ContainerState getRequiredContainerState(ContainerIdentity identity) {
        return getRequiredRegistration(identity).getContainerState();
    }

    private Registration getRequiredRegistration(ContainerIdentity identity) {
        Registration creg = containers.get(identity);
        IllegalStateAssertion.assertNotNull(creg, "Container not registered: " + identity);
        return creg;
    }

    Map<ResourceIdentity, ResourceHandle> getResourceHandles(ContainerIdentity identity) {
        return getRequiredRegistration(identity).getResourceHandles();
    }

    void addResourceHandles(ContainerIdentity identity, Map<ResourceIdentity, ResourceHandle> handles) {
        getRequiredRegistration(identity).addResourceHandles(handles);
    }

    void removeResourceHandles(ContainerIdentity identity, Collection<ResourceIdentity> handles) {
        getRequiredRegistration(identity).removeResourceHandles(handles);
    }

    private final static class Registration {

        private final ContainerState cntState;
        private final Map<ResourceIdentity, ResourceHandle> resourceHandles = new LinkedHashMap<>();
        private final Map<ServiceEndpointIdentity<?>, ServiceEndpoint> endpoints = new HashMap<>();

        private Registration(ContainerState cntState, Set<ServiceEndpoint> endpoints) {
            this.cntState = cntState;
            if (endpoints != null) {
                for (ServiceEndpoint ep : endpoints) {
                    this.endpoints.put(ep.getIdentity(), ep);
                }
            }
        }

        ContainerState getContainerState() {
            return cntState;
        }

        Map<ResourceIdentity, ResourceHandle> getResourceHandles() {
            return Collections.unmodifiableMap(resourceHandles);
        }

        void addResourceHandles(Map<ResourceIdentity, ResourceHandle> handles) {
            cntState.assertNotDestroyed();
            cntState.assertWriteLock();
            resourceHandles.putAll(handles);
        }

        void removeResourceHandles(Collection<ResourceIdentity> handles) {
            cntState.assertNotDestroyed();
            cntState.assertWriteLock();
            for (ResourceIdentity resid : handles) {
                resourceHandles.remove(resid);
            }
        }

        @SuppressWarnings("unchecked")
        <T extends ServiceEndpoint> T getServiceEndpoint(Class<T> type) {
            IllegalArgumentAssertion.assertNotNull(type, "type");
            T endpoint = null;
            for (ServiceEndpoint ep : endpoints.values()) {
                if (type.isAssignableFrom(ep.getClass())) {
                    if (endpoint == null) {
                        endpoint = (T) ep;
                    } else {
                        LOGGER.warn("Multiple service endpoints of type {} for: {}", type.getName(), cntState.getIdentity());
                        endpoint = null;
                        break;
                    }
                }
            }
            return endpoint;
        }

        ServiceEndpoint getServiceEndpoint(ServiceEndpointIdentity<?> identity) {
            return endpoints.get(identity);
        }
    }

    private final static class ContainerState {

        private final ContainerState parentState;
        private final ContainerIdentity identity;
        private final CreateOptions createOptions;
        private final AttributeSupport attributes;
        private final List<String> profiles = new ArrayList<>();
        private final Map<ContainerIdentity, ContainerState> children = new HashMap<>();
        private final Set<ServiceEndpointIdentity<?>> endpoints = new HashSet<>();
        private ProfileVersionState versionState;
        private State state;

        private ContainerState(ContainerState parentState, ContainerIdentity identity, CreateOptions options, ProfileVersionState versionState, List<String> profiles, Set<ServiceEndpointIdentity<?>> endpoints) {
            IllegalArgumentAssertion.assertNotNull(identity, "identity");
            IllegalArgumentAssertion.assertNotNull(options, "options");
            IllegalArgumentAssertion.assertNotNull(profiles, "profiles");
            this.createOptions = options;
            this.attributes = new AttributeSupport(options.getAttributes(), true);
            this.endpoints.addAll(endpoints);
            this.profiles.addAll(profiles);
            this.parentState = parentState;
            this.versionState = versionState;
            this.identity = identity;
            this.state = State.CREATED;

        }

        ContainerIdentity getIdentity() {
            return identity;
        }

        State getState() {
            return state;
        }

        Map<AttributeKey<?>, Object> getAttributes() {
            return attributes.getAttributes();
        }

        /*
        Set<AttributeKey<?>> getAttributeKeys() {
            return attributes.getAttributeKeys();
        }

        <T> T getAttribute(AttributeKey<T> key) {
            return attributes.getAttribute(key);
        }

        <T> boolean hasAttribute(AttributeKey<T> key) {
            return attributes.hasAttribute(key);
        }
        */

        Set<ContainerIdentity> getChildIdentities() {
            assertReadLock();
            return Collections.unmodifiableSet(new HashSet<>(children.keySet()));
        }

        List<String> getProfileIdentities() {
            assertReadLock();
            return Collections.unmodifiableList(new ArrayList<>(profiles));
        }

        Set<ServiceEndpointIdentity<?>> getServiceEndpointIdentities() {
            return Collections.unmodifiableSet(endpoints);
        }

        ImmutableContainer immutableContainer() {
            assertReadLock();
            ImmutableContainer.Builder builder = new ImmutableContainer.Builder(identity, createOptions.getRuntimeType(), getAttributes(), getState());
            builder.addParent(parentState != null ? parentState.getIdentity() : null);
            builder.addProfileVersion(versionState != null ? versionState.getIdentity() : null);
            builder.addChildren(getChildIdentities());
            builder.addProfiles(getProfileIdentities());
            builder.addServiceEndpoints(getServiceEndpointIdentities());
            return builder.build();
        }

        void addChild(ContainerState childState) {
            assertNotDestroyed();
            assertWriteLock();
            children.put(childState.getIdentity(), childState);
        }

        void removeChild(ContainerIdentity identity) {
            assertNotDestroyed();
            assertWriteLock();
            children.remove(identity);
        }

        void setProfileVersion(ProfileVersionState versionState) {
            assertNotDestroyed();
            assertWriteLock();
            this.versionState = versionState;
        }

        void addProfiles(List<String> identities) {
            assertNotDestroyed();
            assertWriteLock();
            profiles.addAll(identities);
        }

        void removeProfiles(List<String> identities) {
            assertNotDestroyed();
            assertWriteLock();
            profiles.removeAll(identities);
        }

        ContainerState start() {
            assertNotDestroyed();
            assertWriteLock();
            state = State.STARTED;
            return this;
        }

        ContainerState stop() {
            assertNotDestroyed();
            assertWriteLock();
            state = State.STOPPED;
            return this;
        }

        ContainerState destroy() {
            assertNotDestroyed();
            assertWriteLock();
            state = State.DESTROYED;
            return this;
        }

        private void assertNotDestroyed() {
            IllegalStateAssertion.assertFalse(state == State.DESTROYED, "Container already destroyed: " + this);
        }

        private void assertReadLock() {
            ContainerLockManager.assertReadLock(identity);
        }

        private void assertWriteLock() {
            ContainerLockManager.assertWriteLock(identity);
        }

        @Override
        public String toString() {
            Version profileVersion = versionState != null ? versionState.getIdentity() : null;
            return "ContainerState[id=" + identity + ",state=" + state + ",version=" + profileVersion + "]";
        }
    }
}
