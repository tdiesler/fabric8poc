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
package io.fabric8.core;

import static io.fabric8.api.Constants.DEFAULT_PROFILE_IDENTITY;
import static io.fabric8.api.Constants.DEFAULT_PROFILE_VERSION;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.LinkedProfile;
import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.LockHandle;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProfileVersion;
import io.fabric8.spi.EventDispatcher;
import io.fabric8.spi.ImmutableProfile;
import io.fabric8.spi.ImmutableProfileVersion;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.scr.AbstractProtectedComponent;
import io.fabric8.spi.scr.ValidatingReference;
import io.fabric8.spi.utils.ProfileUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.IllegalStateAssertion;

/**
 * The internal {@link ProfileService}
 *
 * It is the owner of all {@link ProfileVersion} and {@link Profile} instances.
 * Internally it maintains mutable versions these instances. All mutating operations must go through this service.
 * The public API returns shallow immutable {@link ProfileVersion}, {@link Profile} instances,
 * mutable {@link ProfileVersionState} and {@link ProfileState} instances must not leak outside this service.
 *
 * This service is protected by a permit.
 * Access without the calling client holding a permit is considered a programming error.
 *
 * Concurrency & Locking Strategy
 * ------------------------------
 *
 * Read access to {@link ProfileVersion} and {@link Profile} can happen concurrently.
 * Each {@link ProfileVersion} instance is associated with a {@link ReentrantReadWriteLock}
 * Access to a {@link Profile} is protected by the lock of the associated {@link ProfileVersion}
 * The mutable {@link ProfileVersionState} and {@link ProfileState} must synchronize concurrent read operations,
 * write operations are synchronized by the lock on {@link ProfileVersion}
 *
 * A client may explicitly acquire a write lock for a {@link ProfileVersion}. This is necessary when
 * exclusive access to {@link ProfileVersion} content is required.
 * For example when provisioning a container - while doing so the {@link ProfileVersion} must be locked and cannot change.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ProfileService.class)
@References({
        @Reference(referenceInterface = EventDispatcher.class),
        @Reference(referenceInterface = PermitManager.class)})
public final class ProfileServiceImpl extends AbstractProtectedComponent<ProfileService> implements ProfileService {

    @Reference(referenceInterface = ContainerLockManager.class)
    private final ValidatingReference<ContainerLockManager> containerLocks = new ValidatingReference<>();
    @Reference(referenceInterface = ContainerRegistry.class)
    private final ValidatingReference<ContainerRegistry> containerRegistry = new ValidatingReference<>();
    @Reference(referenceInterface = ProfileRegistry.class)
    private final ValidatingReference<ProfileRegistry> profileRegistry = new ValidatingReference<>();


    @Activate
    void activate() {
        activateComponent(PERMIT, this);
    }

    @Deactivate
    void deactivate() {
        deactivateComponent(PERMIT);
    }

    @Override
    public LockHandle aquireProfileVersionLock(Version version) {
        assertValid();
        return aquireWriteLock(version);
    }

    private LockHandle aquireWriteLock(Version version) {
        return profileRegistry.get().aquireWriteLock(version);
    }

    private LockHandle aquireReadLock(Version version) {
        return profileRegistry.get().aquireReadLock(version);
    }

    @Override
    public ProfileVersion getDefaultProfileVersion() {
        assertValid();
        return profileRegistry.get().getProfileVersion(DEFAULT_PROFILE_VERSION);
    }

    @Override
    public Set<Version> getVersions() {
        assertValid();
        return profileRegistry.get().getVersions();
    }

    @Override
    public Set<ProfileVersion> getProfileVersions(Set<Version> identities) {
        assertValid();
        ProfileRegistry registry = profileRegistry.get();
        Set<ProfileVersion> result = new HashSet<ProfileVersion>();
        if (identities == null) {
            identities = registry.getVersions();
        }
        for (Version version : identities) {
            ProfileVersion prfVersion = registry.getProfileVersion(version);
            if (prfVersion != null) {
                result.add(prfVersion);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public ProfileVersion getProfileVersion(Version version) {
        assertValid();
        return profileRegistry.get().getProfileVersion(version);
    }

    @Override
    public LinkedProfileVersion getLinkedProfileVersion(Version version) {
        assertValid();
        LockHandle readLock = aquireReadLock(version);
        try {
            ProfileRegistry registry = profileRegistry.get();
            ProfileVersion profileVersion = getRequiredProfileVersion(version);
            Set<String> profileIdentities = profileVersion.getProfileIdentities();
            Map<String, Profile> linkedProfiles = new HashMap<>();
            for (String profileId : profileIdentities) {
                Profile profile = registry.getProfile(version, profileId);
                linkedProfiles.put(profileId, profile);
            }
            return new ImmutableProfileVersion(version, profileIdentities, linkedProfiles);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ProfileVersion addProfileVersion(ProfileVersion profileVersion) {
        assertValid();
        ProfileRegistry registry = profileRegistry.get();
        return registry.addProfileVersion((LinkedProfileVersion) profileVersion);
    }

    @Override
    public ProfileVersion removeProfileVersion(Version version) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            ContainerRegistry cntRegistry = containerRegistry.get();
            ContainerLockManager lockManager = containerLocks.get();
            for (ContainerIdentity cntid : cntRegistry.getContainerIdentities()) {
                LockHandle readLock = lockManager.aquireReadLock(cntid);
                try {
                    Container container = cntRegistry.getContainer(cntid);
                    Version cntVersion = container.getProfileVersion();
                    IllegalStateAssertion.assertFalse(version.equals(cntVersion), "Cannot remove profile version used by: " + container);
                } finally {
                    readLock.unlock();
                }
            }
            ProfileRegistry registry = profileRegistry.get();
            return registry.removeProfileVersion(version);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Set<String> getProfileIdentities(Version version) {
        assertValid();
        return getRequiredProfileVersion(version).getProfileIdentities();
    }

    @Override
    public Profile getDefaultProfile() {
        assertValid();
        return getProfile(DEFAULT_PROFILE_VERSION, DEFAULT_PROFILE_IDENTITY);
    }

    @Override
    public Profile getProfile(Version version, String profileId) {
        assertValid();
        ProfileRegistry registry = profileRegistry.get();
        return registry.getProfile(version, profileId);
    }

    @Override
    public Profile getEffectiveProfile(Version version, String profileId) {
        assertValid();
        return ProfileUtils.getEffectiveProfile(getLinkedProfile(version, profileId));
    }

    @Override
    public LinkedProfile getLinkedProfile(Version version, String profileId) {
        assertValid();
        return getLinkedProfileInternal(version, profileId, new HashMap<String, LinkedProfile>());
    }

    private LinkedProfile getLinkedProfileInternal(Version version, String profileId, Map<String, LinkedProfile> linkedProfiles) {
        Profile profile = getRequiredProfile(version, profileId);
        Map<String, LinkedProfile> linkedParents = getLinkedParents(profile, linkedProfiles);
        return new ImmutableProfile(profile.getVersion(), profile.getIdentity(), profile.getAttributes(), profile.getParents(), profile.getProfileItems(null), linkedParents);
    }

    private Map<String, LinkedProfile> getLinkedParents(Profile profile, Map<String, LinkedProfile> linkedProfiles) {
        Map<String, LinkedProfile> linkedParents = new HashMap<>();
        for (String parentId : profile.getParents()) {
            LinkedProfile linkedParent = linkedProfiles.get(parentId);
            if (linkedParent == null) {
                linkedParent = getLinkedProfileInternal(profile.getVersion(), parentId, linkedProfiles);
            }
            linkedProfiles.put(parentId, linkedParent);
            linkedParents.put(parentId, linkedParent);
        }
        return linkedParents;
    }

    @Override
    public Set<Profile> getProfiles(Version version, Set<String> identities) {
        assertValid();
        Set<Profile> result = new HashSet<Profile>();
        ProfileVersion profileVersion = getRequiredProfileVersion(version);
        for (String profileId : profileVersion.getProfileIdentities()) {
            if (identities == null || identities.contains(profileId)) {
                result.add(getRequiredProfile(version, profileId));
            }
        }
        IllegalStateAssertion.assertTrue(identities == null || result.size() == identities.size(), "Cannot obtain the full set of given profiles: " + identities);
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Profile addProfile(Version version, Profile profile) {
        assertValid();
        ProfileRegistry registry = profileRegistry.get();
        return registry.addProfile(version, profile);
    }

    @Override
    public Profile removeProfile(Version version, String profileId) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            ContainerRegistry cntRegistry = containerRegistry.get();
            ContainerLockManager lockManager = containerLocks.get();
            for (ContainerIdentity cntid : cntRegistry.getContainerIdentities()) {
                LockHandle readLock = lockManager.aquireReadLock(cntid);
                try {
                    Container container = cntRegistry.getContainer(cntid);
                    IllegalStateAssertion.assertFalse(container.getProfileIdentities().contains(profileId), "Cannot remove profile used by: " + container);
                } finally {
                    readLock.unlock();
                }
            }
            ProfileRegistry registry = profileRegistry.get();
            return registry.removeProfile(version, profileId);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Profile updateProfile(Profile profile, ProfileEventListener listener) {
        assertValid();
        try {
            ProfileRegistry registry = profileRegistry.get();
            Profile updated = registry.updateProfile(profile.getVersion(), profile);
            ProfileEvent event = new ProfileEvent(updated, ProfileEvent.EventType.UPDATED);
            eventDispatcher.get().dispatchProfileEvent(event, listener);
            return updated;
        } catch (RuntimeException ex) {
            ProfileEvent event = new ProfileEvent(profile, ProfileEvent.EventType.ERROR, ex);
            eventDispatcher.get().dispatchProfileEvent(event, listener);
            throw ex;
        }
    }

    @Override
    public ProfileVersion getRequiredProfileVersion(Version version) {
        assertValid();
        ProfileRegistry registry = profileRegistry.get();
        return registry.getRequiredProfileVersion(version);
    }

    @Override
    public Profile getRequiredProfile(Version version, String profileId) {
        assertValid();
        ProfileRegistry registry = profileRegistry.get();
        return registry.getRequiredProfile(version, profileId);
    }

    @Override
    public URLConnection getProfileURLConnection(URL url) throws IOException {
        assertValid();
        ProfileRegistry registry = profileRegistry.get();
        return registry.getProfileURLConnection(url);
    }

    void bindContainerLocks(ContainerLockManager service) {
        containerLocks.bind(service);
    }
    void unbindContainerLocks(ContainerLockManager service) {
        containerLocks.unbind(service);
    }

    void bindContainerRegistry(ContainerRegistry service) {
        containerRegistry.bind(service);
    }
    void unbindContainerRegistry(ContainerRegistry service) {
        containerRegistry.unbind(service);
    }

    void bindProfileRegistry(ProfileRegistry service) {
        this.profileRegistry.bind(service);
    }
    void unbindProfileRegistry(ProfileRegistry service) {
        this.profileRegistry.unbind(service);
    }
}