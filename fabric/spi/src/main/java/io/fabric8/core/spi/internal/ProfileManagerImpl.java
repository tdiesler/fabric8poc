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
package io.fabric8.core.spi.internal;

import io.fabric8.core.api.LockHandle;
import io.fabric8.core.api.Profile;
import io.fabric8.core.api.ProfileEventListener;
import io.fabric8.core.api.ProfileIdentity;
import io.fabric8.core.api.ProfileManager;
import io.fabric8.core.api.ProfileVersion;
import io.fabric8.core.spi.ProfileService;
import io.fabric8.core.spi.permit.PermitManager;
import io.fabric8.core.spi.permit.PermitManager.Permit;
import io.fabric8.core.spi.scr.AbstractComponent;
import io.fabric8.core.spi.scr.ValidatingReference;

import java.util.Map;
import java.util.Set;

import org.jboss.gravia.resource.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = { ProfileManager.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public final class ProfileManagerImpl extends AbstractComponent implements ProfileManager {

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
    public LockHandle aquireProfileVersionLock(Version identity) {
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
    public Set<Version> getProfileVersionIdentities() {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getProfileVersionIdentities();
        } finally {
            permit.release();
        }
    }

    @Override
    public Set<ProfileVersion> getProfileVersions(Set<Version> identities) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getProfileVersions(identities);
        } finally {
            permit.release();
        }
    }

    @Override
    public ProfileVersion getProfileVersion(Version identity) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getProfileVersion(identity);
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
    public ProfileVersion removeProfileVersion(Version version) {
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
    public Set<ProfileIdentity> getProfileIdentities(Version version) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getProfileIdentities(version);
        } finally {
            permit.release();
        }
    }

    @Override
    public Set<Profile> getProfiles(Version version, Set<ProfileIdentity> identities) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getProfiles(version, identities);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile getProfile(Version version, ProfileIdentity identity) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getProfile(version, identity);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile addProfile(Version version, Profile profile) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.addProfile(version, profile);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile removeProfile(Version version, ProfileIdentity identity) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.removeProfile(version, identity);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile updateProfile(Version version, Profile profile, ProfileEventListener listener) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.updateProfile(version, profile, listener);
        } finally {
            permit.release();
        }
    }

    @Reference
    void bindPermitManager(PermitManager service) {
        this.permitManager.bind(service);
    }

    void unbindPermitManager(PermitManager service) {
        this.permitManager.unbind(service);
    }
}
