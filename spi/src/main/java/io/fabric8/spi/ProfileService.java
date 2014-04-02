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
package io.fabric8.spi;

import io.fabric8.api.Profile;
import io.fabric8.api.ProfileIdentity;
import io.fabric8.spi.permit.PermitState;

import java.util.List;
import java.util.Set;

import org.jboss.gravia.resource.Version;

/**
 * The internal profile service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProfileService {

    /**
     * The {@link PermitState} that protects this service.
     */
    PermitState<ProfileService> PERMIT = new PermitState<ProfileService>(ProfileService.class);

    /**
     * Get the list of profile versions in the cluster
     */
    List<Version> getVersions();

    /**
     * Add a profile version
     */
    void addProfileVersion(Version version);

    /**
     * Remove a profile version
     */
    void removeProfileVersion(Version version);

    /**
     * Get all profiles
     */
    Set<ProfileIdentity> getAllProfiles();

    /**
     * Get the profiles for a given version
     */
    Set<ProfileIdentity> getProfiles(Version version);

    /**
     * Get the profile for a given identity
     */
    ProfileState getProfile(ProfileIdentity identity);

    /**
     * Add a profile to the given version
     *
     * @param version can be null, in which case the profile identity must specify the version
     */
    ProfileState addProfile(Profile profile, Version version);

    /**
     * Remove the profile with the given identity
     */
    ProfileState removeProfile(ProfileIdentity profile);
}
