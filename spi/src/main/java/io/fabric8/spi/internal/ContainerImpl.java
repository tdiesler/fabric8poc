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
package io.fabric8.spi.internal;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.HostIdentity;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.spi.ContainerState;

import java.util.Set;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.NotNullException;

final class ContainerImpl implements Container {

    private final ContainerIdentity identity;
    private final State state;

    ContainerImpl(ContainerState delegate) {
        NotNullException.assertValue(delegate, "delegate");
        this.identity = delegate.getIdentity();
        this.state = delegate.getState();
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
    public Set<AttributeKey<?>> getAttributeKeys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getAttribute(AttributeKey<T> key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> boolean hasAttribute(AttributeKey<T> key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HostIdentity getHost() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<ContainerIdentity> getChildContainers() {
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
    public Version getProfileVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<ProfileIdentity> getProfiles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ContainerImpl)) return false;
        ContainerImpl other = (ContainerImpl) obj;
        return other.identity.equals(identity);
    }

    @Override
    public int hashCode() {
        return identity.hashCode();
    }

    @Override
    public String toString() {
        return identity.toString();
    }
}
