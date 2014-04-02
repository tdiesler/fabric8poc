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
package io.fabric8.spi.internal;

import io.fabric8.api.Profile;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileVersion;
import io.fabric8.spi.ProfileService;
import io.fabric8.spi.ProfileState;
import io.fabric8.spi.ProfileVersionState;
import io.fabric8.spi.permit.PermitManager;
import io.fabric8.spi.permit.PermitManager.Permit;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
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
    public Set<Version> getProfileVersionIdentities() {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return Collections.unmodifiableSet(service.getProfileVersions().keySet());
        } finally {
            permit.release();
        }
    }

    @Override
    public Set<ProfileVersion> getProfileVersions(Set<Version> identities) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            Set<ProfileVersion> result = new HashSet<ProfileVersion>();
            for (Entry<Version, ProfileVersionState> entry : service.getProfileVersions().entrySet()) {
                if (identities == null || identities.contains(entry.getKey())) {
                    result.add(new ProfileVersionImpl(entry.getValue()));
                }
            }
            return Collections.unmodifiableSet(result);
        } finally {
            permit.release();
        }
    }

    @Override
    public ProfileVersion getProfileVersion(Version identity) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            ProfileVersionState pvstate = service.getProfileVersions().get(identity);
            return pvstate != null ? new ProfileVersionImpl(pvstate) : null;
        } finally {
            permit.release();
        }
    }

    @Override
    public ProfileVersion addProfileVersion(ProfileVersion version) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return new ProfileVersionImpl(service.addProfileVersion(version));
        } finally {
            permit.release();
        }
    }

    @Override
    public ProfileVersion removeProfileVersion(Version version) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return new ProfileVersionImpl(service.removeProfileVersion(version));
        } finally {
            permit.release();
        }
    }

    @Override
    public Set<ProfileIdentity> getProfileIdentities(Version version) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getProfiles(version).keySet();
        } finally {
            permit.release();
        }
    }

    @Override
    public Set<Profile> getProfiles(Version version, Set<ProfileIdentity> identities) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            Set<Profile> result = new HashSet<Profile>();
            for (Entry<ProfileIdentity, ProfileState> entry : service.getProfiles(version).entrySet()) {
                if (identities == null || identities.contains(entry.getKey())) {
                    result.add(new ProfileImpl(entry.getValue()));
                }
            }
            return Collections.unmodifiableSet(result);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile getProfile(Version version, ProfileIdentity identity) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return new ProfileImpl(service.getProfiles(version).get(identity));
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile addProfile(Version version, Profile profile) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return new ProfileImpl(service.addProfile(version, profile));
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile removeProfile(Version version, ProfileIdentity profile) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return new ProfileImpl(service.removeProfile(version, profile));
        } finally {
            permit.release();
        }
    }

    @Reference
    void bindStateService(PermitManager stateService) {
        this.permitManager.bind(stateService);
    }

    void unbindStateService(PermitManager stateService) {
        this.permitManager.unbind(stateService);
    }
}
