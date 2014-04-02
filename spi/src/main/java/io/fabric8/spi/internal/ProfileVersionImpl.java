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
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileVersion;
import io.fabric8.spi.ProfileVersionState;

import java.util.Set;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.NotNullException;

final class ProfileVersionImpl implements ProfileVersion {

    private final Version identity;

    ProfileVersionImpl(ProfileVersionState versionState) {
        NotNullException.assertValue(versionState, "versionState");
        this.identity = versionState.getIdentity();
    }

    ProfileVersionImpl(Version identity) {
        NotNullException.assertValue(identity, "identity");
        this.identity = identity;
    }

    @Override
    public Version getIdentity() {
        return identity;
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
    public Set<ProfileIdentity> getProfiles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ProfileVersionImpl)) return false;
        ProfileVersionImpl other = (ProfileVersionImpl) obj;
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
