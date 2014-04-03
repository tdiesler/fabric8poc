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
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileVersion;
import io.fabric8.core.ProfileServiceImpl.ProfileVersionState;
import io.fabric8.spi.internal.AttributeSupport;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.NotNullException;

final class ImmutableProfileVersion implements ProfileVersion {

    private final Version identity;
    private final Set<ProfileIdentity> profiles = new HashSet<ProfileIdentity>();
    private final AttributeSupport attributes;

    ImmutableProfileVersion(ProfileVersionState profileVersion) {
        NotNullException.assertValue(profileVersion, "profileVersion");
        identity = profileVersion.getIdentity();
        profiles.addAll(profileVersion.getProfileIdentities());
        attributes = new AttributeSupport(profileVersion.getAttributes());
    }

    @Override
    public Version getIdentity() {
        return identity;
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
    public Set<ProfileIdentity> getProfileIdentities() {
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
        return identity.toString();
    }
}
