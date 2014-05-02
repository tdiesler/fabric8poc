/*
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
 *
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
 */

package io.fabric8.spi.internal;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.NestedProfileBuilder;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileItem;
import io.fabric8.api.ProfileOptionsProvider;
import io.fabric8.api.ProfileVersionBuilder;
import org.jboss.gravia.resource.Version;

import java.util.Map;

final class ProfileVersionNestedProfileBuilder implements NestedProfileBuilder {

    private final ProfileVersionBuilder parent;
    private final ProfileBuilder nested;

    public ProfileVersionNestedProfileBuilder(ProfileVersionBuilder parent, ProfileBuilder nested) {
        this.parent = parent;
        this.nested = nested;
    }

    @Override
    public ProfileVersionBuilder and() {
        parent.addProfile(nested.build());
        return parent;
    }

    @Override
    public ProfileVersionNestedProfileBuilder identity(String identity) {
         nested.identity(identity);
         return this;
    }

    @Override
    public ProfileVersionNestedProfileBuilder profileVersion(Version version) {
        nested.profileVersion(version);
        return this;
    }

    @Override
    public ProfileVersionNestedProfileBuilder fromOptionsProvider(ProfileOptionsProvider optionsProvider) {
        nested.fromOptionsProvider(optionsProvider);
        return this;
    }

    @Override
    public ProfileVersionNestedProfileBuilder addProfileItem(ProfileItem item) {
         nested.addProfileItem(item);
        return this;
    }

    @Override
    public ProfileVersionNestedProfileBuilder removeProfileItem(String identity) {
        nested.removeProfileItem(identity);
        return this;
    }

    @Override
    public ProfileVersionNestedProfileBuilder addConfigurationItem(String identity, Map<String, Object> config) {
        nested.addConfigurationItem(identity, config);
        return this;
    }

    @Override
    public ProfileVersionNestedProfileBuilder removeConfigurationItem(String identity) {
        nested.removeConfigurationItem(identity);
        return this;
    }

    @Override
    public ProfileVersionNestedProfileBuilder addParentProfile(String identity) {
        nested.addParentProfile(identity);
        return this;
    }

    @Override
    public ProfileVersionNestedProfileBuilder removeParentProfile(String identity) {
        nested.removeParentProfile(identity);
        return this;
    }

    @Override
    public ProfileVersionNestedProfileBuilder addAttributes(Map<AttributeKey<?>, Object> attributes) {
        nested.addAttributes(attributes);
        return this;
    }

    @Override
    public <V> ProfileVersionNestedProfileBuilder addAttribute(AttributeKey<V> key, V value) {
        nested.addAttribute(key, value);
        return this;
    }
}
