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
package io.fabric8.spi.internal;

import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ProfileVersionOptionsProvider;
import io.fabric8.spi.AbstractAttributableBuilder;
import io.fabric8.spi.AttributeSupport;
import io.fabric8.spi.ImmutableProfileVersion;
import io.fabric8.spi.utils.IllegalStateAssertion;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.gravia.resource.Version;

final class DefaultProfileVersionBuilder extends AbstractAttributableBuilder<ProfileVersionBuilder> implements ProfileVersionBuilder {

    private final MutableProfileVersion mutableVersion;

    DefaultProfileVersionBuilder(Version identity) {
        mutableVersion = new MutableProfileVersion(identity);
    }

    @Override
    public ProfileVersionBuilder identity(Version identity) {
        mutableVersion.setIdentity(identity);
        return this;
    }

    @Override
    public ProfileVersionBuilder fromOptionsProvider(ProfileVersionOptionsProvider optionsProvider) {
        return optionsProvider.addBuilderOptions(this);
    }

    @Override
    public ProfileBuilder getProfileBuilder(String identity) {
        Profile linkedProfile = mutableVersion.getLinkedProfile(identity);
        return linkedProfile != null ? new DefaultProfileBuilder(linkedProfile) : new DefaultProfileBuilder(identity);
    }

    @Override
    public NestedProfileBuilder newProfile(String identity) {
        return new ProfileVersionNestedProfileBuilder(this, getProfileBuilder(identity));
    }

    @Override
    public ProfileVersionBuilder addProfile(Profile profile) {
        mutableVersion.addLinkedProfile(profile);
        return this;
    }

    @Override
    public ProfileVersionBuilder removeProfile(String identity) {
        mutableVersion.removeLinkedProfile(identity);
        return this;
    }

    @Override
    public LinkedProfileVersion build() {
        validate();
        return mutableVersion.immutableProfileVersion();
    }

    private void validate() {
        IllegalStateAssertion.assertNotNull(mutableVersion.getIdentity(), "Identity cannot be null");
        Map<String, Profile> linkedProfiles = mutableVersion.getLinkedProfiles();
        for (String profileid : linkedProfiles.keySet()) {
            validateLinkedProfile(profileid, linkedProfiles);
        }
    }

    private void validateLinkedProfile(String profileid, Map<String, Profile> linkedProfiles) {
        Profile profile = linkedProfiles.get(profileid);
        IllegalStateAssertion.assertNotNull(profile, "Profile not linked to version: " + profileid);
        for (String parentid : profile.getParents()) {
            validateLinkedProfile(parentid, linkedProfiles);
        }
    }

    private static class MutableProfileVersion extends AttributeSupport implements LinkedProfileVersion {

        private final Map<String, Profile> linkedProfiles = new HashMap<>();
        private Version identity;

        private MutableProfileVersion(Version identity) {
           this.identity = identity;
        }

        private LinkedProfileVersion immutableProfileVersion() {
            return new ImmutableProfileVersion(identity, getAttributes(), linkedProfiles.keySet(), linkedProfiles);
        }

        @Override
        public Version getIdentity() {
            return identity;
        }

        private void setIdentity(Version identity) {
            this.identity = identity;
        }

        @Override
        public Set<String> getProfileIdentities() {
            return Collections.unmodifiableSet(linkedProfiles.keySet());
        }

        @Override
        public Profile getLinkedProfile(String identity) {
            return linkedProfiles.get(identity);
        }

        @Override
        public Map<String, Profile> getLinkedProfiles() {
            return Collections.unmodifiableMap(linkedProfiles);
        }

        private void addLinkedProfile(Profile profile) {
            linkedProfiles.put(profile.getIdentity(), profile);
        }

        private void removeLinkedProfile(String identity) {
            linkedProfiles.remove(identity);
        }
    }
}
