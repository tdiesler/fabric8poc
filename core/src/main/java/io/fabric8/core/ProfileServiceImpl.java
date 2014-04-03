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
package io.fabric8.core;

import static io.fabric8.api.Constants.DEFAULT_PROFILE_IDENTITY;
import static io.fabric8.api.Constants.DEFAULT_PROFILE_VERSION;
import io.fabric8.api.AttributeKey;
import io.fabric8.api.ConfigurationItem;
import io.fabric8.api.ConfigurationItemBuilder;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileBuilderFactory;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileItem;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ProfileVersionBuilderFactory;
import io.fabric8.spi.ContainerService;
import io.fabric8.spi.ImmutableProfile;
import io.fabric8.spi.ImmutableProfileVersion;
import io.fabric8.spi.NullProfileItem;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.internal.AttributeSupport;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.scr.AbstractProtectedComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.gravia.resource.Version;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { ProfileService.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public final class ProfileServiceImpl extends AbstractProtectedComponent<ProfileService> implements ProfileService {

    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();
    private final ValidatingReference<ProfileVersionBuilderFactory> versionBuilderFactory = new ValidatingReference<ProfileVersionBuilderFactory>();
    private final ValidatingReference<ProfileBuilderFactory> profileBuilderFactory = new ValidatingReference<ProfileBuilderFactory>();

    private Map<Version, ProfileVersionState> profileVersions = new LinkedHashMap<Version, ProfileVersionState>();

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
        ProfileVersion profileVersion = versionBuilder.addIdentity(DEFAULT_PROFILE_VERSION).createProfileVersion();
        ProfileVersionState versionState = addProfileVersionInternal(profileVersion);

        // Add the default profile
        ProfileBuilder profileBuilder = profileBuilderFactory.get().create();
        profileBuilder.addIdentity(DEFAULT_PROFILE_IDENTITY.getSymbolicName());
        ConfigurationItemBuilder configBuilder = profileBuilder.getItemBuilder(ConfigurationItemBuilder.class);
        configBuilder.addIdentity(ContainerService.CONTAINER_SERVICE_PID);
        configBuilder.setConfiguration(Collections.singletonMap(ContainerService.KEY_NAME_PREFIX, "default"));
        profileBuilder.addProfileItem(configBuilder.getConfigurationItem());
        versionState.addProfile(profileBuilder.createProfile());
    }

    @Override
    public Set<Version> getProfileVersionIdentities() {
        assertValid();
        synchronized (profileVersions) {
            return Collections.unmodifiableSet(new HashSet<Version>(profileVersions.keySet()));
        }
    }

    @Override
    public Set<ProfileVersion> getProfileVersions(Set<Version> identities) {
        assertValid();
        Set<ProfileVersion> result = new HashSet<ProfileVersion>();
        synchronized (profileVersions) {
            for (ProfileVersionState aux : profileVersions.values()) {
                result.add(new ImmutableProfileVersion(aux));
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public ProfileVersion getProfileVersion(Version identity) {
        assertValid();
        synchronized (profileVersions) {
            ProfileVersionState versionState = profileVersions.get(identity);
            return versionState != null ? new ImmutableProfileVersion(versionState) : null;
        }
    }

    @Override
    public ProfileVersion addProfileVersion(ProfileVersion version) {
        assertValid();
        synchronized (profileVersions) {
            return new ImmutableProfileVersion(addProfileVersionInternal(version));
        }
    }

    private ProfileVersionState addProfileVersionInternal(ProfileVersion version) {
        ProfileVersionState versionState = new ProfileVersionState(version);
        profileVersions.put(version.getIdentity(), versionState);
        return versionState;
    }

    @Override
    public ProfileVersion removeProfileVersion(Version version) {
        assertValid();
        synchronized (profileVersions) {
            ProfileVersionState versionState = profileVersions.remove(version);
            versionState.clearProfiles();
            return versionState != null ? new ImmutableProfileVersion(versionState) : null;
        }
    }

    @Override
    public Set<ProfileIdentity> getProfileIdentities(Version version) {
        assertValid();
        ProfileVersionState versionState = getRequiredProfileVersion(version);
        synchronized (versionState) {
            return versionState.getProfileIdentities();
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
        ProfileVersionState versionState = getRequiredProfileVersion(version);
        synchronized (versionState) {
            ProfileState profileState = versionState.getProfileState(profid);
            return profileState != null ? new ImmutableProfile(profileState) : null;
        }
    }

    @Override
    public Set<Profile> getProfiles(Version version, Set<ProfileIdentity> identities) {
        assertValid();
        Set<Profile> result = new HashSet<Profile>();
        ProfileVersionState versionState = getRequiredProfileVersion(version);
        synchronized (versionState) {
            for (ProfileState aux : versionState.getProfileStates()) {
                if (identities == null || identities.contains(aux.getIdentity())) {
                    result.add(new ImmutableProfile(aux));
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Profile addProfile(Version version, Profile profile) {
        assertValid();
        ProfileVersionState versionState = getRequiredProfileVersion(version);
        synchronized (versionState) {
            return new ImmutableProfile(versionState.addProfile(profile));
        }
    }

    @Override
    public Profile removeProfile(Version version, ProfileIdentity identity) {
        assertValid();
        ProfileVersionState versionState = getRequiredProfileVersion(version);
        synchronized (versionState) {
            ProfileState profileState = versionState.removeProfile(identity);
            return profileState != null ? new ImmutableProfile(profileState) : null;
        }
    }

    @Override
    public Profile updateProfile(Version version, ProfileIdentity identity, Set<? extends ProfileItem> items, boolean apply) {
        ProfileVersionState versionState = getRequiredProfileVersion(version);
        ProfileState profileState = versionState.getRequiredProfile(identity);
        synchronized (profileState) {
            profileState.updateProfileItems(items);
            if (apply) {
                Set<ConfigurationItem> configItems = profileState.getProfileItems(ConfigurationItem.class);
                ProfileSupport.applyConfigurationItems(configAdmin.get(), configItems);
            }
            return new ImmutableProfile(profileState);
        }
    }

    private ProfileVersionState getRequiredProfileVersion(Version version) {
        synchronized (profileVersions) {
            ProfileVersionState versionState = profileVersions.get(version);
            if (versionState == null)
                throw new IllegalStateException("Cannot obtain profile version: " + version);
            return versionState;
        }
    }

    @Reference
    void bindPermitManager(PermitManager stateService) {
        this.permitManager.bind(stateService);
    }

    void unbindPermitManager(PermitManager stateService) {
        this.permitManager.unbind(stateService);
    }

    @Reference
    void bindConfigurationAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }

    void unbindConfigurationAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }

    @Reference
    void bindProfileVersionBuilderFactory(ProfileVersionBuilderFactory service) {
        this.versionBuilderFactory.bind(service);
    }

    void unbindProfileVersionBuilderFactory(ProfileVersionBuilderFactory service) {
        this.versionBuilderFactory.unbind(service);
    }

    @Reference
    void bindProfileBuilderFactory(ProfileBuilderFactory service) {
        this.profileBuilderFactory.bind(service);
    }

    void unbindProfileBuilderFactory(ProfileBuilderFactory service) {
        this.profileBuilderFactory.unbind(service);
    }

    private static class ProfileVersionState implements ProfileVersion {

        private final Version identity;
        private final AttributeSupport attributes = new AttributeSupport();
        private final Map<ProfileIdentity, ProfileState> profiles = new HashMap<ProfileIdentity, ProfileState>();

        private ProfileVersionState(ProfileVersion version) {
            this.identity = version.getIdentity();
        }

        @Override
        public Version getIdentity() {
            return identity;
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
        public Set<ProfileIdentity> getProfileIdentities() {
            synchronized (profiles) {
                HashSet<ProfileIdentity> snapshot = new HashSet<ProfileIdentity>(profiles.keySet());
                return Collections.unmodifiableSet(snapshot);
            }
        }

        private Set<ProfileState> getProfileStates() {
            synchronized (profiles) {
                HashSet<ProfileState> snapshot = new HashSet<ProfileState>(profiles.values());
                return Collections.unmodifiableSet(snapshot);
            }
        }

        private ProfileState getProfileState(ProfileIdentity profid) {
            synchronized (profiles) {
                return profiles.get(profid);
            }
        }

        private ProfileState getRequiredProfile(ProfileIdentity profid) {
            ProfileState profileState = getProfileState(profid);
            if (profileState == null)
                throw new IllegalStateException("Cannot obtain profile state: " + identity + "/" + profid);
            return profileState;
        }

        private ProfileState addProfile(Profile profile) {
            synchronized (profiles) {
                ProfileState profileState = new ProfileState(this, profile, getProfileParents(profile));
                profiles.put(profile.getIdentity(), profileState);
                return profileState;
            }
        }

        private ProfileState removeProfile(ProfileIdentity identity) {
            synchronized (profiles) {
                return profiles.remove(identity);
            }
        }

        private void clearProfiles() {
            synchronized (profiles) {
                profiles.clear();
            }
        }

        private Set<ProfileState> getProfileParents(Profile profile) {
            synchronized (profiles) {
                Set<ProfileState> result = new HashSet<ProfileState>();
                for (ProfileIdentity pid : profile.getParents()) {
                    ProfileState pstate = profiles.get(pid);
                    if (pstate == null)
                        throw new IllegalStateException("Cannot obtain parent profile: " + pid);
                    result.add(pstate);
                }
                return Collections.unmodifiableSet(result);
            }
        }
    }

    private static class ProfileState implements Profile {

        private final ProfileIdentity identity;
        private final ProfileVersionState versionState;
        private final AttributeSupport attributes = new AttributeSupport();
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

        @Override
        public ProfileIdentity getIdentity() {
            return identity;
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
        public Version getProfileVersion() {
            return versionState.getIdentity();
        }

        @Override
        public Set<ProfileIdentity> getParents() {
            return parents.keySet();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends ProfileItem> Set<T> getProfileItems(Class<T> type) {
            Set<T> result = new HashSet<T>();
            synchronized (profileItems) {
                for (ProfileItem item : profileItems.values()) {
                    if (type == null || type.isAssignableFrom(item.getClass())) {
                        result.add((T) item);
                    }
                }
            }
            return Collections.unmodifiableSet(result);
        }

        private void updateProfileItems(Set<? extends ProfileItem> items) {
            synchronized (profileItems) {
                for (ProfileItem item : items) {
                    if (item instanceof NullProfileItem) {
                        profileItems.remove(item.getIdentity());
                    } else {
                        profileItems.put(item.getIdentity(), item);
                    }
                }
            }
        }
    }
}
