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
package io.fabric8.core.service;

import io.fabric8.core.api.AttributeKey;
import io.fabric8.core.api.ContainerIdentity;
import io.fabric8.core.api.ProfileIdentity;
import io.fabric8.core.api.ProfileVersion;
import io.fabric8.core.service.ProfileServiceImpl.ProfileVersionState;
import io.fabric8.core.spi.AttributeSupport;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.NotNullException;

final class ImmutableProfileVersion implements ProfileVersion {

    private final Version identity;
    private final Set<ContainerIdentity> containers = new HashSet<ContainerIdentity>();
    private final Set<ProfileIdentity> profiles = new HashSet<ProfileIdentity>();
    private final AttributeSupport attributes;
    private final String tostring;

    ImmutableProfileVersion(ProfileVersionState versionState) {
        NotNullException.assertValue(versionState, "versionState");
        identity = versionState.getIdentity();
        containers.addAll(versionState.getContainerIdentities());
        profiles.addAll(versionState.getProfileIdentities());
        attributes = new AttributeSupport(versionState.getAttributes());
        tostring = versionState.toString();
    }

    @Override
    public Version getIdentity() {
        return identity;
    }

    @Override
    public Set<ContainerIdentity> getContainers() {
        return Collections.unmodifiableSet(containers);
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
    public Set<ProfileIdentity> getProfiles() {
        return Collections.unmodifiableSet(profiles);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ImmutableProfileVersion)) return false;
        ImmutableProfileVersion other = (ImmutableProfileVersion) obj;
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
