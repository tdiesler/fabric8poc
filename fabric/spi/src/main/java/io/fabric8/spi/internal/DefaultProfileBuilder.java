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

import io.fabric8.api.ConfigurationProfileItem;
import io.fabric8.api.ConfigurationProfileItemBuilder;
import io.fabric8.api.LinkedProfile;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileItem;
import io.fabric8.api.ProfileItemBuilder;
import io.fabric8.api.ProfileOptionsProvider;
import io.fabric8.spi.AbstractAttributableBuilder;
import io.fabric8.spi.AttributeSupport;
import io.fabric8.spi.utils.IllegalStateAssertion;
import io.fabric8.spi.utils.ProfileUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.gravia.resource.Version;

final class DefaultProfileBuilder extends AbstractAttributableBuilder<ProfileBuilder> implements ProfileBuilder {

    private final MutableProfile mutableProfile;

    DefaultProfileBuilder(String identity) {
        mutableProfile = new MutableProfile(identity);
    }

    DefaultProfileBuilder(LinkedProfile sourceProfile) {
        if (sourceProfile instanceof MutableProfile) {
            mutableProfile = (MutableProfile) sourceProfile;
        } else {
            mutableProfile = new MutableProfile(sourceProfile);
        }
    }

    @Override
    public ProfileBuilder addIdentity(String identity) {
        assertMutable();
        mutableProfile.setIdentity(identity);
        return this;
    }

    @Override
    public ProfileBuilder addProfileVersion(Version version) {
        assertMutable();
        mutableProfile.setVersion(version);
        return this;
    }

    @Override
    public ProfileBuilder addBuilderOptions(ProfileOptionsProvider optionsProvider) {
        assertMutable();
        return optionsProvider.addBuilderOptions(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ProfileItemBuilder<?, ?>> T getProfileItemBuilder(String identity, Class<T> type) {
        assertMutable();
        if (ConfigurationProfileItemBuilder.class.isAssignableFrom(type)) {
            ConfigurationProfileItem item = mutableProfile.getProfileItem(identity, ConfigurationProfileItem.class);
            return (T) (item != null ? new DefaultConfigurationProfileItemBuilder(item) : new DefaultConfigurationProfileItemBuilder(identity));
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    @Override
    public ProfileBuilder addProfileItem(ProfileItem item) {
        assertMutable();
        mutableProfile.addProfileItem(item);
        return this;
    }

    @Override
    public ProfileBuilder removeProfileItem(String symbolicName) {
        assertMutable();
        mutableProfile.removeProfileItem(symbolicName);
        return this;
    }

    @Override
    public ProfileBuilder getParentBuilder(String identity) {
        assertMutable();
        MutableProfile mutableParent = (MutableProfile) mutableProfile.getLinkedParent(identity);
        return mutableParent != null ? new DefaultProfileBuilder(mutableParent) : new DefaultProfileBuilder(identity);
    }

    @Override
    public ProfileBuilder addParentProfile(Profile profile) {
        assertMutable();
        mutableProfile.addParentProfile(profile);
        return this;
    }

    @Override
    public ProfileBuilder removeParentProfile(String identity) {
        assertMutable();
        mutableProfile.removeParentProfile(identity);
        return this;
    }

    @Override
    public LinkedProfile buildProfile() {
        validate();
        makeImmutable();
        return mutableProfile;
    }

    private void validate() {
        IllegalStateAssertion.assertNotNull(mutableProfile.getIdentity(), "Identity cannot be null");
    }

    static class MutableProfile extends AttributeSupport implements LinkedProfile {

        private final Map<String, LinkedProfile> parentProfiles = new HashMap<>();
        private final Map<String, ProfileItem> profileItems = new HashMap<>();
        private String identity;
        private Version version;

        MutableProfile(String identity) {
            this.identity = identity;
        }

        MutableProfile(LinkedProfile linkedProfile) {
            this(linkedProfile, new HashMap<String, LinkedProfile>());
        }

        MutableProfile(LinkedProfile linkedProfile, Map<String, LinkedProfile> linkedProfiles) {
            super(linkedProfile.getAttributes());
            identity = linkedProfile.getIdentity();
            version = linkedProfile.getVersion();
            for (LinkedProfile linkedParent : linkedProfile.getLinkedParents().values()) {
                MutableProfile mutableParent = (MutableProfile) linkedProfiles.get(linkedProfile.getIdentity());
                if (mutableParent == null) {
                    mutableParent = new MutableProfile(linkedParent, linkedProfiles);
                    linkedProfiles.put(mutableParent.getIdentity(), mutableParent);
                }
                parentProfiles.put(mutableParent.getIdentity(), mutableParent);
            }
            for (ProfileItem item : linkedProfile.getProfileItems(null)) {
                profileItems.put(item.getIdentity(), item);
            }
        }

        @Override
        public String getIdentity() {
            return identity;
        }

        @Override
        public Version getVersion() {
            return version;
        }

        @Override
        public Set<String> getParents() {
            return Collections.unmodifiableSet(parentProfiles.keySet());
        }

        @Override
        public Map<String, LinkedProfile> getLinkedParents() {
            return Collections.unmodifiableMap(parentProfiles);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ProfileItem> T getProfileItem(String identity, Class<T> type) {
            return (T) profileItems.get(identity);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ProfileItem> Set<T> getProfileItems(Class<T> type) {
            Set<T> result = new HashSet<T>();
            for (ProfileItem item : profileItems.values()) {
                if (type == null || type.isAssignableFrom(item.getClass())) {
                    result.add((T) item);
                }
            }
            return Collections.unmodifiableSet(result);
        }

        @Override
        public LinkedProfile getEffectiveProfile() {
            return ProfileUtils.getEffectiveProfile(this);
        }

        private LinkedProfile getLinkedParent(String identity) {
            return parentProfiles.get(identity);
        }

        private void setIdentity(String identity) {
            this.identity = identity;
        }

        private void setVersion(Version version) {
            this.version = version;
        }

        private void addParentProfile(Profile profile) {
            parentProfiles.put(profile.getIdentity(), (LinkedProfile) profile);
        }

        private void removeParentProfile(String identity) {
            parentProfiles.remove(identity);
        }

        private void addProfileItem(ProfileItem item) {
            profileItems.put(item.getIdentity(), item);
        }

        private void removeProfileItem(String symbolicName) {
            profileItems.remove(symbolicName);
        }
    }
}
