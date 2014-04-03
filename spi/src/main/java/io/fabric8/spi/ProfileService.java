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
import io.fabric8.api.ProfileVersion;
import io.fabric8.spi.permit.PermitState;

import java.util.Map;

import org.jboss.gravia.resource.Version;

/**
 * The internal profile service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProfileService {

    /**
     * The default profile version
     */
    Version DEFAULT_PROFILE_VERSION = Version.parseVersion("1.0");

    /**
     * The default profile name
     */
    String DEFAULT_PROFILE_NAME = "default";

    /**
     * The {@link PermitState} that protects this service.
     */
    PermitState<ProfileService> PERMIT = new PermitState<ProfileService>(ProfileService.class);

    /**
     * Get the default profile
     */
    ProfileState getDefaultProfile();

    /**
     * Get the set of profile versions in the cluster
     */
    Map<Version, ProfileVersionState> getProfileVersions();

    /**
     * Get the profile version for the given identity
     */
    ProfileVersionState getProfileVersion(Version identity);

    /**
     * Add a profile version
     */
    ProfileVersionState addProfileVersion(ProfileVersion version);

    /**
     * Remove a profile version
     */
    ProfileVersionState removeProfileVersion(Version version);

    /**
     * Get the profiles for a given version
     */
    Map<ProfileIdentity, ProfileState> getProfiles(Version version);

    /**
     * Get the profile for a given version and identity
     */
    ProfileState getProfile(Version version, ProfileIdentity identity);

    /**
     * Add a profile to the given version
     */
    ProfileState addProfile(Version version, Profile profile);

    /**
     * Remove the profile with the given identity
     */
    ProfileState removeProfile(Version version, ProfileIdentity identity);
}
