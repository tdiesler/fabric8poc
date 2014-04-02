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

import io.fabric8.api.Profile;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileVersion;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.ProfileState;
import io.fabric8.spi.ProfileVersionState;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { ProfileService.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public final class ProfileServiceImpl extends AbstractProtectedComponent<ProfileService> implements ProfileService {

    private final ValidatingReference<PermitManager> permitManager = new ValidatingReference<PermitManager>();

    private Map<Version, ProfileVersionState> profileVersions = new LinkedHashMap<Version, ProfileVersionState>();

    @Activate
    void activate(Map<String, ?> config) {
        activateComponent(PERMIT, this);
    }

    @Deactivate
    void deactivate() {
        deactivateComponent(PERMIT);
    }

    @Override
    protected PermitManager getPermitManager() {
        return permitManager.get();
    }

    @Override
    public Map<Version, ProfileVersionState> getProfileVersions() {
        synchronized (profileVersions) {
            HashMap<Version, ProfileVersionState> snapshot = new HashMap<Version, ProfileVersionState>(profileVersions);
            return Collections.unmodifiableMap(snapshot);
        }
    }

    @Override
    public ProfileVersionState addProfileVersion(ProfileVersion version) {
        synchronized (profileVersions) {
            ProfileVersionState versionState = new ProfileVersionStateImpl(version);
            profileVersions.put(version.getIdentity(), versionState);
            return versionState;
        }
    }

    @Override
    public ProfileVersionState removeProfileVersion(Version version) {
        synchronized (profileVersions) {
            return profileVersions.remove(version);
        }
    }

    @Override
    public Map<ProfileIdentity, ProfileState> getProfiles(Version version) {
        synchronized (profileVersions) {
            ProfileVersionState versionState = profileVersions.get(version);
            return versionState != null ? versionState.getProfiles() : Collections.<ProfileIdentity, ProfileState> emptyMap();
        }
    }

    @Override
    public ProfileState addProfile(Version version, Profile profile) {
        synchronized (profileVersions) {
            ProfileVersionStateImpl versionState = (ProfileVersionStateImpl) profileVersions.get(version);
            if (versionState == null) {
                throw new IllegalStateException("Profile version does not exist: " + version);
            } else {
                return versionState.addProfile(profile);
            }
        }
    }

    @Override
    public ProfileState removeProfile(Version version, ProfileIdentity identity) {
        synchronized (profileVersions) {
            ProfileVersionStateImpl versionState = (ProfileVersionStateImpl) profileVersions.get(version);
            if (versionState == null) {
                throw new IllegalStateException("Profile version does not exist: " + version);
            } else {
                return versionState.removeProfile(identity);
            }
        }
    }

    @Reference
    void bindPermitManager(PermitManager stateService) {
        this.permitManager.bind(stateService);
    }

    void unbindPermitManager(PermitManager stateService) {
        this.permitManager.unbind(stateService);
    }

    static class ProfileVersionStateImpl implements ProfileVersionState {

        private final Version identity;
        private final Map<ProfileIdentity, ProfileState> profileStates = new HashMap<ProfileIdentity, ProfileState>();

        ProfileVersionStateImpl(ProfileVersion version) {
            this.identity = version.getIdentity();
        }

        @Override
        public Version getIdentity() {
            return identity;
        }

        @Override
        public Map<ProfileIdentity, ProfileState> getProfiles() {
            synchronized (profileStates) {
                HashMap<ProfileIdentity, ProfileState> snapshot = new HashMap<ProfileIdentity, ProfileState>(profileStates);
                return Collections.unmodifiableMap(snapshot);
            }
        }

        ProfileState addProfile(Profile profile) {
            synchronized (profileStates) {
                ProfileState profileState = new ProfileStateImpl(this, profile, getProfileParents(profile));
                profileStates.put(profile.getIdentity(), profileState);
                return profileState;
            }
        }

        ProfileState removeProfile(ProfileIdentity identity) {
            synchronized (profileStates) {
                return profileStates.remove(identity);
            }
        }

        private Set<ProfileState> getProfileParents(Profile profile) {
            synchronized (profileStates) {
                Set<ProfileState> result = new HashSet<ProfileState>();
                for (ProfileIdentity pid : profile.getParents()) {
                    ProfileState pstate = profileStates.get(pid);
                    if (pstate == null)
                        throw new IllegalStateException("Cannot obtain parent profile: " + pid);
                    result.add(pstate);
                }
                return Collections.unmodifiableSet(result);
            }
        }
    }

    static class ProfileStateImpl implements ProfileState {

        private final ProfileIdentity identity;
        private final ProfileVersionState versionState;
        private final Set<ProfileState> parents = new HashSet<ProfileState>();

        ProfileStateImpl(ProfileVersionState versionState, Profile profile, Set<ProfileState> parents) {
            this.versionState = versionState;
            this.identity = profile.getIdentity();
            this.parents.addAll(parents);
        }

        @Override
        public ProfileIdentity getIdentity() {
            return identity;
        }

        @Override
        public ProfileVersionState getProfileVersion() {
            return versionState;
        }

        @Override
        public Set<ProfileState> getParents() {
            return Collections.unmodifiableSet(parents);
        }
    }
}
