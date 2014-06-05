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
import io.fabric8.api.Host;
import io.fabric8.api.ProfileVersion;
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

    ContainerState createContainer(ContainerIdentity parentId, ContainerIdentity identity, CreateOptions options) {
        ContainerState parentState = parentId != null ? getRequiredContainerState(parentId) : null;
        ContainerState cntState = new ContainerState(parentState, identity, options);
        return cntState;
    }

    Set<ContainerIdentity> getContainerIdentities() {
        assertValid();
        return Collections.unmodifiableSet(containers.keySet());
    }

    Set<ContainerState> getContainers(Set<ContainerIdentity> identities) {
        assertValid();
        Set<ContainerState> result = new HashSet<ContainerState>();
        for (Registration creg : containers.values()) {
            ContainerState cntState = creg.getContainerState();
            if (identities == null || identities.contains(cntState.getIdentity())) {
                result.add(cntState);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    void publishContainer(ContainerState cntState) {
        assertValid();
        ContainerIdentity identity = cntState.getIdentity();
        IllegalStateAssertion.assertTrue(getContainerState(identity) == null, "Container already exists: " + identity);
        containers.put(identity, new Registration(cntState));
    }

    void removeContainer(ContainerIdentity identity) {
        assertValid();
        getRequiredContainerState(identity);
        containers.remove(identity);
    }

    ContainerState getContainerState(ContainerIdentity identity) {
        Registration creg = containers.get(identity);
        return creg != null ? creg.getContainerState() : null;
    }

    ContainerState getRequiredContainerState(ContainerIdentity identity) {
        return getRequiredRegistration(identity).getContainerState();
    }

    Registration getRequiredRegistration(ContainerIdentity identity) {
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

    final static class Registration {

        private final ContainerState cntState;
        private final Map<ResourceIdentity, ResourceHandle> resourceHandles = new LinkedHashMap<>();

        private Registration(ContainerState cntState) {
            this.cntState = cntState;
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
    }

    final static class ContainerState {

        private final ContainerState parentState;
        private final ContainerIdentity identity;
        private final AttributeSupport attributes;
        private final List<String> profiles = new ArrayList<>();
        private final Map<ContainerIdentity, ContainerState> children = new HashMap<>();
        private ProfileVersionState versionState;
        private State state;

        private ContainerState(ContainerState parentState, ContainerIdentity identity, CreateOptions options) {
            IllegalArgumentAssertion.assertNotNull(identity, "identity");
            IllegalArgumentAssertion.assertNotNull(options, "options");
            this.parentState = parentState;
            this.identity = identity;
            this.state = State.CREATED;
            this.attributes = new AttributeSupport(options.getAttributes(), true);
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

        Set<String> getManagementDomains() {
            throw new UnsupportedOperationException();
        }

        ContainerState getParentState() {
            return parentState != null ? parentState : null;
        }

        Set<ContainerIdentity> getChildIdentities() {
            assertReadLock();
            return Collections.unmodifiableSet(new HashSet<>(children.keySet()));
        }

        ProfileVersion getProfileVersion() {
            return versionState != null ? versionState.getProfileVersion() : null;
        }

        List<String> getProfileIdentities() {
            assertReadLock();
            return Collections.unmodifiableList(new ArrayList<>(profiles));
        }

        Set<ServiceEndpointIdentity<?>> getServiceEndpointIdentities() {
            return Collections.unmodifiableSet(new HashSet<>(getServiceEndpoints().keySet()));
        }

        @SuppressWarnings("unchecked")
        <T extends ServiceEndpoint> T getServiceEndpoint(Class<T> type) {
            IllegalArgumentAssertion.assertNotNull(type, "type");
            T endpoint = null;
            for (ServiceEndpoint ep : getServiceEndpoints().values()) {
                if (type.isAssignableFrom(ep.getClass())) {
                    if (endpoint == null) {
                        endpoint = (T) ep;
                    } else {
                        LOGGER.warn("Multiple service endpoints of type {} for: {}", type.getName(), identity);
                        endpoint = null;
                        break;
                    }
                }
            }
            return endpoint;
        }

        ServiceEndpoint getServiceEndpoint(ServiceEndpointIdentity<?> identity) {
            return getServiceEndpoints().get(identity);
        }

        Map<ServiceEndpointIdentity<?>, ServiceEndpoint> getServiceEndpoints() {
            Map<ServiceEndpointIdentity<?>, ServiceEndpoint> endpoints = new HashMap<>();
            return Collections.unmodifiableMap(endpoints);
        }

        ImmutableContainer immutableContainer() {
            assertReadLock();
            ImmutableContainer.Builder builder = new ImmutableContainer.Builder(identity, getAttributes(), getState());
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
