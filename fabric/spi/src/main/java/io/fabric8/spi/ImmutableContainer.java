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
package io.fabric8.spi;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.gravia.resource.Version;

/**
 * An immutable container
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 *
 * @Immutable
 */
public final class ImmutableContainer extends AttributeSupport implements Container {

    private final ContainerIdentity identity;
    private final State state;

    private Version profileVersion;
    private Set<ServiceEndpointIdentity<?>> endpoints = new HashSet<>();
    private Set<ContainerIdentity> children = new HashSet<>();
    private List<String> profiles = new ArrayList<>();
    private ContainerIdentity parent;

    private ImmutableContainer(ContainerIdentity identity, Map<AttributeKey<?>, Object> attributes, State state) {
        super(attributes, true);
        this.identity = identity;
        this.state = state;
    }

    @Override
    public ContainerIdentity getParentIdentity() {
        return parent;
    }

    @Override
    public ContainerIdentity getIdentity() {
        return identity;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public Set<ContainerIdentity> getChildIdentities() {
        return Collections.unmodifiableSet(children);
    }

    @Override
    public Version getProfileVersion() {
        return profileVersion;
    }

    @Override
    public List<String> getProfileIdentities() {
        return Collections.unmodifiableList(profiles);
    }

    @Override
    public InetAddress getHostIdentity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getManagementDomains() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends ServiceEndpoint> Set<ServiceEndpointIdentity<?>> getEndpointIdentities(Class<T> type) {
        Set<ServiceEndpointIdentity<?>> result = new HashSet<>();
        for (ServiceEndpointIdentity<?> epid : endpoints) {
            if (type == null || type.isAssignableFrom(epid.getType())) {
                result.add(epid);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ImmutableContainer)) return false;
        ImmutableContainer other = (ImmutableContainer) obj;
        return other.identity.equals(identity);
    }

    @Override
    public int hashCode() {
        return identity.hashCode();
    }

    @Override
    public String toString() {
        return "Container[id=" + identity + ",state=" + state + ",version=" + profileVersion + "]";
    }

    public static class Builder {
        private final ImmutableContainer container;

        public Builder(ContainerIdentity identity, Map<AttributeKey<?>, Object> attributes, State state) {
            container = new ImmutableContainer(identity, attributes, state);
        }

        public Builder addProfileVersion(Version version) {
            container.profileVersion = version;
            return this;
        }

        public Builder addServiceEndpoints(Set<ServiceEndpointIdentity<?>> endpoints) {
            container.endpoints.addAll(endpoints);
            return this;
        }

        public Builder addChildren(Set<ContainerIdentity> children) {
            container.children.addAll(children);
            return this;
        }

        public Builder addParent(ContainerIdentity parent) {
            container.parent = parent;
            return this;
        }

        public Builder addProfiles(List<String> profiles) {
            container.profiles.addAll(profiles);
            return this;
        }

        public ImmutableContainer build() {
            return container;
        }
    }
}
