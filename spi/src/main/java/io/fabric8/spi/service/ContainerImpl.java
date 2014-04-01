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
package io.fabric8.spi.service;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.Container;
import io.fabric8.api.Host;
import io.fabric8.api.Identity;
import io.fabric8.api.JoinOptions;
import io.fabric8.api.Profile;
import io.fabric8.api.ProvisionListener;
import io.fabric8.api.ServiceEndpoint;
import io.fabric8.api.Version;
import io.fabric8.spi.ContainerState;
import io.fabric8.spi.FabricService;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.permit.PermitManager.Permit;

import java.util.List;
import java.util.Set;

final class ContainerImpl implements Container {

    private final PermitManager permitManager;
    private final ContainerState delegate;

    ContainerImpl(PermitManager permitManager, ContainerState delegate) {
        this.permitManager = permitManager;
        this.delegate = delegate;
    }

    @Override
    public Identity getIdentity() {
        return delegate.getIdentity();
    }

    @Override
    public State getState() {
        return delegate.getState();
    }

    @Override
    public void start() {
        Permit<FabricService> permit = permitManager.aquirePermit(FabricService.PERMIT, false);
        try {
            FabricService fabricService = permit.getInstance();
            fabricService.startContainer(getIdentity());
        } finally {
            permit.release();
        }
    }

    @Override
    public void stop() {
        Permit<FabricService> permit = permitManager.aquirePermit(FabricService.PERMIT, false);
        try {
            FabricService fabricService = permit.getInstance();
            fabricService.stopContainer(getIdentity());
        } finally {
            permit.release();
        }
    }

    @Override
    public void destroy() {
        Permit<FabricService> permit = permitManager.aquirePermit(FabricService.PERMIT, false);
        try {
            FabricService fabricService = permit.getInstance();
            fabricService.destroyContainer(getIdentity());
        } finally {
            permit.release();
        }
    }

    @Override
    public Version getVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVersion(Version version, ProvisionListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Profile> getProfiles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Profile getProfile(Identity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasProfile(Identity identity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addProfiles(List<Profile> profiles, ProvisionListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProfiles(List<Profile> profiles, ProvisionListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Host getHost() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Container> getChildren() {
        throw new UnsupportedOperationException();
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
    public void joinFabric(JoinOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void leaveFabric() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Throwable> getFailures() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Throwable> clearFailures() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getManagementDomains() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<ServiceEndpoint> getServiceEndpoints() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean ping() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ContainerImpl)) return false;
        ContainerImpl other = (ContainerImpl) obj;
        return delegate.equals(other);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
