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
import io.fabric8.spi.ContainerState;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.ProfileState;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.scr.AbstractProtectedComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.gravia.resource.Resource;
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
    public List<Version> getVersions() {
        synchronized (profileVersions) {
            List<Version> versions = new ArrayList<Version>(profileVersions.keySet());
            return Collections.unmodifiableList(versions);
        }
    }

    @Override
    public void addProfileVersion(Version version) {
        synchronized (profileVersions) {
            ProfileVersionState versionState = new ProfileVersionState(version);
            profileVersions.put(version, versionState);
        }
    }

    @Override
    public void removeProfileVersion(Version version) {
        synchronized (profileVersions) {
            profileVersions.remove(version);
        }
    }

    @Override
    public Set<ProfileIdentity> getAllProfiles() {
        synchronized (profileVersions) {
            Set<ProfileIdentity> result = new HashSet<ProfileIdentity>();
            for (Version version : profileVersions.keySet()) {
                result.addAll(getProfiles(version));
            }
            return Collections.unmodifiableSet(result);
        }
    }

    @Override
    public Set<ProfileIdentity> getProfiles(Version version) {
        synchronized (profileVersions) {
            Set<ProfileIdentity> result = new HashSet<ProfileIdentity>();
            ProfileVersionState versionState = profileVersions.get(version);
            if (versionState != null) {
                for (ProfileState state : versionState.getProfileStates()) {
                    result.add(state.getIdentity());
                }
            }
            return Collections.unmodifiableSet(result);
        }
    }

    @Override
    public ProfileState getProfile(ProfileIdentity identity) {
        synchronized (profileVersions) {
            ProfileVersionState versionState = profileVersions.get(identity.getVersion());
            return versionState != null ? versionState.getProfile(identity.getSymbolicName()) : null;
        }
    }

    @Override
    public ProfileState addProfile(Profile profile, Version version) {
        synchronized (profileVersions) {
            if (version == null) {
                version = profile.getIdentity().getVersion();
            }
            ProfileVersionState versionState = profileVersions.get(version);
            if (versionState == null)
                throw new IllegalStateException("Profile version does not exist: " + version);

            return versionState.addProfile(profile);
        }
    }

    @Override
    public ProfileState removeProfile(ProfileIdentity profile) {
        throw new UnsupportedOperationException();
    }

    @Reference
    void bindPermitManager(PermitManager stateService) {
        this.permitManager.bind(stateService);
    }

    void unbindPermitManager(PermitManager stateService) {
        this.permitManager.unbind(stateService);
    }

    static class ProfileVersionState {

        private final Version identity;
        private Map<String, ProfileState> profileStates = new LinkedHashMap<String, ProfileState>();

        ProfileVersionState(Version identity) {
            this.identity = identity;
        }

        Version getIdentity() {
            return identity;
        }

        List<ProfileState> getProfileStates() {
            synchronized (profileStates) {
                List<ProfileState> list = new ArrayList<ProfileState>(profileStates.values());
                return Collections.unmodifiableList(list);
            }
        }

        ProfileState getProfile(String symbolicName) {
            synchronized (profileStates) {
                return profileStates.get(symbolicName);
            }
        }

        boolean hasProfile(String symbolicName) {
            synchronized (profileStates) {
                return profileStates.containsKey(symbolicName);
            }
        }

        ProfileState addProfile(Profile profile) {
            synchronized (profileStates) {
                ProfileState profileState = new ProfileStateImpl(profile);
                profileStates.put(profile.getIdentity().getSymbolicName(), profileState);
                return profileState;
            }
        }
    }

    static class ProfileStateImpl implements ProfileState {

        private final ProfileIdentity identity;
        private final List<Resource> resources = new ArrayList<Resource>();
        private final Set<ContainerState> containers = new HashSet<ContainerState>();

        ProfileStateImpl(Profile profile) {
            this.identity = profile.getIdentity();
            this.resources.addAll(profile.getResources());
        }

        @Override
        public ProfileIdentity getIdentity() {
            return identity;
        }

        @Override
        public Set<ContainerState> getContainers() {
            return Collections.unmodifiableSet(containers);
        }

        @Override
        public List<Resource> getResources() {
            return Collections.unmodifiableList(resources);
        }
    }
}
