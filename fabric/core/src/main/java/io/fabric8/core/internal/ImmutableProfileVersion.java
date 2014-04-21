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
package io.fabric8.core.internal;

import io.fabric8.api.LockHandle;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileVersion;
import io.fabric8.core.internal.ProfileServiceImpl.ProfileVersionState;
import io.fabric8.spi.AttributeSupport;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.gravia.resource.Version;

/**
 * An immutable profile version
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 *
 * @Immutable
 */
final class ImmutableProfileVersion extends AttributeSupport implements ProfileVersion {

    private final Version identity;
    private final Set<ProfileIdentity> profiles = new HashSet<ProfileIdentity>();
    private final String tostring;

    ImmutableProfileVersion(ProfileVersionState versionState) {
        super(versionState.getAttributes());
        LockHandle readLock = versionState.aquireReadLock();
        try {
            identity = versionState.getIdentity();
            profiles.addAll(versionState.getProfileIdentities());
            tostring = versionState.toString();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Version getIdentity() {
        return identity;
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
