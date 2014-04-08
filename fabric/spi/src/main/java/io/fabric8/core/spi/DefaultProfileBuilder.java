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
package io.fabric8.core.spi;

import io.fabric8.core.api.ConfigurationProfileItemBuilder;
import io.fabric8.core.api.ContainerIdentity;
import io.fabric8.core.api.NullProfileItemBuilder;
import io.fabric8.core.api.Profile;
import io.fabric8.core.api.ProfileBuilder;
import io.fabric8.core.api.ProfileIdentity;
import io.fabric8.core.api.ProfileItem;
import io.fabric8.core.api.ProfileItemBuilder;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.gravia.resource.Version;

public final class DefaultProfileBuilder implements ProfileBuilder {

    private final Set<ProfileItem> items = new HashSet<ProfileItem>();
    private ProfileIdentity identity;

    @Override
    public ProfileBuilder addIdentity(String symbolicName) {
        identity = ProfileIdentity.create(symbolicName);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ProfileItemBuilder<?>> T getItemBuilder(Class<T> type) {
        if (ConfigurationProfileItemBuilder.class.isAssignableFrom(type)) {
            return (T) new DefaultConfigurationProfileItemBuilder();
        } else if (NullProfileItemBuilder.class.isAssignableFrom(type)) {
            return (T) new DefaultNullProfileItemBuilder();
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    @Override
    public ProfileBuilder addProfileItem(ProfileItem item) {
        items.add(item);
        return this;
    }

    @Override
    public ProfileBuilder importProfile(InputStream input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Profile getProfile() {
        return new ProfileImpl();
    }

    class ProfileImpl extends AttributeSupport implements Profile {

        @Override
        public ProfileIdentity getIdentity() {
            return identity;
        }

        @Override
        public Version getProfileVersion() {
            return null;
        }

        @Override
        public Set<ProfileIdentity> getParents() {
            return Collections.emptySet();
        }

        @Override
        public Set<ContainerIdentity> getContainerIds() {
            return Collections.emptySet();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ProfileItem> Set<T> getProfileItems(Class<T> type) {
            return (Set<T>) items;
        }
    }
}
