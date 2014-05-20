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
import io.fabric8.api.ResourceItem;
import io.fabric8.spi.DefaultResourceItem;
import io.fabric8.spi.ImmutableProfile;
import io.fabric8.spi.ImmutableProfileVersion;
import io.fabric8.spi.scr.AbstractComponent;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.resource.Capability;
import org.jboss.gravia.resource.ContentCapability;
import org.jboss.gravia.resource.ContentNamespace;
import org.jboss.gravia.resource.DefaultResourceBuilder;
import org.jboss.gravia.resource.Requirement;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceBuilder;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;

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

    Set<Version> getVersions() {
        assertValid();
        synchronized (profileVersions) {
            return Collections.unmodifiableSet(new HashSet<>(profileVersions.keySet()));
        }
    }

    ProfileVersion getProfileVersion(Version version) {
        assertValid();
        ProfileVersion profileVersion = null;
        synchronized (profileVersions) {
            Map<String, Profile> profiles = profileVersions.get(version);
            if (profiles != null) {
                profileVersion = new ImmutableProfileVersion(version, profiles.keySet(), null);
            }
        }
        return profileVersion;
    }

    ProfileVersion addProfileVersion(LinkedProfileVersion profileVersion) {
        assertValid();
        synchronized (profileVersions) {
            Version version = profileVersion.getIdentity();
            Map<String, Profile> linkedProfiles = profileVersion.getLinkedProfiles();
            for (Profile profile : linkedProfiles.values()) {
                addProfile(version, profile);
            }
            return getProfileVersion(version);
        }
    }

    ProfileVersion removeProfileVersion(Version version) {
        assertValid();
        synchronized (profileVersions) {
            ProfileVersion profileVersion = getProfileVersion(version);
            profileVersions.remove(version);
            return profileVersion;
        }
    }

    Profile getProfile(Version version, String identity) {
        assertValid();
        synchronized (profileVersions) {
            Map<String, Profile> profiles = profileVersions.get(version);
            return profiles != null ? profiles.get(identity) : null;
        }
    }

    Profile addProfile(Version version, Profile profile) {
        assertValid();
        synchronized (profileVersions) {
            Map<String, Profile> profiles = profileVersions.get(version);
            if (profiles == null) {
                profiles = new HashMap<>();
                profileVersions.put(version, profiles);
            }
            List<ProfileItem> profileItems = new ArrayList<>();
            for (ProfileItem item : profile.getProfileItems(null)) {
                if (item instanceof ResourceItem) {
                    ResourceItem resitem = (ResourceItem) item;
                    Resource resource = processResourceItem(profile, resitem);
                    item = new DefaultResourceItem(resource, resitem.isShared());
                }
                profileItems.add(item);
            }
            profile = new ImmutableProfile(version, profile.getIdentity(), profile.getAttributes(), profile.getParents(), profileItems, null);
            profiles.put(profile.getIdentity(), profile);
            return profile;
        }
    }

    Profile removeProfile(Version version, String identity) {
        assertValid();
        synchronized (profileVersions) {
            Map<String, Profile> profiles = profileVersions.get(version);
            return profiles != null ? profiles.remove(identity) : null;
        }
    }

    URLConnection getProfileURLConnection(URL url) throws IOException {
        String path = url.getPath();
        String version = url.getHost();
        String profile = path.substring(1, path.lastIndexOf('/'));
        String item = path.substring(path.lastIndexOf('/') + 1);
        Path versionPath = Paths.get(profilesDir.toString(), version);
        IllegalStateAssertion.assertTrue(versionPath.toFile().isDirectory(), "Cannot find version directory: " + versionPath);
        Path profilePath = Paths.get(versionPath.toString(), profile);
        IllegalStateAssertion.assertTrue(profilePath.toFile().isDirectory(), "Cannot find profile directory: " + profilePath);
        Path itemPath = Paths.get(profilePath.toString(), item);
        IllegalStateAssertion.assertTrue(itemPath.toFile().isDirectory(), "Cannot find item file: " + itemPath);
        Path contentPath = null;
        if (url.getQuery() != null) {
            String query = url.getQuery().substring(1);
            for (String param : query.split("&")) {
                String[] keyval = param.split("=");
                IllegalStateAssertion.assertEquals(2, keyval.length, "Unexpected array length: " + Arrays.asList(keyval));
                String key = keyval[0];
                String val = keyval[1];
                if ("cntindex".equals(key)) {
                    contentPath = Paths.get(itemPath.toString(), "content" + val);
                    break;
                }
            }
        } else {
            contentPath = Paths.get(itemPath.toString(), "content0");
        }
        IllegalStateAssertion.assertNotNull(contentPath, "Cannot obtain content path from: " + url);
        IllegalStateAssertion.assertTrue(contentPath.toFile().isFile(), "Cannot find item file: " + contentPath);
        return contentPath.toFile().toURI().toURL().openConnection();
    }

    private Resource processResourceItem(Profile profile, ResourceItem item) {
        IllegalArgumentAssertion.assertNotNull(profile, "profile");
        IllegalArgumentAssertion.assertNotNull(item, "item");
        Resource resource = item.getResource();

        // Copy von-content capabilities
        ResourceBuilder builder = new DefaultResourceBuilder();
        for (Capability cap : resource.getCapabilities(null)) {
            if (!ContentNamespace.CONTENT_NAMESPACE.equals(cap.getNamespace())) {
                builder.addCapability(cap.getNamespace(), cap.getAttributes(), cap.getDirectives());
            }
        }

        // Process the content capabilities & update the contentURL
        List<Capability> ccaps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        for (int i = 0; i < ccaps.size(); i++) {
            ContentCapability ccap = ccaps.get(i).adapt(ContentCapability.class);
            Path targetPath = Paths.get(profilesDir.toString(), profile.getVersion().toString(), profile.getIdentity(), item.getIdentity(), "content" + i);
            URL contentURL;
            try {
                File targetDir = targetPath.toFile().getParentFile();
                IllegalStateAssertion.assertTrue(targetDir.isDirectory() || targetDir.mkdirs(), "Cannot create directory: " + targetDir);
                Files.copy(getRequiredCapabilityContent(ccap), targetPath, REPLACE_EXISTING);
                String spec = "profile://" + profile.getVersion() + "/" + profile.getIdentity() + "/" + item.getIdentity();
                if (ccaps.size() > 1) {
                    spec += "?cntindex=" + i;
                }
                contentURL = new URL(null, spec, new ProfileURLStreamHandler(targetPath.toFile()));
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
            Map<String, Object> atts = new HashMap<>(ccap.getAttributes());
            atts.remove(ContentNamespace.CAPABILITY_STREAM_ATTRIBUTE);
            builder.addContentCapability(contentURL, atts, ccap.getDirectives());
        }

        // Copy the requirements
        for (Requirement req : resource.getRequirements(null)) {
            builder.addRequirement(req.getNamespace(), req.getAttributes(), req.getDirectives());
        }

        return builder.getResource();
    }

    private InputStream getRequiredCapabilityContent(ContentCapability ccap) throws IOException {
        InputStream content = ccap.getContentStream();
        if (content == null) {
            content = ccap.getContentURL().openStream();
        }
        IllegalStateAssertion.assertNotNull(content, "Cannot obtain content from: " + ccap);
        return content;
    }
}
