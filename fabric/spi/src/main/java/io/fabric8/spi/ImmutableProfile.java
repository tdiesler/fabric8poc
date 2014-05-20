/*
 * #%L
 * Fabric8 :: Core
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
import io.fabric8.api.LinkedProfile;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileItem;
import io.fabric8.spi.utils.ProfileUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.IllegalStateAssertion;

/**
 * An immutable profile
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 *
 * @Immutable
 */
public final class ImmutableProfile extends AttributeSupport implements LinkedProfile {

    private final Version version;
    private final String identity;
    private final List<String> parentIdentities = new ArrayList<>();
    private final Map<String, ProfileItem> profileItems = new LinkedHashMap<>();
    private Map<String, LinkedProfile> parentProfiles;

    public ImmutableProfile(Version version, String identity, Map<AttributeKey<?>, Object> attributes, List<String> parents, List<ProfileItem> items, Map<String, LinkedProfile> linkedProfiles) {
        super(attributes);
        this.identity = identity;
        this.version = version;
        this.parentIdentities.addAll(parents);
        for (ProfileItem item : items) {
            this.profileItems.put(item.getIdentity(), item);
        }
        if (linkedProfiles != null) {
            this.parentProfiles = new HashMap<>();
            this.parentProfiles.putAll(linkedProfiles);
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
    public List<String> getParents() {
        return Collections.unmodifiableList(parentIdentities);
    }

    @Override
    public LinkedProfile getLinkedParent(String identity) {
        IllegalStateAssertion.assertNotNull(parentProfiles, "Linked parents not available");
        return parentProfiles.get(identity);
    }

    @Override
    public Map<String, LinkedProfile> getLinkedParents() {
        IllegalStateAssertion.assertNotNull(parentProfiles, "Linked parents not available");
        return Collections.unmodifiableMap(parentProfiles);
    }

    @Override
    public Profile getEffectiveProfile() {
        IllegalStateAssertion.assertNotNull(parentProfiles, "Linked parents not available");
        return ProfileUtils.getEffectiveProfile(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ProfileItem> T getProfileItem(String identity, Class<T> type) {
        return (T) profileItems.get(identity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ProfileItem> List<T> getProfileItems(Class<T> type) {
        List<T> result = new ArrayList<T>();
        for (ProfileItem item : profileItems.values()) {
            if (type == null || type.isAssignableFrom(item.getClass())) {
                result.add((T) item);
            }
        }
        return Collections.unmodifiableList(result);
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
        return "Profile[version=" + version + ",id=" + identity + "]";
    }
}
