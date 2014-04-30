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
import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.Profile;
import io.fabric8.spi.utils.IllegalStateAssertion;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
public final class ImmutableProfileVersion extends AttributeSupport implements LinkedProfileVersion {

    private final Version identity;
    private final Set<String> profileIdentities = new HashSet<String>();
    private Map<String, Profile> linkedProfiles;

    public ImmutableProfileVersion(Version identity, Map<AttributeKey<?>, Object> attributes, Set<String> profileIds, Map<String, Profile> linkedProfiles) {
        super(attributes);
        this.identity = identity;
        this.profileIdentities.addAll(profileIds);
        if (linkedProfiles != null) {
            this.linkedProfiles = new HashMap<>();
            this.linkedProfiles.putAll(linkedProfiles);
        }
    }

    @Override
    public Version getIdentity() {
        return identity;
    }

    @Override
    public Set<String> getProfileIdentities() {
        return Collections.unmodifiableSet(profileIdentities);
    }

    @Override
    public Profile getLinkedProfile(String identity) {
        IllegalStateAssertion.assertNotNull(linkedProfiles, "Linked profiles not available");
        return linkedProfiles.get(identity);
    }

    @Override
    public Map<String, Profile> getLinkedProfiles() {
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
