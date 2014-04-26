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

import io.fabric8.api.LinkedProfile;
import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ProfileVersionOptionsProvider;
import io.fabric8.spi.AbstractAttributableBuilder;
import io.fabric8.spi.AttributeSupport;
import io.fabric8.spi.internal.DefaultProfileBuilder.MutableProfile;
import io.fabric8.spi.utils.IllegalStateAssertion;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.gravia.resource.Version;

final class DefaultProfileVersionBuilder extends AbstractAttributableBuilder<ProfileVersionBuilder> implements ProfileVersionBuilder {

    private final MutableProfileVersion mutableVersion;

    DefaultProfileVersionBuilder() {
        mutableVersion = new MutableProfileVersion();
    }

    DefaultProfileVersionBuilder(LinkedProfileVersion linkedVersion) {
        mutableVersion = new MutableProfileVersion(linkedVersion);
    }

    @Override
    public ProfileVersionBuilder addIdentity(Version identity) {
        assertMutable();
        mutableVersion.setIdentity(identity);
        return this;
    }

    @Override
    public ProfileVersionBuilder addBuilderOptions(ProfileVersionOptionsProvider optionsProvider) {
        assertMutable();
        return optionsProvider.addBuilderOptions(this);
    }

    @Override
    public ProfileBuilder getProfileBuilder(String identity) {
        assertMutable();
        MutableProfile mutableProfile = (MutableProfile) mutableVersion.getLinkedProfile(identity);
        return mutableProfile != null ? new DefaultProfileBuilder(mutableProfile) : new DefaultProfileBuilder().addIdentity(identity);
    }

    @Override
    public ProfileVersionBuilder addProfile(Profile profile) {
        assertMutable();
        mutableVersion.addProfile(profile);
        return this;
    }

    @Override
    public ProfileVersionBuilder removeProfile(String identity) {
        assertMutable();
        mutableVersion.removeProfile(identity);
        return this;
    }

    @Override
    public ProfileVersion getProfileVersion() {
        validate();
        makeImmutable();
        return mutableVersion;
    }

    private void validate() {
        IllegalStateAssertion.assertNotNull(mutableVersion.getIdentity(), "Identity cannot be null");
    }

    static class MutableProfileVersion extends AttributeSupport implements LinkedProfileVersion {

        private final Map<String, LinkedProfile> linkedProfiles = new HashMap<>();
        private Version identity;

        MutableProfileVersion() {
        }

        MutableProfileVersion(LinkedProfileVersion linkedVersion) {
            identity = linkedVersion.getIdentity();
            for (LinkedProfile linkedProfile : linkedVersion.getLinkedProfiles().values()) {
                MutableProfile mutableProfile = (MutableProfile) linkedProfiles.get(linkedProfile.getIdentity());
                if (mutableProfile == null) {
                    mutableProfile = new MutableProfile(linkedProfile, linkedProfiles);
                }
                linkedProfiles.put(mutableProfile.getIdentity(), mutableProfile);
            }
        }

        @Override
        public Version getIdentity() {
            return identity;
        }

        void setIdentity(Version identity) {
            this.identity = identity;
        }

        @Override
        public Set<String> getProfileIdentities() {
            return Collections.unmodifiableSet(linkedProfiles.keySet());
        }

        @Override
        public LinkedProfile getLinkedProfile(String identity) {
            return linkedProfiles.get(identity);
        }

        @Override
        public Map<String, LinkedProfile> getLinkedProfiles() {
            return Collections.unmodifiableMap(linkedProfiles);
        }

        void addProfile(Profile profile) {
            linkedProfiles.put(profile.getIdentity(), (LinkedProfile) profile);
        }

        void removeProfile(String identity) {
            linkedProfiles.remove(identity);
        }
    }
}
