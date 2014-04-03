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

import io.fabric8.api.ConfigurationItemBuilder;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileItem;
import io.fabric8.api.ProfileItemBuilder;
import io.fabric8.spi.ImmutableProfile;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

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
        if (ConfigurationItemBuilder.class.isAssignableFrom(type)) {
            return (T) new DefaultConfigurationItemBuilder();
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
    public Profile createProfile() {
        return new ImmutableProfile(identity, items);
    }
}
