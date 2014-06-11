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
package io.fabric8.core;

import static io.fabric8.api.Constants.DEFAULT_PROFILE_IDENTITY;
import static io.fabric8.api.Constants.DEFAULT_PROFILE_VERSION;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import io.fabric8.api.Container;
import io.fabric8.api.LinkedProfileVersion;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ResourceItem;
import io.fabric8.spi.DefaultProfileBuilder;
import io.fabric8.spi.DefaultProfileVersionBuilder;
import io.fabric8.spi.DefaultProfileXMLReader;
import io.fabric8.spi.DefaultProfileXMLWriter;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;
import io.fabric8.spi.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jboss.gravia.repository.spi.AbstractContentHandler;
import org.jboss.gravia.resource.Capability;
import org.jboss.gravia.resource.ContentCapability;
import org.jboss.gravia.resource.ContentNamespace;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.IOUtils;
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

    private static String PROFILES_FILE = "profiles.xml";

    private Path workspace;

    @Reference(referenceInterface = RuntimeService.class)
    private final ValidatingReference<RuntimeService> runtimeService = new ValidatingReference<>();

    @Activate
    void activate() {
        activateInternal();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        dactivateInternal();
        deactivateComponent();
    }

    private void activateInternal() {
        Path dataPath = runtimeService.get().getDataPath();
        workspace = dataPath.resolve("profiles");

        // Add the default profile version
        File defaultFile = getProfilesFile(DEFAULT_PROFILE_VERSION);
        if (!defaultFile.exists()) {
            Profile profile = new DefaultProfileBuilder(DEFAULT_PROFILE_IDENTITY)
                .addConfigurationItem(Container.CONTAINER_SERVICE_PID, Collections.singletonMap("config.token", (Object) "default"))
                .getProfile();

            LinkedProfileVersion profileVersion = new DefaultProfileVersionBuilder(DEFAULT_PROFILE_VERSION)
                .addProfile(profile)
                .getProfileVersion();

            addProfileVersionInternal(profileVersion);
        }
    }

    private void dactivateInternal() {
    }

    Set<Version> getVersions() {
        assertValid();
        synchronized (workspace) {
            Set<Version> versions = new HashSet<>();
            for (String name : workspace.toFile().list()) {
                if (workspace.resolve(name).toFile().isDirectory()) {
                    versions.add(Version.parseVersion(name));
                }
            }
            return Collections.unmodifiableSet(versions);
        }
    }

    /**
     * Get the {@link LinkedProfileVersion} for the given version
     * @return null if the version does not exist
     */
    LinkedProfileVersion getProfileVersion(Version version) {
        assertValid();
        return getProfileVersionInternal(version);
    }

    private LinkedProfileVersion getProfileVersionInternal(Version version) {
        File profilesFile = getProfilesFile(version);
        if (profilesFile.exists()) {
            return getProfileVersionBuilder(version).getProfileVersion();
        } else {
            return null;
        }
    }

    private ProfileVersionBuilder getProfileVersionBuilder(Version version) {
        ProfileVersionBuilder builder = new DefaultProfileVersionBuilder(version);
        synchronized (workspace) {
            File profilesFile = getRequiredProfilesFile(version);

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(profilesFile);
                DefaultProfileXMLReader reader = new DefaultProfileXMLReader(fis);
                Profile profile = reader.nextProfile();
                while (profile != null) {
                    builder.addProfile(profile);
                    profile = reader.nextProfile();
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot profile version: " + version, ex);
            } finally {
                IOUtils.safeClose(fis);
            }
        }
        return builder;
    }

    LinkedProfileVersion addProfileVersion(LinkedProfileVersion profileVersion) {
        assertValid();
        return addProfileVersionInternal(profileVersion);
    }

    private LinkedProfileVersion addProfileVersionInternal(LinkedProfileVersion profileVersion) {
        synchronized (workspace) {
            Version version = profileVersion.getIdentity();
            Path versionPath = getProfileVersionPath(version);
            IllegalStateAssertion.assertFalse(versionPath.toFile().exists(), "Profile version path already exists: " + versionPath);
            IllegalStateAssertion.assertTrue(versionPath.toFile().mkdirs(), "Cannot create profile version directory: " + versionPath);
            return writeProfileVersion(profileVersion);
        }
    }

    LinkedProfileVersion removeProfileVersion(Version version) {
        assertValid();
        synchronized (workspace) {
            LinkedProfileVersion profileVersion = getProfileVersion(version);
            deleteProfileVersionPath(version);
            return profileVersion;
        }
    }

    Profile getProfile(Version version, String identity) {
        assertValid();
        Profile result = null;
        synchronized (workspace) {
            FileInputStream fis = null;
            try {
                File profilesFile = getRequiredProfilesFile(version);
                fis = new FileInputStream(profilesFile);
                DefaultProfileXMLReader reader = new DefaultProfileXMLReader(fis);
                Profile profile = reader.nextProfile();
                while (profile != null) {
                    if (profile.getIdentity().equals(identity)) {
                        result = profile;
                        break;
                    }
                    profile = reader.nextProfile();
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot profile version: " + version, ex);
            } finally {
                IOUtils.safeClose(fis);
            }
        }
        return result;
    }

    Profile getRequiredProfile(Version version, String identity) {
        Profile profile = getProfile(version, identity);
        IllegalStateAssertion.assertNotNull(profile, "Cannot obtain profile '" + identity + "' from: " + version);
        return profile;
    }

    Profile addProfile(Version version, Profile profile) {
        assertValid();
        synchronized (workspace) {
            ProfileVersionBuilder builder = getProfileVersionBuilder(version);
            writeProfileVersion(builder.addProfile(profile).getProfileVersion());
            return getProfile(version, profile.getIdentity());
        }
    }

    Profile updateProfile(Version version, Profile profile) {
        assertValid();
        synchronized (workspace) {
            String identity = profile.getIdentity();
            ProfileVersionBuilder builder = getProfileVersionBuilder(version);
            builder.removeProfile(identity);
            deleteProfilePath(version, identity);
            writeProfileVersion(builder.addProfile(profile).getProfileVersion());
            return getProfile(version, identity);
        }
    }

    Profile removeProfile(Version version, String identity) {
        assertValid();
        synchronized (workspace) {
            Profile profile = getRequiredProfile(version, identity);
            ProfileVersionBuilder builder = getProfileVersionBuilder(version);
            builder.removeProfile(identity);
            deleteProfilePath(version, identity);
            writeProfileVersion(builder.getProfileVersion());
            return profile;
        }
    }

    URLConnection getProfileURLConnection(URL url) throws IOException {
        String path = url.getPath();
        String version = url.getHost();
        String profile = path.substring(1, path.lastIndexOf('/'));
        String item = path.substring(path.lastIndexOf('/') + 1);
        Path profilePath = getRequiredProfilePath(Version.parseVersion(version), profile);
        Path itemPath = profilePath.resolve(item);
        IllegalStateAssertion.assertTrue(itemPath.toFile().isDirectory(), "Cannot find item directory: " + itemPath);
        int cntindex = 0;
        Version resourceVersion = null;
        if (url.getQuery() != null) {
            String query = url.getQuery().substring(1);
            for (String param : query.split("&")) {
                String[] keyval = param.split("=");
                IllegalStateAssertion.assertEquals(2, keyval.length, "Unexpected array length: " + Arrays.asList(keyval));
                String key = keyval[0];
                String val = keyval[1];
                if ("version".equals(key)) {
                    resourceVersion = Version.parseVersion(val);
                } else if ("cntindex".equals(key)) {
                    cntindex = Integer.parseInt(val);
                }
            }
        }
        Path versionPath;
        if (resourceVersion != null) {
            versionPath = Paths.get(itemPath.toString(), resourceVersion.toString());
        } else {
            Version higest = Version.emptyVersion;
            for (String verstr : itemPath.toFile().list()) {
                Version nextver = Version.parseVersion(verstr);
                if (nextver.compareTo(higest) > 0) {
                    higest = nextver;
                }
            }
            versionPath = Paths.get(itemPath.toString(), higest.toString());
        }
        IllegalStateAssertion.assertTrue(versionPath.toFile().isDirectory(), "Cannot find version directory: " + versionPath);
        Path contentPath = Paths.get(versionPath.toString(), "content" + cntindex);
        IllegalStateAssertion.assertNotNull(contentPath, "Cannot obtain content path from: " + url);
        IllegalStateAssertion.assertTrue(contentPath.toFile().isFile(), "Cannot find item file: " + contentPath);
        return contentPath.toFile().toURI().toURL().openConnection();
    }

    private LinkedProfileVersion writeProfileVersion(LinkedProfileVersion profileVersion) {
        Version version = profileVersion.getIdentity();
        FileOutputStream fos = null;
        try {
            File profilesFile = getProfilesFile(version);
            fos = new FileOutputStream(profilesFile);
            DefaultProfileXMLWriter writer = new DefaultProfileXMLWriter(fos, new ResourceItemContentHandler());
            writer.writeProfileVersion(profileVersion);
            writer.close();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot add profile version: " + version, ex);
        } finally {
            IOUtils.safeClose(fos);
        }
        return getProfileVersionInternal(version);
    }

    private Path getProfileVersionPath(Version version) {
        return workspace.resolve(version.toString());
    }

    private Path getRequiredProfileVersionPath(Version version) {
        Path versionPath = getProfileVersionPath(version);
        IllegalStateAssertion.assertTrue(versionPath.toFile().exists(), "Cannot find profile version directory: " + versionPath);
        return versionPath;
    }

    private Path getProfilePath(Version version, String identity) {
        return getProfileVersionPath(version).resolve(identity);
    }

    private Path getRequiredProfilePath(Version version, String identity) {
        Path profilePath = getProfilePath(version, identity);
        IllegalStateAssertion.assertTrue(profilePath.toFile().exists(), "Cannot find profile directory: " + profilePath);
        return profilePath;
    }

    private File getProfilesFile(Version version) {
        Path versionPath = getProfileVersionPath(version);
        return versionPath.resolve(PROFILES_FILE).toFile();
    }

    private File getRequiredProfilesFile(Version version) {
        File profilesFile = getProfilesFile(version);
        IllegalStateAssertion.assertTrue(profilesFile.exists(), "Profiles file does not exist: " + profilesFile);
        return profilesFile;
    }

    private void deleteProfilePath(Version version, String identity) {
        Path profilePath = getProfilePath(version, identity);
        try {
            FileUtils.deleteRecursively(profilePath);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot delete profile path: " + profilePath);
        }
    }

    private void deleteProfileVersionPath(Version version) {
        Path versionPath = getRequiredProfileVersionPath(version);
        try {
            FileUtils.deleteRecursively(versionPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot delete profile version path: " + versionPath);
        }
    }

    void bindRuntimeService(RuntimeService service) {
        runtimeService.bind(service);
    }
    void unbindRuntimeService(RuntimeService service) {
        runtimeService.unbind(service);
    }

    class ResourceItemContentHandler extends AbstractContentHandler {

        @Override
        public Map<String, Object> process(ContentCapability ccap) throws IOException {
            Profile profile = getContextItem(Profile.class);
            ResourceItem resItem = getContextItem(ResourceItem.class);
            Path profilePath = getProfileVersionPath(profile.getVersion()).resolve(profile.getIdentity());
            List<Capability> ccaps = resItem.getResource().getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
            int i = ccaps.indexOf(ccap);
            Path targetPath = Paths.get(profilePath.toString(), resItem.getSymbolicName(), resItem.getVersion().toString(), "content" + i);
            URL contentURL;
            try {
                File targetDir = targetPath.toFile().getParentFile();
                IllegalStateAssertion.assertTrue(targetDir.isDirectory() || targetDir.mkdirs(), "Cannot create directory: " + targetDir);
                Files.copy(getRequiredCapabilityContent(ccap), targetPath, REPLACE_EXISTING);
                String spec = "profile://" + profile.getVersion() + "/" + profile.getIdentity() + "/" + resItem.getSymbolicName() + "?version=" + resItem.getVersion();
                if (ccaps.size() > 1) {
                    spec += "&cntindex=" + i;
                }
                contentURL = new URL(null, spec, new ProfileURLStreamHandler(targetPath.toFile()));
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
            Map<String, Object> atts = new HashMap<>(ccap.getAttributes());
            atts.remove(ContentNamespace.CAPABILITY_STREAM_ATTRIBUTE);
            atts.put(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, contentURL);
            return atts;
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
}
