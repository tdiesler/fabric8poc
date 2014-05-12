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

import io.fabric8.api.AttributeKey;
import io.fabric8.api.ConfigurationItemBuilder;
import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.OptionsProvider;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileItem;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ResourceItemBuilder;
import io.fabric8.spi.DefaultProfileBuilder.DefaultConfigurationItemBuilder;
import io.fabric8.spi.DefaultProfileBuilder.DefaultResourceItemBuilder;
import io.fabric8.spi.utils.IllegalStateAssertion;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.gravia.resource.Version;

public final class DefaultProfileVersionBuilder implements ProfileVersionBuilder {

    private final MutableProfileVersion mutableVersion;

    public DefaultProfileVersionBuilder(Version identity) {
        mutableVersion = new MutableProfileVersion(identity);
    }

    public DefaultProfileVersionBuilder(LinkedProfileVersion linkedVersion) {
        mutableVersion = new MutableProfileVersion(linkedVersion);
    }

    @Override
    public ProfileVersionBuilder identity(Version identity) {
        mutableVersion.setIdentity(identity);
        return this;
    }

    @Override
    public ProfileVersionBuilder addOptions(OptionsProvider<ProfileVersionBuilder> optionsProvider) {
        return optionsProvider.addBuilderOptions(this);
    }

    @Override
    public NestedProfileBuilder withProfile(String identity) {
        Profile linkedProfile = mutableVersion.getLinkedProfile(identity);
        DefaultProfileBuilder profileBuilder;
        if (linkedProfile != null) {
            profileBuilder = new DefaultProfileBuilder(linkedProfile);
        } else {
            Version version = mutableVersion.getIdentity();
            profileBuilder = new DefaultProfileBuilder(identity);
            profileBuilder.profileVersion(version);
        }
        return new DefaultNestedProfileBuilder(this, profileBuilder);
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

    ProfileVersionBuilder addProfile(Profile profile) {
        mutableVersion.addLinkedProfile(profile);
        return this;
    }

    private void validate() {
        Version version = mutableVersion.getIdentity();
        IllegalStateAssertion.requireNotNull(version, "Identity cannot be null");
        Map<String, Profile> linkedProfiles = mutableVersion.getLinkedProfiles();
        IllegalStateAssertion.assertFalse(linkedProfiles.isEmpty(), "Profile version must have at least one profile");
        for (String profileid : linkedProfiles.keySet()) {
            validateLinkedProfile(version, profileid, linkedProfiles);
        }
    }

    private void validateLinkedProfile(Version version, String profileid, Map<String, Profile> linkedProfiles) {
        Profile profile = linkedProfiles.get(profileid);
        IllegalStateAssertion.requireNotNull(profile, "Profile not linked to version: " + profileid);
        IllegalStateAssertion.requireNotNull(profile.getVersion(), "Profile has no version version: " + profileid);
        IllegalStateAssertion.assertEquals(version, profile.getVersion(), "Profile not linked to version: " + version);
        for (String parentid : profile.getParents()) {
            validateLinkedProfile(version, parentid, linkedProfiles);
        }
    }

    private static class MutableProfileVersion implements LinkedProfileVersion {

        private final Map<String, Profile> linkedProfiles = new HashMap<>();
        private Version identity;

        private MutableProfileVersion(Version identity) {
           this.identity = identity;
        }

        public MutableProfileVersion(LinkedProfileVersion linkedVersion) {
            linkedProfiles.putAll(linkedVersion.getLinkedProfiles());
        }

        private LinkedProfileVersion immutableProfileVersion() {
            return new ImmutableProfileVersion(identity, linkedProfiles.keySet(), linkedProfiles);
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

    private static class DefaultNestedProfileBuilder implements NestedProfileBuilder {

        private final DefaultProfileVersionBuilder parent;
        private final DefaultProfileBuilder nested;

        public DefaultNestedProfileBuilder(DefaultProfileVersionBuilder parent, DefaultProfileBuilder nested) {
            this.parent = parent;
            this.nested = nested;
        }

        @Override
        public ProfileVersionBuilder and() {
            parent.addProfile(nested.build());
            return parent;
        }

        @Override
        public DefaultNestedProfileBuilder identity(String identity) {
             nested.identity(identity);
             return this;
        }

        @Override
        public DefaultNestedProfileBuilder profileVersion(Version version) {
            nested.profileVersion(version);
            return this;
        }

        @Override
        public DefaultNestedProfileBuilder addOptions(OptionsProvider<ProfileBuilder> optionsProvider) {
            nested.addOptions(optionsProvider);
            return this;
        }

        @Override
        public DefaultNestedProfileBuilder addProfileItem(ProfileItem item) {
             nested.addProfileItem(item);
            return this;
        }

        @Override
        public DefaultNestedProfileBuilder removeProfileItem(String identity) {
            nested.removeProfileItem(identity);
            return this;
        }

        @Override
        public DefaultNestedProfileBuilder addConfigurationItem(String identity, Map<String, Object> config) {
            nested.addConfigurationItem(identity, config);
            return this;
        }

        @Override
        public ConfigurationItemBuilder<NestedProfileBuilder> withConfigurationItem(String identity) {
            return new DefaultConfigurationItemBuilder<NestedProfileBuilder>(this, identity);
        }

        @Override
        public DefaultNestedProfileBuilder addResourceItem(String identity, InputStream inputStream) {
            nested.addResourceItem(identity, inputStream);
            return this;
        }

        @Override
        public ResourceItemBuilder<NestedProfileBuilder> withResourceItem(String identity) {
            return new DefaultResourceItemBuilder<NestedProfileBuilder>(this, identity);
        }

        @Override
        public DefaultNestedProfileBuilder addParentProfile(String identity) {
            nested.addParentProfile(identity);
            return this;
        }

        @Override
        public DefaultNestedProfileBuilder removeParentProfile(String identity) {
            nested.removeParentProfile(identity);
            return this;
        }

        @Override
        public DefaultNestedProfileBuilder addAttributes(Map<AttributeKey<?>, Object> attributes) {
            nested.addAttributes(attributes);
            return this;
        }

        @Override
        public <V> DefaultNestedProfileBuilder addAttribute(AttributeKey<V> key, V value) {
            nested.addAttribute(key, value);
            return this;
        }
    }
}
