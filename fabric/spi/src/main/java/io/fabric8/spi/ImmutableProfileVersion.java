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

import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.VersionIdentity;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.gravia.utils.IllegalStateAssertion;

/**
 * An immutable profile version
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 *
 * @Immutable
 */
public final class ImmutableProfileVersion implements LinkedProfileVersion {

    private final VersionIdentity identity;
    private final Set<ProfileIdentity> profileIdentities = new HashSet<>();
    private Map<ProfileIdentity, Profile> linkedProfiles;

    public ImmutableProfileVersion(VersionIdentity version, Set<ProfileIdentity> profileIdentities, Map<ProfileIdentity, Profile> linkedProfiles) {
        this.identity = version;
        this.profileIdentities.addAll(profileIdentities);
        if (linkedProfiles != null) {
            this.linkedProfiles = new HashMap<>();
            this.linkedProfiles.putAll(linkedProfiles);
        }
    }

    @Override
    public VersionIdentity getIdentity() {
        return identity;
    }

    @Override
    public Set<ProfileIdentity> getProfileIdentities() {
        return Collections.unmodifiableSet(profileIdentities);
    }

    @Override
    public Profile getLinkedProfile(ProfileIdentity identity) {
        IllegalStateAssertion.assertNotNull(linkedProfiles, "Linked profiles not available");
        return linkedProfiles.get(identity);
    }

    @Override
    public Map<ProfileIdentity, Profile> getLinkedProfiles() {
        IllegalStateAssertion.assertNotNull(linkedProfiles, "Linked profiles not available");
        return Collections.unmodifiableMap(linkedProfiles);
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
        return "ProfileVersion[" + identity + "]";
    }
}
