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
package io.fabric8.core.internal;

import io.fabric8.api.LinkedProfile;
import io.fabric8.api.LockHandle;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileItem;
import io.fabric8.core.internal.ProfileServiceImpl.ProfileState;
import io.fabric8.core.internal.ProfileServiceImpl.ProfileVersionState;
import io.fabric8.spi.AttributeSupport;
import io.fabric8.spi.utils.IllegalStateAssertion;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.gravia.resource.Version;

/**
 * An immutable profile
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 *
 * @Immutable
 */
final class ImmutableProfile extends AttributeSupport implements LinkedProfile {

    private final Version version;
    private final ProfileIdentity identity;
    private final Map<ProfileIdentity, LinkedProfile> parentProfiles;
    private final Set<ProfileIdentity> parentIdentities = new HashSet<>();
    private final Map<String, ProfileItem> profileItems = new HashMap<>();
    private final String tostring;

    ImmutableProfile(ProfileState profileState) {
        this(profileState, false);
    }

    ImmutableProfile(ProfileState profileState, boolean linked) {
        this(profileState, linked, new HashMap<ProfileIdentity, LinkedProfile>());
    }

    ImmutableProfile(ProfileState profileState, boolean linked, Map<ProfileIdentity, LinkedProfile> linkedProfiles) {
        super(profileState.getAttributes());
        ProfileVersionState versionState = profileState.getProfileVersion();
        LockHandle readLock = versionState.aquireReadLock();
        try {
            identity = profileState.getIdentity();
            version = versionState.getIdentity();
            profileItems.putAll(profileState.getProfileItems());
            parentIdentities.addAll(profileState.getParentIdentities());
            parentProfiles = linked ? new HashMap<ProfileIdentity, LinkedProfile>() : null;
            if (linked) {
                for (ProfileState parentState : profileState.getParentStates()) {
                    LinkedProfile linkedParent = linkedProfiles.get(parentState.getIdentity());
                    if (linkedParent == null) {
                        linkedParent = new ImmutableProfile(parentState, linked, linkedProfiles);
                        linkedProfiles.put(linkedParent.getIdentity(), linkedParent);
                    }
                    parentProfiles.put(linkedParent.getIdentity(), linkedParent);
                }
            }
            tostring = profileState.toString();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ProfileIdentity getIdentity() {
        return identity;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public Set<ProfileIdentity> getParents() {
        return Collections.unmodifiableSet(parentIdentities);
    }

    @Override
    public Map<ProfileIdentity, LinkedProfile> getLinkedParents() {
        IllegalStateAssertion.assertNotNull(parentProfiles, "Linked parents not available");
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
        return tostring;
    }
}
