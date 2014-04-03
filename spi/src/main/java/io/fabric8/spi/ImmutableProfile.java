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
package io.fabric8.spi;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileItem;
import io.fabric8.spi.internal.AttributeSupport;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.NotNullException;

public final class ImmutableProfile implements Profile {

    private final ProfileIdentity identity;

    private final Set<ProfileIdentity> parents = new HashSet<ProfileIdentity>();
    private final Set<ProfileItem> profileItems = new HashSet<ProfileItem>();
    private final AttributeSupport attributes = new AttributeSupport();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ImmutableProfile(Profile profile) {
        NotNullException.assertValue(profile, "profile");
        identity = profile.getIdentity();
        profileItems.addAll(profile.getProfileItems(null));
        for (AttributeKey key : profile.getAttributeKeys()) {
            attributes.putAttribute(key, profile.getAttribute(key));
        }
    }

    public ImmutableProfile(ProfileIdentity identity, Set<ProfileItem> items) {
        NotNullException.assertValue(identity, "identity");
        NotNullException.assertValue(items, "items");
        this.identity = identity;
        this.profileItems.addAll(items);
    }

    @Override
    public ProfileIdentity getIdentity() {
        return identity;
    }

    @Override
    public Version getProfileVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<ProfileIdentity> getParents() {
        return Collections.unmodifiableSet(parents);
    }

    @Override
    public Set<AttributeKey<?>> getAttributeKeys() {
        return attributes.getAttributeKeys();
    }

    @Override
    public <T> T getAttribute(AttributeKey<T> key) {
        return attributes.getAttribute(key);
    }

    @Override
    public <T> boolean hasAttribute(AttributeKey<T> key) {
        return attributes.hasAttribute(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ProfileItem> Set<T> getProfileItems(Class<T> type) {
        Set<T> result = new HashSet<T>();
        for (ProfileItem item : profileItems) {
            if (type == null || type.isAssignableFrom(item.getClass())) {
                result.add((T) item);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ImmutableProfile)) return false;
        ImmutableProfile other = (ImmutableProfile) obj;
        return other.identity.equals(identity);
    }

    @Override
    public int hashCode() {
        return identity.hashCode();
    }

    @Override
    public String toString() {
        return identity.toString();
    }
}
