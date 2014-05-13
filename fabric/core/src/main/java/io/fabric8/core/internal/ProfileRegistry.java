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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileItem;
import io.fabric8.api.ProfileVersion;
import io.fabric8.spi.DefaultResourceItem;
import io.fabric8.spi.ImmutableProfile;
import io.fabric8.spi.ImmutableProfileVersion;
import io.fabric8.spi.ImportableResourceItem;
import io.fabric8.spi.scr.AbstractComponent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.gravia.utils.IllegalArgumentAssertion;

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
    private Path profilesDir;

    @Activate
    void activate() {
        activateInternal();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    private void activateInternal() {
        profilesDir = Paths.get(".", "target", "profiles").toAbsolutePath();
    }

    synchronized Set<Version> getVersions() {
        assertValid();
        return Collections.unmodifiableSet(new HashSet<>(profileVersions.keySet()));
    }

    synchronized ProfileVersion getProfileVersion(Version version) {
        assertValid();
        ProfileVersion profileVersion = null;
        Map<String, Profile> profiles = profileVersions.get(version);
        if (profiles != null) {
            profileVersion = new ImmutableProfileVersion(version, profiles.keySet(), null);
        }
        return profileVersion;
    }

    synchronized ProfileVersion addProfileVersion(LinkedProfileVersion profileVersion) {
        assertValid();
        Version version = profileVersion.getIdentity();
        Map<String, Profile> linkedProfiles = profileVersion.getLinkedProfiles();
        for (Profile profile : linkedProfiles.values()) {
            addProfile(version, profile);
        }
        return getProfileVersion(version);
    }

    synchronized ProfileVersion removeProfileVersion(Version version) {
        assertValid();
        ProfileVersion profileVersion = getProfileVersion(version);
        profileVersions.remove(version);
        return profileVersion;
    }

    synchronized Profile getProfile(Version version, String identity) {
        assertValid();
        Map<String, Profile> profiles = profileVersions.get(version);
        return profiles != null ? profiles.get(identity) : null;
    }

    synchronized Profile addProfile(Version version, Profile profile) {
        assertValid();
        Map<String, Profile> profiles = profileVersions.get(version);
        if (profiles == null) {
            profiles = new HashMap<>();
            profileVersions.put(version, profiles);
        }
        Set<ProfileItem> profileItems = new HashSet<>();
        for (ProfileItem item : profile.getProfileItems(null)) {
            if (item instanceof ImportableResourceItem) {
                URL resourceURL = addImportableResourceItem(profile, (ImportableResourceItem) item);
                item = new DefaultResourceItem(item.getIdentity(), item.getAttributes(), resourceURL);
            }
            profileItems.add(item);
        }
        profile = new ImmutableProfile(version, profile.getIdentity(), profile.getAttributes(), profile.getParents(), profileItems, null);
        profiles.put(profile.getIdentity(), profile);
        return profile;
    }

    synchronized Profile removeProfile(Version version, String identity) {
        assertValid();
        Map<String, Profile> profiles = profileVersions.get(version);
        return profiles != null ? profiles.remove(identity) : null;
    }

    private URL addImportableResourceItem(Profile profile, ImportableResourceItem item) {
        InputStream inputStream = item.getInputStream();
        IllegalStateAssertion.assertNotNull(inputStream, "No input stream for: " + item);
        String identity = item.getIdentity();
        File targetFile = copyResourceItem(profile, identity, inputStream);
        return getResourceItemURL(profile, identity, targetFile);
    }

    private File copyResourceItem(Profile profile, String identity, InputStream inputStream) {
        Path targetPath = Paths.get(profilesDir.toString(), profile.getVersion().toString(), profile.getIdentity(), identity);
        try {
            File targetDir = targetPath.toFile().getParentFile();
            IllegalStateAssertion.assertTrue(targetDir.isDirectory() || targetDir.mkdirs(), "Cannot create directory: " + targetDir);
            Files.copy(inputStream, targetPath, REPLACE_EXISTING);
            return targetPath.toFile();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private URL getResourceItemURL(Profile profile, String itemid, File targetFile) {
        IllegalArgumentAssertion.assertNotNull(profile, "profile");
        IllegalArgumentAssertion.assertNotNull(profile.getVersion(), "version");
        IllegalArgumentAssertion.assertNotNull(itemid, "itemid");
        try {
            String spec = "profile://" + profile.getVersion() + "/" + profile.getIdentity() + "/" + itemid;
            return new URL(null, spec, new ProfileURLStreamHandler(targetFile));
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
