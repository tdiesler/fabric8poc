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
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.HostIdentity;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.core.ContainerServiceImpl.ContainerState;
import io.fabric8.spi.AttributeSupport;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.NotNullException;

final class ImmutableContainer implements Container {

    private final ContainerIdentity identity;
    private final Version profileVersion;
    private final Set<ContainerIdentity> children = new HashSet<ContainerIdentity>();
    private final Set<ProfileIdentity> profiles = new HashSet<ProfileIdentity>();
    private final AttributeSupport attributes;
    private final ContainerIdentity parent;
    private final String tostring;
    private final State state;

    ImmutableContainer(ContainerState cntState) {
        NotNullException.assertValue(cntState, "containerState");
        identity = cntState.getIdentity();
        ContainerState parentState = cntState.getParent();
        parent = parentState != null ? parentState.getIdentity() : null;
        profileVersion = cntState.getProfileVersion();
        state = cntState.getState();
        children.addAll(cntState.getChildContainers());
        profiles.addAll(cntState.getProfiles());
        attributes = new AttributeSupport(cntState.getAttributes());
        tostring = cntState.toString();
    }

    @Override
    public ContainerIdentity getParent() {
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
    public Set<ContainerIdentity> getChildContainers() {
        return Collections.unmodifiableSet(children);
    }

    @Override
    public Version getProfileVersion() {
        return profileVersion;
    }

    @Override
    public Set<ProfileIdentity> getProfileIds() {
        return Collections.unmodifiableSet(profiles);
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
    public Set<String> getManagementDomains() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<ServiceEndpointIdentity> getServiceEndpoints() {
        throw new UnsupportedOperationException();
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
        return tostring;
    }
}
