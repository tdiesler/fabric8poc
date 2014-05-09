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
package io.fabric8.core.internal;

import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileVersion;
import io.fabric8.spi.ImmutableProfileVersion;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.utils.IllegalStateAssertion;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.NotNullException;

/**
 * The internal profile registry
 *
 * @author thomas.diesler@jboss.com
 * @since 07-May-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ProfileRegistry.class)
public final class ProfileRegistry extends AbstractComponent {

    private Map<Version, Map<String, Profile>> profileVersions = new HashMap<>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    synchronized Set<Version> getVersions() {
        assertValid();
        return Collections.unmodifiableSet(new HashSet<>(profileVersions.keySet()));
    }

    synchronized ProfileVersion getProfileVersion(Version version) {
        NotNullException.assertValue(version, "version");
        assertValid();
        ProfileVersion profileVersion = null;
        Map<String, Profile> profiles = profileVersions.get(version);
        if (profiles != null) {
            profileVersion = new ImmutableProfileVersion(version, profiles.keySet(), null);
        }
        return profileVersion;
    }

    synchronized ProfileVersion addProfileVersion(LinkedProfileVersion profileVersion) {
        NotNullException.assertValue(profileVersion, "profileVersion");
        assertValid();
        Version version = profileVersion.getIdentity();
        IllegalStateAssertion.assertNull(profileVersions.get(version), "ProfileVersion already exists: " + profileVersion);
        Map<String, Profile> linkedProfiles = profileVersion.getLinkedProfiles();
        IllegalStateAssertion.assertFalse(linkedProfiles.isEmpty(), "ProfileVersion must contain at least one profile: " + profileVersion);
        for (Profile profile : linkedProfiles.values()) {
            addProfile(version, profile);
        }
        return getProfileVersion(version);
    }

    synchronized ProfileVersion removeProfileVersion(Version version) {
        NotNullException.assertValue(version, "version");
        assertValid();
        ProfileVersion profileVersion = getProfileVersion(version);
        profileVersions.remove(version);
        return profileVersion;
    }

    synchronized Profile getProfile(Version version, String identity) {
        NotNullException.assertValue(version, "version");
        NotNullException.assertValue(identity, "identity");
        assertValid();
        Map<String, Profile> profiles = profileVersions.get(version);
        return profiles != null ? profiles.get(identity) : null;
    }

    synchronized Profile addProfile(Version version, Profile profile) {
        NotNullException.assertValue(version, "version");
        NotNullException.assertValue(profile, "profile");
        assertValid();
        IllegalStateAssertion.assertEquals(version, profile.getVersion(), "Unexpected profile version: " + profile);
        IllegalStateAssertion.assertNull(getProfile(version, profile.getIdentity()), "Profile already exists: " + profile);
        Map<String, Profile> profiles = profileVersions.get(version);
        if (profiles == null) {
            profiles = new HashMap<>();
            profileVersions.put(version, profiles);
        }
        profiles.put(profile.getIdentity(), profile);
        return profile;
    }

    synchronized Profile removeProfile(Version version, String identity) {
        NotNullException.assertValue(version, "version");
        NotNullException.assertValue(identity, "identity");
        assertValid();
        Map<String, Profile> profiles = profileVersions.get(version);
        return profiles != null ? profiles.remove(identity) : null;
    }
}
