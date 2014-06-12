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
import io.fabric8.api.LockHandle;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ResourceItem;
import io.fabric8.git.GitService;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

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
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The internal profile registry
 *
 * @author thomas.diesler@jboss.com
 * @since 07-May-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ProfileRegistry.class)
public final class ProfileRegistry extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileRegistry.class);

    private static String PROFILES_FILE = "profiles.xml";

    private final Map<Version, ReentrantReadWriteLock> versionLocks = new ConcurrentHashMap<>();
    private final RegistryCache registryCache = new RegistryCache();
    private Path workspace;

    @Reference(referenceInterface = GitService.class)
    private final ValidatingReference<GitService> gitService = new ValidatingReference<>();
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

    LockHandle aquireWriteLock(Version version) {

        final WriteLock writeLock = getReadWriteLock(version).writeLock();

        boolean success;
        try {
            success = writeLock.tryLock() || writeLock.tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            success = false;
        }
        IllegalStateAssertion.assertTrue(success, "Cannot obtain write lock in time for: " + version);

        return new LockHandle() {
            @Override
            public void unlock() {
                writeLock.unlock();
            }
        };
    }

    LockHandle aquireReadLock(Version version) {

        final ReadLock readLock = getReadWriteLock(version).readLock();

        boolean success;
        try {
            success = readLock.tryLock() || readLock.tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            success = false;
        }
        IllegalStateAssertion.assertTrue(success, "Cannot obtain read lock in time for: " + version);

        return new LockHandle() {
            @Override
            public void unlock() {
                readLock.unlock();
            }
        };
    }

    private ReentrantReadWriteLock getReadWriteLock(Version version) {
        IllegalArgumentAssertion.assertNotNull(version, "version");
        ReentrantReadWriteLock readWriteLock;
        synchronized (versionLocks) {
            readWriteLock = versionLocks.get(version);
            if (readWriteLock == null) {
                readWriteLock = new ReentrantReadWriteLock();
                versionLocks.put(version, readWriteLock);
            }
        }
        return readWriteLock;
    }

    Set<Version> getVersions() {
        assertValid();
        Set<Version> versions = new HashSet<>();
        synchronized (workspace) {
            for (String name : workspace.toFile().list()) {
                if (workspace.resolve(name).toFile().isDirectory()) {
                    versions.add(Version.parseVersion(name));
                }
            }
        }
        return Collections.unmodifiableSet(versions);
    }

    LinkedProfileVersion getProfileVersion(Version version) {
        assertValid();
        LockHandle readLock = aquireReadLock(version);
        try {
            return getProfileVersionInternal(version);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Get the {@link LinkedProfileVersion} for the given version
     * @return null if the version does not exist
     */
    private LinkedProfileVersion getProfileVersionInternal(Version version) {
        File profilesFile = getProfilesFile(version);
        return profilesFile.exists() ? registryCache.getProfileVersion(version) : null;
    }

    LinkedProfileVersion addProfileVersion(LinkedProfileVersion profileVersion) {
        assertValid();
        Version version = profileVersion.getIdentity();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            return addProfileVersionInternal(profileVersion);
        } finally {
            writeLock.unlock();
        }
    }

    private LinkedProfileVersion addProfileVersionInternal(LinkedProfileVersion profileVersion) {
        Version version = profileVersion.getIdentity();
        LOGGER.info("Add profile version: {}", version);
        Path versionPath = getProfileVersionPath(version);
        IllegalStateAssertion.assertFalse(versionPath.toFile().exists(), "Profile version path already exists: " + versionPath);
        IllegalStateAssertion.assertTrue(versionPath.toFile().mkdirs(), "Cannot create profile version directory: " + versionPath);
        return writeProfileVersion(profileVersion);
    }

    LinkedProfileVersion removeProfileVersion(Version version) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            LOGGER.info("Remove profile version: {}", version);
            LinkedProfileVersion profileVersion = getProfileVersion(version);
            deleteProfileVersionPath(version);
            return profileVersion;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Get the profile for the given version and identity
     * @return null if the profile does not exist
     */
    Profile getProfile(Version version, String identity) {
        assertValid();
        Profile result = null;
        LockHandle readLock = aquireReadLock(version);
        try {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(getRequiredProfilesFile(version));
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
        } finally {
            readLock.unlock();
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
        String identity = profile.getIdentity();
        Version pversion = profile.getVersion();
        IllegalStateAssertion.assertTrue(pversion == null || version.equals(pversion), "Unexpected profile version: " + profile);
        LockHandle writeLock = aquireWriteLock(version);
        try {
            LOGGER.info("Add profile to version: {} <= {}", version, profile);
            LinkedProfileVersion linkedVersion = getRequiredProfileVersion(version);
            IllegalStateAssertion.assertNull(linkedVersion.getLinkedProfile(identity), "Profile already exists in version: " + version);
            DefaultProfileVersionBuilder builder = new DefaultProfileVersionBuilder(linkedVersion);
            writeProfileVersion(builder.addProfile(profile).getProfileVersion());
            return getProfile(version, identity);
        } finally {
            writeLock.unlock();
        }
    }

    Profile updateProfile(Version version, Profile profile) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            LOGGER.info("Update profile: {}", profile);
            String identity = profile.getIdentity();
            LinkedProfileVersion linkedVersion = getProfileVersion(version);
            DefaultProfileVersionBuilder builder = new DefaultProfileVersionBuilder(linkedVersion);
            builder.removeProfile(identity);
            deleteProfilePath(version, identity);
            writeProfileVersion(builder.addProfile(profile).getProfileVersion());
            return getProfile(version, identity);
        } finally {
            writeLock.unlock();
        }
    }

    Profile removeProfile(Version version, String identity) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            LOGGER.info("Remove profile from version: {} => {}", version, identity);
            Profile profile = getRequiredProfile(version, identity);
            LinkedProfileVersion linkedVersion = getProfileVersion(version);
            DefaultProfileVersionBuilder builder = new DefaultProfileVersionBuilder(linkedVersion);
            builder.removeProfile(identity);
            deleteProfilePath(version, identity);
            writeProfileVersion(builder.getProfileVersion());
            return profile;
        } finally {
            writeLock.unlock();
        }
    }

    URLConnection getProfileURLConnection(URL url) throws IOException {
        assertValid();
        String path = url.getPath();
        String version = url.getHost();
        String profile = path.substring(1, path.lastIndexOf('/'));
        String item = path.substring(path.lastIndexOf('/') + 1);
        LockHandle readLock = aquireReadLock(Version.parseVersion(version));
        try {
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
        } finally {
            readLock.unlock();
        }
    }

    private LinkedProfileVersion writeProfileVersion(LinkedProfileVersion profileVersion) {
        Version version = profileVersion.getIdentity();
        registryCache.invalidate(version);
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

    LinkedProfileVersion getRequiredProfileVersion(Version version) {
        LinkedProfileVersion profileVersion = getProfileVersion(version);
        IllegalStateAssertion.assertNotNull(profileVersion, "Cannot obtain profile version: " + version);
        return profileVersion;
    }

    void bindGitService(GitService service) {
        gitService.bind(service);
    }
    void unbindGitService(GitService service) {
        gitService.bind(service);
    }

    void bindRuntimeService(RuntimeService service) {
        runtimeService.bind(service);
    }
    void unbindRuntimeService(RuntimeService service) {
        runtimeService.unbind(service);
    }

    private class RegistryCache {

        private final Map<Version, LinkedProfileVersion> cacheMap = new HashMap<>();

        LinkedProfileVersion getProfileVersion(Version version) {
            LinkedProfileVersion linkedVersion = cacheMap.get(version);
            if (linkedVersion == null) {
                ProfileVersionBuilder builder = new DefaultProfileVersionBuilder(version);
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(getRequiredProfilesFile(version));
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
                linkedVersion = builder.getProfileVersion();
                cacheMap.put(version, linkedVersion);
            }
            return linkedVersion;
        }

        void invalidate(Version version) {
            cacheMap.remove(version);
        }
    }

    private class ResourceItemContentHandler extends AbstractContentHandler {

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
