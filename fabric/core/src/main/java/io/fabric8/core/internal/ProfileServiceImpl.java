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

import static io.fabric8.api.Constants.DEFAULT_PROFILE_IDENTITY;
import static io.fabric8.api.Constants.DEFAULT_PROFILE_VERSION;
import io.fabric8.api.ConfigurationProfileItemBuilder;
import io.fabric8.api.Container;
import io.fabric8.api.LinkedProfile;
import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.LockHandle;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileBuilderFactory;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileItem;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ProfileVersionBuilderFactory;
import io.fabric8.core.internal.ContainerServiceImpl.ContainerState;
import io.fabric8.spi.AttributeSupport;
import io.fabric8.spi.EventDispatcher;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.scr.AbstractProtectedComponent;
import io.fabric8.spi.scr.ValidatingReference;
import io.fabric8.spi.utils.IllegalStateAssertion;

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
import org.jboss.gravia.utils.NotNullException;
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
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
@Component(service = { ProfileService.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public final class ProfileServiceImpl extends AbstractProtectedComponent<ProfileService> implements ProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private final ValidatingReference<ProfileBuilderFactory> profileBuilderFactory = new ValidatingReference<ProfileBuilderFactory>();
    private final ValidatingReference<ProfileVersionBuilderFactory> versionBuilderFactory = new ValidatingReference<ProfileVersionBuilderFactory>();
    private final ValidatingReference<ContainerRegistry> containerRegistry = new ValidatingReference<ContainerRegistry>();

    private Map<Version, ProfileVersionState> profileVersions = new ConcurrentHashMap<Version, ProfileVersionState>();

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
        profileBuilder.addIdentity(DEFAULT_PROFILE_IDENTITY);
        ConfigurationProfileItemBuilder configBuilder = profileBuilder.getProfileItemBuilder(Container.CONTAINER_SERVICE_PID, ConfigurationProfileItemBuilder.class);
        configBuilder.setConfiguration(Collections.singletonMap(Container.CNFKEY_CONFIG_TOKEN, (Object) "default"));
        profileBuilder.addProfileItem(configBuilder.getProfileItem());
        Profile defaultProfile = profileBuilder.getProfile();

        // Add the default profile
        addProfileInternal(DEFAULT_PROFILE_VERSION, defaultProfile);
    }

    @Override
    public LockHandle aquireProfileVersionLock(Version version) {
        assertValid();
        ProfileVersionState versionState = getRequiredProfileVersion(version);
        return versionState.aquireWriteLock();
    }

    @Override
    public ProfileVersion getDefaultProfileVersion() {
        assertValid();
        ProfileVersionState versionState = getRequiredProfileVersion(DEFAULT_PROFILE_VERSION);
        return new ImmutableProfileVersion(versionState);
    }

    @Override
    public Set<Version> getVersions() {
        assertValid();
        return Collections.unmodifiableSet(profileVersions.keySet());
    }

    @Override
    public Set<ProfileVersion> getProfileVersions(Set<Version> identities) {
        assertValid();
        Set<ProfileVersion> result = new HashSet<ProfileVersion>();
        for (ProfileVersionState versionState : profileVersions.values()) {
            Version identity = versionState.getIdentity();
            if (identities == null || identities.contains(identity)) {
                result.add(new ImmutableProfileVersion(versionState));
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public ProfileVersion getProfileVersion(Version identity) {
        assertValid();
        ProfileVersionState versionState = profileVersions.get(identity);
        return versionState != null ? new ImmutableProfileVersion(versionState) : null;
    }

    @Override
    public ProfileVersion addProfileVersion(ProfileVersion profileVersion) {
        assertValid();
        ProfileVersionState versionState = addProfileVersionInternal(profileVersion);
        return new ImmutableProfileVersion(versionState);
    }

    private ProfileVersionState addProfileVersionInternal(ProfileVersion profileVersion) {
        ProfileVersionState versionState = new ProfileVersionState(profileVersion);
        LOGGER.info("Add profile version: {}", versionState);
        Version identity = profileVersion.getIdentity();
        IllegalStateAssertion.assertTrue(profileVersions.get(identity) == null, "ProfileVersion already exists: " + identity);
        profileVersions.put(identity, versionState);
        return versionState;
    }

    @Override
    public ProfileVersion removeProfileVersion(Version version) {
        assertValid();
        ProfileVersionState versionState = getRequiredProfileVersion(version);
        LockHandle writeLock = versionState.aquireWriteLock();
        try {
            for (ContainerState cntState : containerRegistry.get().getContainers(null)) {
                ProfileVersionState cntVersionState = cntState.getProfileVersion();
                Version cntVersion = cntVersionState != null ? cntVersionState.getIdentity() : null;
                IllegalStateAssertion.assertFalse(version.equals(cntVersion), "Cannot remove profile version used by: " + cntState);
            }
            LOGGER.info("Remove profile version: {}", versionState);
            profileVersions.remove(version);
            return new ImmutableProfileVersion(versionState);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Set<ProfileIdentity> getProfileIdentities(Version version) {
        assertValid();
        ProfileVersionState versionState = getRequiredProfileVersion(version);
        return versionState.getProfileIdentities();
    }

    @Override
    public Profile getDefaultProfile() {
        assertValid();
        return getProfile(DEFAULT_PROFILE_VERSION, DEFAULT_PROFILE_IDENTITY);
    }

    @Override
    public Profile getProfile(Version version, ProfileIdentity profid) {
        assertValid();
        ProfileVersionState versionState = getRequiredProfileVersion(version);
        ProfileState profileState = versionState.getProfileState(profid);
        return profileState != null ? new ImmutableProfile(profileState) : null;
    }

    @Override
    public Set<Profile> getProfiles(Version version, Set<ProfileIdentity> identities) {
        assertValid();
        Set<Profile> result = new HashSet<Profile>();
        ProfileVersionState versionState = getRequiredProfileVersion(version);
        for (ProfileState aux : versionState.getProfileStates()) {
            if (identities == null || identities.contains(aux.getIdentity())) {
                result.add(new ImmutableProfile(aux));
            }
        }
        IllegalStateAssertion.assertTrue(identities == null || result.size() == identities.size(), "Cannot obtain the full set of given profiles: " + identities);
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Profile addProfile(Version version, Profile profile) {
        assertValid();
        return addProfileInternal(version, profile);
    }

    private Profile addProfileInternal(Version version, Profile profile) {
        ProfileVersionState versionState = getRequiredProfileVersion(version);
        LockHandle writeLock = versionState.aquireWriteLock();
        try {
            ProfileIdentity identity = profile.getIdentity();
            IllegalStateAssertion.assertTrue(versionState.getProfileState(identity) == null, "Profile already exists: " + identity);

            LOGGER.info("Add profile to version: {} <= {}", version, profile);

            ProfileState profileState = new ProfileState(versionState, profile);
            return new ImmutableProfile(profileState);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Profile removeProfile(Version version, ProfileIdentity identity) {
        assertValid();
        ProfileVersionState versionState = getRequiredProfileVersion(version);
        LockHandle writeLock = versionState.aquireWriteLock();
        try {
            for (ContainerState cntState : containerRegistry.get().getContainers(null)) {
                IllegalStateAssertion.assertFalse(cntState.getProfiles().contains(identity), "Cannot remove profile used by: " + cntState);
            }
            LOGGER.info("Remove profile from version: {} => {}", versionState, identity);
            ProfileState profileState = versionState.removeProfile(identity);
            return profileState != null ? new ImmutableProfile(profileState) : null;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Profile updateProfile(Profile profile, ProfileEventListener listener) {
        assertValid();
        ProfileVersionState versionState = getRequiredProfileVersion(profile.getVersion());
        LockHandle writeLock = versionState.aquireWriteLock();
        try {
            ProfileState profileState = versionState.getRequiredProfile(profile.getIdentity());
            LOGGER.info("Update profile: {}", profileState);
            try {
                Profile updated = new ImmutableProfile(profileState.update(profile));
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
    public LinkedProfileVersion copyProfileVersion(ProfileVersion version) {
        ProfileVersionState versionState = getRequiredProfileVersion(version.getIdentity());
        return new ImmutableProfileVersion(versionState, true);
    }

    @Override
    public LinkedProfile copyProfile(Profile profile) {
        ProfileVersionState versionState = getRequiredProfileVersion(profile.getVersion());
        ProfileState profileState = versionState.getRequiredProfile(profile.getIdentity());
        return new ImmutableProfile(profileState, true);
    }

    ProfileVersionState getRequiredProfileVersion(Version version) {
        NotNullException.assertValue(version, "version");
        ProfileVersionState versionState = profileVersions.get(version);
        IllegalStateAssertion.assertNotNull(versionState, "Cannot obtain profile version: " + version);
        return versionState;
    }

    @Reference
    void bindContainerRegistry(ContainerRegistry service) {
        containerRegistry.bind(service);
    }

    void unbindContainerRegistry(ContainerRegistry service) {
        containerRegistry.unbind(service);
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

    static class ProfileVersionState extends AttributeSupport {

        private final Version identity;
        private final Map<ProfileIdentity, ProfileState> profiles = new HashMap<ProfileIdentity, ProfileState>();
        private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

        private ProfileVersionState(ProfileVersion profileVersion) {
            super(profileVersion.getAttributes());
            identity = profileVersion.getIdentity();
            if (profileVersion instanceof LinkedProfileVersion) {
                LinkedProfileVersion linkedVersion = (LinkedProfileVersion) profileVersion;
                for (LinkedProfile linkedProfile : linkedVersion.getLinkedProfiles().values()) {
                    new ProfileState(this, linkedProfile);
                }
            }
        }

        LockHandle aquireWriteLock() {
            final WriteLock writeLock = readWriteLock.writeLock();

            boolean success;
            try {
                success = writeLock.tryLock() || writeLock.tryLock(10, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                success = false;
            }
            IllegalStateAssertion.assertTrue(success, "Cannot obtain write lock in time for: " + identity);

            return new LockHandle() {
                @Override
                public void unlock() {
                    writeLock.unlock();
                }
            };
        }

        LockHandle aquireReadLock() {
            final ReadLock readLock = readWriteLock.readLock();

            boolean success;
            try {
                success = readLock.tryLock() || readLock.tryLock(10, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                success = false;
            }
            IllegalStateAssertion.assertTrue(success, "Cannot obtain read lock in time for: " + identity);

            return new LockHandle() {
                @Override
                public void unlock() {
                    readLock.unlock();
                }
            };
        }

        Version getIdentity() {
            return identity;
        }

        Set<ProfileIdentity> getProfileIdentities() {
            LockHandle readLock = aquireReadLock();
            try {
                HashSet<ProfileIdentity> snapshot = new HashSet<ProfileIdentity>(profiles.keySet());
                return Collections.unmodifiableSet(snapshot);
            } finally {
                readLock.unlock();
            }
        }

        Set<ProfileState> getProfileStates() {
            LockHandle readLock = aquireReadLock();
            try {
                HashSet<ProfileState> snapshot = new HashSet<ProfileState>(profiles.values());
                return Collections.unmodifiableSet(snapshot);
            } finally {
                readLock.unlock();
            }
        }

        ProfileState getProfileState(ProfileIdentity profileId) {
            LockHandle readLock = aquireReadLock();
            try {
                return profiles.get(profileId);
            } finally {
                readLock.unlock();
            }
        }

        ProfileState getRequiredProfile(ProfileIdentity profileId) {
            ProfileState profileState = getProfileState(profileId);
            IllegalStateAssertion.assertNotNull(profileState, "Cannot obtain profile state: " + identity + "/" + profileId);
            return profileState;
        }

        // NOTE - Methods that mutate this objects should be private
        // Only the {@link ProfileService} is supposed to mutate the {@link ProfileVersionState}

        private ProfileState addProfile(ProfileState profileState) {
            LockHandle writeLock = aquireWriteLock();
            try {
                profiles.put(profileState.getIdentity(), profileState);
                return profileState;
            } finally {
                writeLock.unlock();
            }
        }

        private ProfileState removeProfile(ProfileIdentity identity) {
            LockHandle writeLock = aquireWriteLock();
            try {
                return profiles.remove(identity);
            } finally {
                writeLock.unlock();
            }
        }

        @Override
        public String toString() {
            return "ProfileVersion[" + identity + "]";
        }
    }

    static class ProfileState extends AttributeSupport {

        private final ProfileIdentity identity;
        private final ProfileVersionState versionState;
        private final Map<ProfileIdentity, ProfileState> parents = new HashMap<ProfileIdentity, ProfileState>();
        private final Map<String, ProfileItem> profileItems = new HashMap<String, ProfileItem>();

        private ProfileState(ProfileVersionState versionState, Profile profile) {
            super(profile.getAttributes());
            this.versionState = versionState;
            this.identity = profile.getIdentity();
            IllegalStateAssertion.assertTrue(profile.getParents().isEmpty(), "Profile parents not yet supported");
            for (ProfileItem item : profile.getProfileItems(null)) {
                profileItems.put(item.getIdentity(), item);
            }
            versionState.addProfile(this);
        }

        ProfileIdentity getIdentity() {
            return identity;
        }

        ProfileVersionState getProfileVersion() {
            return versionState;
        }

        Set<ProfileIdentity> getParentIdentities() {
            LockHandle readLock = versionState.aquireReadLock();
            try {
                return Collections.unmodifiableSet(new HashSet<>(parents.keySet()));
            } finally {
                readLock.unlock();
            }
        }

        Set<ProfileState> getParentStates() {
            LockHandle readLock = versionState.aquireReadLock();
            try {
                return Collections.unmodifiableSet(new HashSet<>(parents.values()));
            } finally {
                readLock.unlock();
            }
        }

        Map<String, ProfileItem> getProfileItems() {
            LockHandle readLock = versionState.aquireReadLock();
            try {
                return Collections.unmodifiableMap(new HashMap<>(profileItems));
            } finally {
                readLock.unlock();
            }
        }

        // NOTE - Methods that mutate this objects should be private
        // Only the {@link ProfileService} is supposed to mutate the {@link ProfileVersionState}

        private ProfileState update(Profile profile) {
            LockHandle writeLock = versionState.aquireWriteLock();
            try {
                IllegalStateAssertion.assertTrue(profile.getParents().isEmpty(), "Profile parents not yet supported");
                profileItems.clear();
                for (ProfileItem item : profile.getProfileItems(null)) {
                    profileItems.put(item.getIdentity(), item);
                }
            } finally {
                writeLock.unlock();
            }
            return this;
        }

        @Override
        public String toString() {
            return "Profile[version=" + versionState.getIdentity() + ",id=" + identity + "]";
        }
    }
}
