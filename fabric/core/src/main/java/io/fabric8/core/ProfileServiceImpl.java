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
import io.fabric8.api.LinkedProfile;
import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.LockHandle;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProfileVersion;
import io.fabric8.core.ContainerServiceImpl.ContainerState;
import io.fabric8.spi.DefaultProfileBuilder;
import io.fabric8.spi.DefaultProfileVersionBuilder;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;
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
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ProfileService.class)
@References({
        @Reference(referenceInterface = EventDispatcher.class),
        @Reference(referenceInterface = PermitManager.class)})
public final class ProfileServiceImpl extends AbstractProtectedComponent<ProfileService> implements ProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileServiceImpl.class);

    @Reference(referenceInterface = ContainerRegistry.class)
    private final ValidatingReference<ContainerRegistry> containerRegistry = new ValidatingReference<>();
    @Reference(referenceInterface = ProfileRegistry.class)
    private final ValidatingReference<ProfileRegistry> profileRegistry = new ValidatingReference<>();

    private final Map<Version, ReentrantReadWriteLock> versionLocks = new ConcurrentHashMap<>();

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
        Profile profile = new DefaultProfileBuilder(DEFAULT_PROFILE_IDENTITY)
                .addConfigurationItem(Container.CONTAINER_SERVICE_PID, Collections.singletonMap("config.token", (Object) "default"))
                .getProfile();

        LinkedProfileVersion profileVersion = new DefaultProfileVersionBuilder(DEFAULT_PROFILE_VERSION)
                .addProfile(profile)
                .getProfileVersion();

        addProfileVersionInternal(profileVersion);
    }

    @Override
    public LockHandle aquireProfileVersionLock(Version version) {
        assertValid();
        return aquireWriteLock(version);
    }

    private LockHandle aquireWriteLock(Version version) {

        final WriteLock writeLock = getReadWriteLock(version).writeLock();

        boolean success;
        try {
            success = writeLock.tryLock() || writeLock.tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            success = false;
        }
        IllegalStateAssertion.assertTrue(success, "Cannot obtain write lock in time for: " + version);

        return new LockHandle() {
            @Override
            public void unlock() {
                writeLock.unlock();
            }
        };
    }

    private LockHandle aquireReadLock(Version version) {

        final ReadLock readLock = getReadWriteLock(version).readLock();

        boolean success;
        try {
            success = readLock.tryLock() || readLock.tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            success = false;
        }
        IllegalStateAssertion.assertTrue(success, "Cannot obtain read lock in time for: " + version);

        return new LockHandle() {
            @Override
            public void unlock() {
                readLock.unlock();
            }
        };
    }

    private ReentrantReadWriteLock getReadWriteLock(Version version) {
        IllegalArgumentAssertion.assertNotNull(version, "version");
        ReentrantReadWriteLock readWriteLock;
        synchronized (versionLocks) {
            readWriteLock = versionLocks.get(version);
            if (readWriteLock == null) {
                readWriteLock = new ReentrantReadWriteLock();
                versionLocks.put(version, readWriteLock);
            }
        }
        return readWriteLock;
    }

    @Override
    public ProfileVersion getDefaultProfileVersion() {
        assertValid();
        LockHandle readLock = aquireReadLock(DEFAULT_PROFILE_VERSION);
        try {
            return profileRegistry.get().getProfileVersion(DEFAULT_PROFILE_VERSION);
        } finally {
            readLock.unlock();
        }
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
            LockHandle readLock = aquireReadLock(version);
            try {
                ProfileVersion prfVersion = registry.getProfileVersion(version);
                if (prfVersion != null) {
                    result.add(prfVersion);
                }
            } finally {
                readLock.unlock();
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public ProfileVersion getProfileVersion(Version version) {
        assertValid();
        LockHandle readLock = aquireReadLock(version);
        try {
            return profileRegistry.get().getProfileVersion(version);
        } finally {
            readLock.unlock();
        }
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
            for (String identity : profileIdentities) {
                Profile profile = registry.getProfile(version, identity);
                linkedProfiles.put(identity, profile);
            }
            return new ImmutableProfileVersion(version, profileIdentities, linkedProfiles);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public ProfileVersion addProfileVersion(ProfileVersion profileVersion) {
        assertValid();
        return addProfileVersionInternal((LinkedProfileVersion) profileVersion);
    }

    private ProfileVersion addProfileVersionInternal(LinkedProfileVersion profileVersion) {
        Version version = profileVersion.getIdentity();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            ProfileRegistry registry = profileRegistry.get();
            IllegalStateAssertion.assertNull(registry.getProfileVersion(version), "ProfileVersion already exists: " + profileVersion);
            IllegalStateAssertion.assertFalse(profileVersion.getProfileIdentities().isEmpty(), "ProfileVersion must contain at least one profile: " + profileVersion);
            LOGGER.info("Add profile version: {}", version);
            return registry.addProfileVersion(profileVersion);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public ProfileVersion removeProfileVersion(Version version) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            ProfileVersion profileVersion = getRequiredProfileVersion(version);
            for (ContainerState cntState : containerRegistry.get().getContainers(null)) {
                ProfileVersion cntVersion = cntState.getProfileVersion();
                IllegalStateAssertion.assertFalse(profileVersion.equals(cntVersion), "Cannot remove profile version used by: " + cntState);
            }
            LOGGER.info("Remove profile version: {}", version);
            versionLocks.remove(version);
            return profileRegistry.get().removeProfileVersion(version);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Set<String> getProfileIdentities(Version version) {
        assertValid();
        LockHandle readLock = aquireReadLock(version);
        try {
            return getRequiredProfileVersion(version).getProfileIdentities();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Profile getDefaultProfile() {
        assertValid();
        LockHandle readLock = aquireReadLock(DEFAULT_PROFILE_VERSION);
        try {
            return getProfile(DEFAULT_PROFILE_VERSION, DEFAULT_PROFILE_IDENTITY);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Profile getProfile(Version version, String identity) {
        assertValid();
        LockHandle readLock = aquireReadLock(version);
        try {
            return profileRegistry.get().getProfile(version, identity);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Profile getEffectiveProfile(Version version, String identity) {
        assertValid();
        return ProfileUtils.getEffectiveProfile(getLinkedProfile(version, identity));
    }

    @Override
    public LinkedProfile getLinkedProfile(Version version, String identity) {
        assertValid();
        LockHandle readLock = aquireReadLock(version);
        try {
            return getLinkedProfileInternal(version, identity, new HashMap<String, LinkedProfile>());
        } finally {
            readLock.unlock();
        }
    }

    private LinkedProfile getLinkedProfileInternal(Version version, String identity, Map<String, LinkedProfile> linkedProfiles) {
        Profile profile = getRequiredProfile(version, identity);
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
        LockHandle readLock = aquireReadLock(version);
        try {
            Set<Profile> result = new HashSet<Profile>();
            ProfileVersion profileVersion = getRequiredProfileVersion(version);
            for (String identity : profileVersion.getProfileIdentities()) {
                if (identities == null || identities.contains(identity)) {
                    result.add(getRequiredProfile(version, identity));
                }
            }
            IllegalStateAssertion.assertTrue(identities == null || result.size() == identities.size(), "Cannot obtain the full set of given profiles: " + identities);
            return Collections.unmodifiableSet(result);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Profile addProfile(Version version, Profile profile) {
        assertValid();
        return addProfileInternal(version, profile);
    }

    private Profile addProfileInternal(Version version, Profile profile) {
        LockHandle writeLock = aquireWriteLock(version);
        try {
            getRequiredProfileVersion(version);
            Version pversion = profile.getVersion();
            IllegalStateAssertion.assertTrue(pversion == null || version.equals(pversion), "Unexpected profile version: " + profile);
            IllegalStateAssertion.assertNull(getProfile(version, profile.getIdentity()), "Profile already exists: " + profile);
            LOGGER.info("Add profile to version: {} <= {}", version, profile);
            return profileRegistry.get().addProfile(version, profile);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Profile removeProfile(Version version, String identity) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            getRequiredProfileVersion(version);
            ContainerRegistry registry = containerRegistry.get();
            for (ContainerState cntState : registry.getContainers(null)) {
                LockHandle readLock = registry.aquireReadLock(cntState.getIdentity());
                try {
                    IllegalStateAssertion.assertFalse(cntState.getProfileIdentities().contains(identity), "Cannot remove profile used by: " + cntState);
                } finally {
                    readLock.unlock();
                }
            }
            LOGGER.info("Remove profile from version: {} => {}", version, identity);
            return profileRegistry.get().removeProfile(version, identity);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Profile updateProfile(Profile profile, ProfileEventListener listener) {
        assertValid();
        Version version = profile.getVersion();
        String identity = profile.getIdentity();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            getRequiredProfile(version, identity);
            LOGGER.info("Update profile: {}", profile);
            try {
                ProfileRegistry registry = profileRegistry.get();
                registry.removeProfile(version, identity);
                Profile updated = registry.addProfile(version, profile);
                ProfileEvent event = new ProfileEvent(updated, ProfileEvent.EventType.UPDATED);
                eventDispatcher.get().dispatchProfileEvent(event, listener);
                return updated;
            } catch (RuntimeException ex) {
                ProfileEvent event = new ProfileEvent(profile, ProfileEvent.EventType.ERROR, ex);
                eventDispatcher.get().dispatchProfileEvent(event, listener);
                throw ex;
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public ProfileVersion getRequiredProfileVersion(Version version) {
        LockHandle readLock = aquireReadLock(version);
        try {
            ProfileVersion profileVersion = profileRegistry.get().getProfileVersion(version);
            IllegalStateAssertion.assertNotNull(profileVersion, "Cannot obtain profile version: " + version);
            return profileVersion;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Profile getRequiredProfile(Version version, String identity) {
        LockHandle readLock = aquireReadLock(version);
        try {
            Profile profile = profileRegistry.get().getProfile(version, identity);
            IllegalStateAssertion.assertNotNull(profile, "Cannot obtain profile: " + version + ":" + identity);
            return profile;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public URLConnection getProfileURLConnection(URL url) throws IOException {
        Version version = new Version(url.getHost());
        LockHandle readLock = aquireReadLock(version);
        try {
            return profileRegistry.get().getProfileURLConnection(url);
        } finally {
            readLock.unlock();
        }
    }

    ProfileVersionState getProfileVersionState(Version version) {
        return new ProfileVersionState(getRequiredProfileVersion(version));
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

    final class ProfileVersionState {

        private final ProfileVersion profileVersion;

        ProfileVersionState(ProfileVersion profileVersion) {
            IllegalArgumentAssertion.assertNotNull(profileVersion, "profileVersion");
            this.profileVersion = profileVersion;
        }

        ProfileVersion getProfileVersion() {
            return profileVersion;
        }

        Version getIdentity() {
            return profileVersion.getIdentity();
        }

        LockHandle aquireWriteLock() {
            return aquireProfileVersionLock(getIdentity());
        }

    }
}
