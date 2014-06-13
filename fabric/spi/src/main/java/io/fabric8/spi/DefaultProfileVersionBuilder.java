/*
 * #%L
 * Fabric8 :: SPI
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
import io.fabric8.api.OptionsProvider;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.VersionIdentity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.gravia.utils.IllegalStateAssertion;

public final class DefaultProfileVersionBuilder implements ProfileVersionBuilder {

    private final MutableProfileVersion mutableVersion;

    public DefaultProfileVersionBuilder(String canonical) {
        mutableVersion = new MutableProfileVersion(VersionIdentity.createFrom(canonical));
    }

    public DefaultProfileVersionBuilder(VersionIdentity identity) {
        mutableVersion = new MutableProfileVersion(identity);
    }

    public DefaultProfileVersionBuilder(LinkedProfileVersion linkedVersion) {
        mutableVersion = new MutableProfileVersion(linkedVersion);
    }

    @Override
    public ProfileVersionBuilder identity(VersionIdentity identity) {
        mutableVersion.setIdentity(identity);
        return this;
    }

    @Override
    public ProfileVersionBuilder addOptions(OptionsProvider<ProfileVersionBuilder> optionsProvider) {
        return optionsProvider.addBuilderOptions(this);
    }

    @Override
    public ProfileVersionBuilder addProfile(Profile profile) {
        if (profile.getVersion() == null) {
            profile = new ImmutableProfile(mutableVersion.identity, profile.getIdentity(), profile.getAttributes(), profile.getParents(), profile.getProfileItems(null), null);
        }
        mutableVersion.addLinkedProfile(profile);
        return this;
    }

    @Override
    public ProfileVersionBuilder removeProfile(ProfileIdentity identity) {
        mutableVersion.removeLinkedProfile(identity);
        return this;
    }

    @Override
    public LinkedProfileVersion getProfileVersion() {
        validate();
        return mutableVersion.immutableProfileVersion();
    }

    private void validate() {
        VersionIdentity version = mutableVersion.getIdentity();
        IllegalStateAssertion.assertNotNull(version, "Identity cannot be null");
        Map<ProfileIdentity, Profile> linkedProfiles = mutableVersion.getLinkedProfiles();
        IllegalStateAssertion.assertFalse(linkedProfiles.isEmpty(), "Profile version must have at least one profile");
        for (ProfileIdentity profileid : linkedProfiles.keySet()) {
            validateLinkedProfile(version, profileid, linkedProfiles);
        }
    }

    private void validateLinkedProfile(VersionIdentity version, ProfileIdentity profileId, Map<ProfileIdentity, Profile> linkedProfiles) {
        Profile profile = linkedProfiles.get(profileId);
        IllegalStateAssertion.assertNotNull(profile, "Profile not linked to version: " + profileId);
        IllegalStateAssertion.assertNotNull(profile.getVersion(), "Profile has no version version: " + profileId);
        IllegalStateAssertion.assertEquals(version, profile.getVersion(), "Profile not linked to version: " + version);
        for (ProfileIdentity parentid : profile.getParents()) {
            validateLinkedProfile(version, parentid, linkedProfiles);
        }
    }

    private static class MutableProfileVersion implements LinkedProfileVersion {

        private final Map<ProfileIdentity, Profile> linkedProfiles = new HashMap<>();
        private VersionIdentity identity;

        private MutableProfileVersion(VersionIdentity identity) {
           this.identity = identity;
        }

        public MutableProfileVersion(LinkedProfileVersion linkedVersion) {
            linkedProfiles.putAll(linkedVersion.getLinkedProfiles());
            identity = linkedVersion.getIdentity();
        }

        private LinkedProfileVersion immutableProfileVersion() {
            return new ImmutableProfileVersion(identity, linkedProfiles.keySet(), linkedProfiles);
        }

        @Override
        public VersionIdentity getIdentity() {
            return identity;
        }

        private void setIdentity(VersionIdentity identity) {
            this.identity = identity;
        }

        @Override
        public Set<ProfileIdentity> getProfileIdentities() {
            return Collections.unmodifiableSet(linkedProfiles.keySet());
        }

        @Override
        public Profile getLinkedProfile(ProfileIdentity identity) {
            return linkedProfiles.get(identity);
        }

        @Override
        public Map<ProfileIdentity, Profile> getLinkedProfiles() {
            return Collections.unmodifiableMap(linkedProfiles);
        }

        private void addLinkedProfile(Profile profile) {
            linkedProfiles.put(profile.getIdentity(), profile);
        }

        private void removeLinkedProfile(ProfileIdentity identity) {
            linkedProfiles.remove(identity);
        }
    }
}
