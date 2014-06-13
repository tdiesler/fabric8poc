/*
 * #%L
 * Fabric8 :: SPI
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
package io.fabric8.spi.internal;

import io.fabric8.api.LinkedProfile;
import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.LockHandle;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileVersion;
import io.fabric8.api.VersionIdentity;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.permit.PermitManager.Permit;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ProfileManager.class)
public final class ProfileManagerImpl extends AbstractComponent implements ProfileManager {

    @Reference(referenceInterface = PermitManager.class)
    private final ValidatingReference<PermitManager> permitManager = new ValidatingReference<PermitManager>();

    @Activate
    void activate(Map<String, ?> config) {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public LockHandle aquireProfileVersionLock(VersionIdentity identity) {
        final Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        final ProfileService service = permit.getInstance();
        final LockHandle writeLock = service.aquireProfileVersionLock(identity);
        return new LockHandle() {
            @Override
            public void unlock() {
                writeLock.unlock();
                permit.release();
            }
        };
    }

    @Override
    public ProfileVersion getDefaultProfileVersion() {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getDefaultProfileVersion();
        } finally {
            permit.release();
        }
    }

    @Override
    public Set<VersionIdentity> getVersions() {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getVersions();
        } finally {
            permit.release();
        }
    }

    @Override
    public Set<ProfileVersion> getProfileVersions(Set<VersionIdentity> identities) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getProfileVersions(identities);
        } finally {
            permit.release();
        }
    }

    @Override
    public ProfileVersion getProfileVersion(VersionIdentity identity) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getProfileVersion(identity);
        } finally {
            permit.release();
        }
    }

    @Override
    public LinkedProfileVersion getLinkedProfileVersion(VersionIdentity identity) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getLinkedProfileVersion(identity);
        } finally {
            permit.release();
        }
    }

    @Override
    public ProfileVersion addProfileVersion(ProfileVersion version) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.addProfileVersion(version);
        } finally {
            permit.release();
        }
    }

    @Override
    public ProfileVersion removeProfileVersion(VersionIdentity version) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.removeProfileVersion(version);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile getDefaultProfile() {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getDefaultProfile();
        } finally {
            permit.release();
        }
    }

    @Override
    public Set<String> getProfileIdentities(VersionIdentity version) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getProfileIdentities(version);
        } finally {
            permit.release();
        }
    }

    @Override
    public Set<Profile> getProfiles(VersionIdentity version, Set<String> identities) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getProfiles(version, identities);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile getProfile(VersionIdentity version, String identity) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getProfile(version, identity);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile getEffectiveProfile(VersionIdentity version, String identity) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getEffectiveProfile(version, identity);
        } finally {
            permit.release();
        }
    }

    @Override
    public LinkedProfile getLinkedProfile(VersionIdentity version, String identity) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getLinkedProfile(version, identity);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile addProfile(VersionIdentity version, Profile profile) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.addProfile(version, profile);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile removeProfile(VersionIdentity version, String identity) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.removeProfile(version, identity);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile updateProfile(Profile profile, ProfileEventListener listener) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.updateProfile(profile, listener);
        } finally {
            permit.release();
        }
    }

    void bindPermitManager(PermitManager service) {
        this.permitManager.bind(service);
    }

    void unbindPermitManager(PermitManager service) {
        this.permitManager.unbind(service);
    }
}
