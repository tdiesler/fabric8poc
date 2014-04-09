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
package io.fabric8.core.service;

import static io.fabric8.core.api.Constants.DEFAULT_PROFILE_IDENTITY;
import static io.fabric8.core.api.Constants.DEFAULT_PROFILE_VERSION;
import io.fabric8.core.api.AttributeKey;
import io.fabric8.core.api.ConfigurationProfileItemBuilder;
import io.fabric8.core.api.Container;
import io.fabric8.core.api.ContainerIdentity;
import io.fabric8.core.api.LockHandle;
import io.fabric8.core.api.NullProfileItem;
import io.fabric8.core.api.Profile;
import io.fabric8.core.api.ProfileBuilder;
import io.fabric8.core.api.ProfileBuilderFactory;
import io.fabric8.core.api.ProfileEvent;
import io.fabric8.core.api.ProfileEventListener;
import io.fabric8.core.api.ProfileIdentity;
import io.fabric8.core.api.ProfileItem;
import io.fabric8.core.api.ProfileVersion;
import io.fabric8.core.api.ProfileVersionBuilder;
import io.fabric8.core.api.ProfileVersionBuilderFactory;
import io.fabric8.core.spi.AttributeSupport;
import io.fabric8.core.spi.EventDispatcher;
import io.fabric8.core.spi.ProfileService;
import io.fabric8.core.spi.permit.PermitManager;
import io.fabric8.core.spi.scr.AbstractProtectedComponent;
import io.fabric8.core.spi.scr.ValidatingReference;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.jboss.gravia.resource.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
@Component(service = { ProfileService.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public final class ProfileServiceImpl extends AbstractProtectedComponent<ProfileService> implements ProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private final ValidatingReference<ProfileBuilderFactory> profileBuilderFactory = new ValidatingReference<ProfileBuilderFactory>();
    private final ValidatingReference<ProfileVersionBuilderFactory> versionBuilderFactory = new ValidatingReference<ProfileVersionBuilderFactory>();

    private Map<Version, ProfileVersionState> profileVersions = new ConcurrentHashMap<Version, ProfileVersionState>();
    private Map<Version, ReentrantReadWriteLock> profileVersionLocks = new HashMap<Version, ReentrantReadWriteLock>();

    @Activate
    void activate() {
        activateInternal();
        activateComponent(PERMIT, this);
    }

    @Deactivate
    void deactivate() {
        deactivateComponent(PERMIT);
    }

    private void activateInternal() {

        // Add the default profile version
        ProfileVersionBuilder versionBuilder = versionBuilderFactory.get().create();
        ProfileVersion profileVersion = versionBuilder.addIdentity(DEFAULT_PROFILE_VERSION).getProfileVersion();
        addProfileVersionInternal(profileVersion);

        // Build the default profile
        ProfileBuilder profileBuilder = profileBuilderFactory.get().create();
        profileBuilder.addIdentity(DEFAULT_PROFILE_IDENTITY.getSymbolicName());
        ConfigurationProfileItemBuilder configBuilder = profileBuilder.getItemBuilder(ConfigurationProfileItemBuilder.class);
        configBuilder.addIdentity(Container.CONTAINER_SERVICE_PID);
        configBuilder.setConfiguration(Collections.singletonMap(Container.CNFKEY_CONFIG_TOKEN, (Object) "default"));
        profileBuilder.addProfileItem(configBuilder.getProfileItem());
        Profile defaultProfile = profileBuilder.getProfile();

        // Add the default profile
        LockHandle writeLock = aquireWriteLock(DEFAULT_PROFILE_VERSION);
        try {
            addProfileInternal(DEFAULT_PROFILE_VERSION, defaultProfile);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public LockHandle aquireProfileVersionLock(Version version) {
        assertValid();
        return aquireWriteLock(version);
    }

    private LockHandle aquireWriteLock(Version version) {
        final WriteLock writeLock;
        synchronized (profileVersions) {
            ReentrantReadWriteLock lock = profileVersionLocks.get(version);
            if (lock == null)
                throw new IllegalStateException("Cannot obtain write lock for: " + version);

            writeLock = lock.writeLock();
        }

        boolean success;
        try {
            success = writeLock.tryLock() || writeLock.tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            success = false;
        }
        if (!success)
            throw new IllegalStateException("Cannot obtain write lock in time for: " + version);

        return new LockHandle() {
            @Override
            public void unlock() {
                writeLock.unlock();
            }
        };
    }

    private LockHandle aquireReadLock(Version version) {
        final ReadLock readLock;
        synchronized (profileVersions) {
            ReentrantReadWriteLock lock = profileVersionLocks.get(version);
            if (lock == null)
                throw new IllegalStateException("Cannot obtain read lock for: " + version);

            readLock = lock.readLock();
        }

        boolean success;
        try {
            success = readLock.tryLock() || readLock.tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            success = false;
        }
        if (!success)
            throw new IllegalStateException("Cannot obtain read lock in time for: " + version);

        return new LockHandle() {
            @Override
            public void unlock() {
                readLock.unlock();
            }
        };
    }

    @Override
    public Set<Version> getProfileVersionIdentities() {
        assertValid();
        return Collections.unmodifiableSet(profileVersions.keySet());
    }

    @Override
    public Set<ProfileVersion> getProfileVersions(Set<Version> identities) {
        assertValid();
        Set<ProfileVersion> result = new HashSet<ProfileVersion>();
        for (ProfileVersionState aux : profileVersions.values()) {
            Version identity = aux.getIdentity();
            if (identities == null || identities.contains(identity)) {
                LockHandle readLock = aquireReadLock(identity);
                try {
                    result.add(new ImmutableProfileVersion(aux));
                } finally {
                    readLock.unlock();
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public ProfileVersion getProfileVersion(Version identity) {
        assertValid();

        LockHandle readLock = null;
        ProfileVersionState versionState;
        synchronized (profileVersions) {
            versionState = profileVersions.get(identity);
            if (versionState != null) {
                readLock = aquireReadLock(identity);
            }
        }
        if (versionState == null)
            return null;

        try {
            return new ImmutableProfileVersion(versionState);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ProfileVersion addProfileVersion(ProfileVersion version) {
        assertValid();
        ProfileVersionState versionState = addProfileVersionInternal(version);
        LockHandle readLock = aquireReadLock(versionState.getIdentity());
        try {
            return new ImmutableProfileVersion(versionState);
        } finally {
            readLock.unlock();
        }
    }

    private ProfileVersionState addProfileVersionInternal(ProfileVersion version) {
        ProfileVersionState versionState = new ProfileVersionState(version);
        LOGGER.info("Add profile version: {}", versionState);
        synchronized (profileVersions) {
            Version identity = version.getIdentity();
            if (profileVersions.get(identity) != null)
                throw new IllegalStateException("ProfileVersion already exists: " + identity);

            profileVersionLocks.put(identity, new ReentrantReadWriteLock());
            profileVersions.put(identity, versionState);
        }
        return versionState;
    }

    @Override
    public ProfileVersion removeProfileVersion(Version version) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            Set<ContainerIdentity> containers = getRequiredProfileVersion(version).getContainerIdentities();
            if (!containers.isEmpty())
                throw new IllegalStateException("Cannot remove profile version used by: " + containers);

            synchronized (profileVersions) {
                ProfileVersionState versionState = profileVersions.remove(version);
                LOGGER.info("Remove profile version: {}", versionState);
                profileVersionLocks.remove(version);
                versionState.clearProfiles();
                return new ImmutableProfileVersion(versionState);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Set<ProfileIdentity> getProfileIdentities(Version version) {
        assertValid();
        LockHandle readLock = aquireReadLock(version);
        try {
            ProfileVersionState versionState = getRequiredProfileVersion(version);
            return versionState.getProfileIdentities();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Profile getDefaultProfile() {
        assertValid();
        return getProfile(DEFAULT_PROFILE_VERSION, DEFAULT_PROFILE_IDENTITY);
    }

    @Override
    public Profile getProfile(Version version, ProfileIdentity profid) {
        assertValid();
        LockHandle readLock = aquireReadLock(version);
        try {
            ProfileVersionState versionState = getRequiredProfileVersion(version);
            ProfileState profileState = versionState.getProfileState(profid);
            return profileState != null ? new ImmutableProfile(profileState) : null;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Set<Profile> getProfiles(Version version, Set<ProfileIdentity> identities) {
        assertValid();
        Set<Profile> result = new HashSet<Profile>();
        LockHandle readLock = aquireReadLock(version);
        try {
            ProfileVersionState versionState = getRequiredProfileVersion(version);
            for (ProfileState aux : versionState.getProfileStates()) {
                if (identities == null || identities.contains(aux.getIdentity())) {
                    result.add(new ImmutableProfile(aux));
                }
            }
        } finally {
            readLock.unlock();
        }
        if (identities != null && result.size() != identities.size()) {
            throw new IllegalStateException("Cannot obtain the full set of given profiles: " + identities);
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Profile addProfile(Version version, Profile profile) {
        assertValid();
        return addProfileInternal(version, profile);
    }

    private Profile addProfileInternal(Version version, Profile profile) {
        LockHandle writeLock = aquireWriteLock(version);
        try {
            ProfileVersionState versionState = getRequiredProfileVersion(version);
            ProfileIdentity identity = profile.getIdentity();
            if (versionState.getProfileState(identity) != null)
                throw new IllegalStateException("Profile already exists: " + identity);

            Set<ProfileState> profileParents = getProfileParents(versionState, profile);
            ProfileState profileState = new ProfileState(versionState, profile, profileParents);

            LOGGER.info("Add profile to version: {} <= {}", versionState, profileState);
            return new ImmutableProfile(versionState.addProfile(profileState));
        } finally {
            writeLock.unlock();
        }
    }

    private Set<ProfileState> getProfileParents(ProfileVersionState versionState, Profile profile) {
        Set<ProfileState> result = new HashSet<ProfileState>();
        for (ProfileIdentity profid : profile.getParents()) {
            ProfileState pstate = versionState.getRequiredProfile(profid);
            if (pstate == null)
                throw new IllegalStateException("Cannot obtain parent profile: " + profid);
            result.add(pstate);
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Profile removeProfile(Version version, ProfileIdentity identity) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            ProfileVersionState versionState = getRequiredProfileVersion(version);
            Set<ContainerIdentity> containers = versionState.getRequiredProfile(identity).getContainers();
            if (!containers.isEmpty())
                throw new IllegalStateException("Cannot remove profile used by: " + containers);

            LOGGER.info("Remove profile from version: {} => {}", versionState, identity);
            ProfileState profileState = versionState.removeProfile(identity);
            return profileState != null ? new ImmutableProfile(profileState) : null;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Profile updateProfile(Version version, Profile profile, ProfileEventListener listener) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            ProfileVersionState versionState = getRequiredProfileVersion(version);
            ProfileState profileState = versionState.getRequiredProfile(profile.getIdentity());
            LOGGER.info("Update profile: {}", profileState);
            try {
                Set<ProfileItem> profileItems = profile.getProfileItems(null);
                profileState.updateProfileItems(profileItems);
                Profile updated = new ImmutableProfile(profileState);
                ProfileEvent event = new ProfileEvent(updated, ProfileEvent.EventType.UPDATED);
                eventDispatcher.get().dispatchProfileEvent(event, listener);
                return updated;
            } catch (RuntimeException ex) {
                Profile result = new ImmutableProfile(profileState);
                ProfileEvent event = new ProfileEvent(result, ProfileEvent.EventType.ERROR, ex);
                eventDispatcher.get().dispatchProfileEvent(event, listener);
                throw ex;
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void addContainerToProfileVersion(Version version, ContainerIdentity containerId) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            ProfileVersionState profileVersion = getRequiredProfileVersion(version);
            profileVersion.addContainer(containerId);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void removeContainerFromProfileVersion(Version version, ContainerIdentity containerId) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            ProfileVersionState profileVersion = getRequiredProfileVersion(version);
            profileVersion.removeContainer(containerId);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void addContainerToProfile(Version version, ProfileIdentity profileId, ContainerIdentity containerId) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            ProfileVersionState profileVersion = getRequiredProfileVersion(version);
            ProfileState profileState = profileVersion.getRequiredProfile(profileId);
            profileState.addContainer(containerId);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void removeContainerFromProfile(Version version, ProfileIdentity profileId, ContainerIdentity containerId) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            ProfileVersionState profileVersion = getRequiredProfileVersion(version);
            ProfileState profileState = profileVersion.getRequiredProfile(profileId);
            profileState.removeContainer(containerId);
        } finally {
            writeLock.unlock();
        }
    }

    private ProfileVersionState getRequiredProfileVersion(Version version) {
        ProfileVersionState versionState = profileVersions.get(version);
        if (versionState == null)
            throw new IllegalStateException("Cannot obtain profile version: " + version);
        return versionState;
    }

    @Reference
    void bindEventDispatcher(EventDispatcher service) {
        eventDispatcher.bind(service);
    }

    void unbindEventDispatcher(EventDispatcher service) {
        eventDispatcher.unbind(service);
    }

    @Reference
    void bindPermitManager(PermitManager stateService) {
        this.permitManager.bind(stateService);
    }

    void unbindPermitManager(PermitManager stateService) {
        this.permitManager.unbind(stateService);
    }

    @Reference
    void bindProfileBuilderFactory(ProfileBuilderFactory service) {
        this.profileBuilderFactory.bind(service);
    }

    void unbindProfileBuilderFactory(ProfileBuilderFactory service) {
        this.profileBuilderFactory.unbind(service);
    }

    @Reference
    void bindProfileVersionBuilderFactory(ProfileVersionBuilderFactory service) {
        this.versionBuilderFactory.bind(service);
    }

    void unbindProfileVersionBuilderFactory(ProfileVersionBuilderFactory service) {
        this.versionBuilderFactory.unbind(service);
    }

    static class ProfileVersionState {

        private final Version identity;
        private final AttributeSupport attributes = new AttributeSupport();
        private final Set<ContainerIdentity> containers = new HashSet<ContainerIdentity>();
        private final Map<ProfileIdentity, ProfileState> profiles = new HashMap<ProfileIdentity, ProfileState>();

        private ProfileVersionState(ProfileVersion version) {
            this.identity = version.getIdentity();
        }

        Version getIdentity() {
            return identity;
        }

        Set<ContainerIdentity> getContainerIdentities() {
            HashSet<ContainerIdentity> snapshot = new HashSet<ContainerIdentity>(containers);
            return Collections.unmodifiableSet(snapshot);
        }

        Map<AttributeKey<?>, Object> getAttributes() {
            return attributes.getAttributes();
        }

        Set<AttributeKey<?>> getAttributeKeys() {
            return attributes.getAttributeKeys();
        }

        <T> T getAttribute(AttributeKey<T> key) {
            return attributes.getAttribute(key);
        }

        <T> boolean hasAttribute(AttributeKey<T> key) {
            return attributes.hasAttribute(key);
        }

        Set<ProfileIdentity> getProfileIdentities() {
            HashSet<ProfileIdentity> snapshot = new HashSet<ProfileIdentity>(profiles.keySet());
            return Collections.unmodifiableSet(snapshot);
        }

        Set<ProfileState> getProfileStates() {
            HashSet<ProfileState> snapshot = new HashSet<ProfileState>(profiles.values());
            return Collections.unmodifiableSet(snapshot);
        }

        ProfileState getProfileState(ProfileIdentity profid) {
            return profiles.get(profid);
        }

        ProfileState getRequiredProfile(ProfileIdentity profid) {
            ProfileState profileState = getProfileState(profid);
            if (profileState == null)
                throw new IllegalStateException("Cannot obtain profile state: " + identity + "/" + profid);
            return profileState;
        }

        // NOTE - Methods that mutate this objects should be private
        // Only the {@link ProfileService} is supposed to mutate the {@link ProfileVersionState}

        private void addContainer(ContainerIdentity identity) {
            containers.add(identity);
        }

        private void removeContainer(ContainerIdentity identity) {
            containers.remove(identity);
        }

        private ProfileState addProfile(ProfileState profileState) {
            profiles.put(profileState.getIdentity(), profileState);
            return profileState;
        }

        private ProfileState removeProfile(ProfileIdentity identity) {
            return profiles.remove(identity);
        }

        private void clearProfiles() {
            profiles.clear();
        }

        @Override
        public String toString() {
            return "ProfileVersion[" + identity + "]";
        }
    }

    static class ProfileState {

        private final ProfileIdentity identity;
        private final ProfileVersionState versionState;
        private final AttributeSupport attributes = new AttributeSupport();
        private final Set<ContainerIdentity> containers = new HashSet<ContainerIdentity>();
        private final Map<ProfileIdentity, ProfileState> parents = new HashMap<ProfileIdentity, ProfileState>();
        private final Map<String, ProfileItem> profileItems = new HashMap<String, ProfileItem>();

        private ProfileState(ProfileVersionState versionState, Profile profile, Set<ProfileState> parentStates) {
            this.versionState = versionState;
            this.identity = profile.getIdentity();
            for (ProfileState aux : parentStates) {
                parents.put(aux.getIdentity(), aux);
            }
            for (ProfileItem item : profile.getProfileItems(null)) {
                profileItems.put(item.getIdentity(), item);
            }
        }

        ProfileIdentity getIdentity() {
            return identity;
        }

        Set<ContainerIdentity> getContainers() {
            HashSet<ContainerIdentity> snapshot = new HashSet<ContainerIdentity>(containers);
            return Collections.unmodifiableSet(snapshot);
        }

        Map<AttributeKey<?>, Object> getAttributes() {
            return attributes.getAttributes();
        }

        Set<AttributeKey<?>> getAttributeKeys() {
            return attributes.getAttributeKeys();
        }

        <T> T getAttribute(AttributeKey<T> key) {
            return attributes.getAttribute(key);
        }

        <T> boolean hasAttribute(AttributeKey<T> key) {
            return attributes.hasAttribute(key);
        }

        Version getProfileVersion() {
            return versionState.getIdentity();
        }

        Set<ProfileIdentity> getParents() {
            return parents.keySet();
        }

        @SuppressWarnings("unchecked")
        <T extends ProfileItem> Set<T> getProfileItems(Class<T> type) {
            Set<T> result = new HashSet<T>();
            for (ProfileItem item : profileItems.values()) {
                if (type == null || type.isAssignableFrom(item.getClass())) {
                    result.add((T) item);
                }
            }
            return Collections.unmodifiableSet(result);
        }

        // NOTE - Methods that mutate this objects should be private
        // Only the {@link ProfileService} is supposed to mutate the {@link ProfileVersionState}

        private void addContainer(ContainerIdentity identity) {
            containers.add(identity);
        }

        private void removeContainer(ContainerIdentity identity) {
            containers.remove(identity);
        }

        private void updateProfileItems(Set<? extends ProfileItem> items) {
            for (ProfileItem item : items) {
                if (item instanceof NullProfileItem) {
                    profileItems.remove(item.getIdentity());
                } else {
                    profileItems.put(item.getIdentity(), item);
                }
            }
        }

        @Override
        public String toString() {
            return "Profile[version=" + versionState.getIdentity() + ",id=" + identity + "]";
        }
    }
}
