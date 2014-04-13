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
package io.fabric8.core.api;

import java.util.Set;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.runtime.Runtime;

/**
 * A profile manager
 *
 * An instance of this service can be obtained from the gravia {@link Runtime}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProfileManager {

    /**
     * Aquire an exclusive write lock for the given profile version
     */
    LockHandle aquireProfileVersionLock(Version version);

    /**
     * Get the set of profile version identities in the cluster
     */
    Set<Version> getProfileVersionIds();

    /**
     * Get the set of profile versions for the given identities
     * @param identities The requested identities or <code>null</code> for all profile versions
     */
    Set<ProfileVersion> getProfileVersions(Set<Version> identities);

    /**
     * Get the profile versions for the given identity
     */
    ProfileVersion getProfileVersion(Version identity);

    /**
     * Add a profile version
     */
    ProfileVersion addProfileVersion(ProfileVersion profileVersion);

    /**
     * Remove a profile version
     */
    ProfileVersion removeProfileVersion(Version identity);

    /**
     * Get the default profile
     */
    Profile getDefaultProfile();

    /**
     * Get the profile idetities for a given version
     */
    Set<ProfileIdentity> getProfileIds(Version version);

    /**
     * Get the profiles for a given version and identities
     * @param identities The requested identities or <code>null</code> for all profile versions
     */
    Set<Profile> getProfiles(Version version, Set<ProfileIdentity> identities);

    /**
     * Get the profile for a given identity and version
     */
    Profile getProfile(Version version, ProfileIdentity identity);

    /**
     * Add a profile to the given version
     */
    Profile addProfile(Version version, Profile profile);

    /**
     * Remove a profile from the given version
     */
    Profile removeProfile(Version version, ProfileIdentity identity);

    /**
     * Update the given profile
     */
    Profile updateProfile(Version version, Profile profile, ProfileEventListener listener);
}
