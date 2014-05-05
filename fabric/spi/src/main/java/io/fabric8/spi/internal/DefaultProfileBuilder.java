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

import io.fabric8.api.OptionsProvider;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileItem;
import io.fabric8.spi.AbstractAttributableBuilder;
import io.fabric8.spi.AttributeSupport;
import io.fabric8.spi.ImmutableConfigurationProfileItem;
import io.fabric8.spi.ImmutableProfile;
import io.fabric8.spi.utils.IllegalStateAssertion;

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

    DefaultProfileBuilder(Profile sourceProfile) {
        mutableProfile = new MutableProfile(sourceProfile);
    }

    @Override
    public ProfileBuilder identity(String identity) {
        mutableProfile.setIdentity(identity);
        return this;
    }

    @Override
    public ProfileBuilder profileVersion(Version version) {
        mutableProfile.setVersion(version);
        return this;
    }

    @Override
    public ProfileBuilder addOptions(OptionsProvider<ProfileBuilder> optionsProvider) {
        return optionsProvider.addBuilderOptions(this);
    }

    @Override
    public ProfileBuilder addProfileItem(ProfileItem item) {
        mutableProfile.addProfileItem(item);
        return this;
    }

    @Override
    public ProfileBuilder removeProfileItem(String identity) {
        mutableProfile.removeProfileItem(identity);
        return this;
    }

    @Override
    public ProfileBuilder addConfigurationItem(String identity, Map<String, Object> config) {
        mutableProfile.addProfileItem(new ImmutableConfigurationProfileItem(identity, config));
        return this;
    }

    @Override
    public ProfileBuilder addParentProfile(String identity) {
        mutableProfile.addParentProfile(identity);
        return this;
    }

    @Override
    public ProfileBuilder removeParentProfile(String identity) {
        mutableProfile.removeParentProfile(identity);
        return this;
    }

    @Override
    public Profile build() {
        validate();
        return mutableProfile.immutableProfile();
    }

    private void validate() {
        IllegalStateAssertion.assertNotNull(mutableProfile.getIdentity(), "Identity cannot be null");
    }

    private static class MutableProfile extends AttributeSupport implements Profile {

        private final Set<String> parentProfiles = new HashSet<>();
        private final Map<String, ProfileItem> profileItems = new HashMap<>();
        private String identity;
        private Version version;

        private MutableProfile(String identity) {
            this.identity = identity;
        }

        private MutableProfile(Profile sourceProfile) {
            super(sourceProfile.getAttributes());
            identity = sourceProfile.getIdentity();
            version = sourceProfile.getVersion();
            parentProfiles.addAll(sourceProfile.getParents());
            for (ProfileItem item : sourceProfile.getProfileItems(null)) {
                profileItems.put(item.getIdentity(), item);
            }
        }

        private Profile immutableProfile() {
            return new ImmutableProfile(identity, getAttributes(), version, getParents(), getProfileItems(null), null);
        }

        @Override
        public String getIdentity() {
            return identity;
        }

        private void setIdentity(String identity) {
            this.identity = identity;
        }

        @Override
        public Version getVersion() {
            return version;
        }

        private void setVersion(Version version) {
            this.version = version;
        }

        @Override
        public Set<String> getParents() {
            return Collections.unmodifiableSet(parentProfiles);
        }

        private void addParentProfile(String identity) {
            parentProfiles.add(identity);
        }

        private void removeParentProfile(String identity) {
            parentProfiles.remove(identity);
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

        private void addProfileItem(ProfileItem item) {
            profileItems.put(item.getIdentity(), item);
        }

        private void removeProfileItem(String identity) {
            profileItems.remove(identity);
        }
    }
}
