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
import io.fabric8.api.ProfileIdentity;
import io.fabric8.api.ProfileVersionBuilder;
import io.fabric8.api.ResourceItem;
import io.fabric8.api.VersionIdentity;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.DeleteBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jboss.gravia.repository.RepositoryWriter.ContentHandler;
import org.jboss.gravia.repository.spi.AbstractContentHandler;
import org.jboss.gravia.resource.Capability;
import org.jboss.gravia.resource.ContentCapability;
import org.jboss.gravia.resource.ContentNamespace;
import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.IOUtils;
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

    private static String PROFILES_METADATA_FILE = "profiles.xml";

    private final ProfileVersionCache profileVersionCache = new ProfileVersionCache();
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private GitRepository repository;
    private Path workspace;

    @Reference(referenceInterface = RuntimeService.class)
    private final ValidatingReference<RuntimeService> runtimeService = new ValidatingReference<>();

    @Activate
    void activate() throws IOException {
        activateInternal();
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        dactivateInternal();
        deactivateComponent();
    }

    private void activateInternal() throws IOException {
        Path dataPath = runtimeService.get().getDataPath();
        workspace = dataPath.resolve("profiles");
        repository = new GitRepository(workspace);

        // Add the default profile version
        if (!repository.hasBranch(DEFAULT_PROFILE_VERSION.getVersion())) {
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

    // [TODO] Consider support for locking individual profile versions
    LockHandle aquireWriteLock(VersionIdentity version) {
        final WriteLock writeLock = readWriteLock.writeLock();
        boolean success;
        try {
            success = writeLock.tryLock() || writeLock.tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            success = false;
        }
        IllegalStateAssertion.assertTrue(success, "Cannot obtain profile write lock in time for: " + version);
        return new LockHandle() {
            @Override
            public void unlock() {
                writeLock.unlock();
            }
        };
    }

    // [TODO] Consider support for locking individual profile versions
    LockHandle aquireReadLock(VersionIdentity version) {
        final ReadLock readLock = readWriteLock.readLock();
        boolean success;
        try {
            success = readLock.tryLock() || readLock.tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            success = false;
        }
        IllegalStateAssertion.assertTrue(success, "Cannot obtain profile read lock in time");
        return new LockHandle() {
            @Override
            public void unlock() {
                readLock.unlock();
            }
        };
    }

    Set<VersionIdentity> getVersions() {
        assertValid();
        final Set<VersionIdentity> versions = new HashSet<>();
        for (String branch : repository.listBranches()) {
            if (!branch.equals("master")) {
                versions.add(VersionIdentity.createFrom(branch));
            }
        }
        return Collections.unmodifiableSet(versions);
    }

    LinkedProfileVersion getProfileVersion(VersionIdentity version) {
        assertValid();
        LockHandle readLock = aquireReadLock(version);
        try {
            return getProfileVersionInternal(version);
        } finally {
            readLock.unlock();
        }
    }

    LinkedProfileVersion getRequiredProfileVersion(VersionIdentity version) {
        LinkedProfileVersion profileVersion = getProfileVersion(version);
        IllegalStateAssertion.assertNotNull(profileVersion, "Cannot obtain profile version: " + version);
        return profileVersion;
    }

    private LinkedProfileVersion getProfileVersionInternal(VersionIdentity version) {
        boolean hasBranch = repository.hasBranch(version.getVersion());
        return hasBranch ? profileVersionCache.getProfileVersion(version) : null;
    }

    LinkedProfileVersion addProfileVersion(LinkedProfileVersion profileVersion) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(profileVersion.getIdentity());
        try {
            return addProfileVersionInternal(profileVersion);
        } finally {
            writeLock.unlock();
        }
    }

    private LinkedProfileVersion addProfileVersionInternal(LinkedProfileVersion profileVersion) {
        VersionIdentity version = profileVersion.getIdentity();
        IllegalStateAssertion.assertFalse(repository.hasBranch(version.getVersion()), "Profile version already exists: " + version);
        String message = String.format("Add profile version: %s", profileVersion);
        LOGGER.info(message);
        return writeProfileVersion(profileVersion, message);
    }

    LinkedProfileVersion removeProfileVersion(VersionIdentity version) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            LinkedProfileVersion profileVersion = getRequiredProfileVersion(version);
            String message = String.format("Remove profile version: {}", version);
            LOGGER.info(message);

            // git reset --hard
            repository.resetHard();

            // git checkout master
            repository.checkoutBranch(Version.emptyVersion, false);

            // git branch -D [version]
            repository.deleteBranch(version.getVersion());

            return profileVersion;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Get the profile for the given version and identity
     * @return null if the profile does not exist
     */
    Profile getProfile(VersionIdentity version, ProfileIdentity identity) {
        assertValid();
        LockHandle readLock = aquireReadLock(version);
        try {
            Profile result = null;
            LinkedProfileVersion linkedVersion = getRequiredProfileVersion(version);
            for (Profile profile : linkedVersion.getLinkedProfiles().values()) {
                if (profile.getIdentity().equals(identity)) {
                    result = profile;
                    break;
                }
            }
            return result;
        } finally {
            readLock.unlock();
        }
    }

    Profile getRequiredProfile(VersionIdentity version, ProfileIdentity identity) {
        Profile profile = getProfile(version, identity);
        IllegalStateAssertion.assertNotNull(profile, "Cannot obtain profile '" + identity + "' from: " + version);
        return profile;
    }

    Profile addProfile(VersionIdentity version, Profile profile) {
        assertValid();
        ProfileIdentity identity = profile.getIdentity();
        VersionIdentity pversion = profile.getVersion();
        IllegalStateAssertion.assertTrue(pversion == null || version.equals(pversion), "Unexpected profile version: " + profile);
        LockHandle writeLock = aquireWriteLock(version);
        try {
            String message = String.format("Add profile to version: %s <= %s", version, profile);
            LOGGER.info(message);
            LinkedProfileVersion linkedVersion = getRequiredProfileVersion(version);
            IllegalStateAssertion.assertNull(linkedVersion.getLinkedProfile(identity), "Profile already exists in version: " + version);
            DefaultProfileVersionBuilder builder = new DefaultProfileVersionBuilder(linkedVersion);
            writeProfileVersion(builder.addProfile(profile).getProfileVersion(), message);
            return getProfile(version, identity);
        } finally {
            writeLock.unlock();
        }
    }

    Profile updateProfile(VersionIdentity version, Profile profile) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            String message = String.format("Update profile: %s", profile);
            LOGGER.info(message);
            ProfileIdentity identity = profile.getIdentity();
            LinkedProfileVersion linkedVersion = getProfileVersion(version);
            DefaultProfileVersionBuilder builder = new DefaultProfileVersionBuilder(linkedVersion);
            builder.removeProfile(identity);
            deleteProfilePath(version, identity);
            writeProfileVersion(builder.addProfile(profile).getProfileVersion(), message);
            return getProfile(version, identity);
        } finally {
            writeLock.unlock();
        }
    }

    Profile removeProfile(VersionIdentity version, ProfileIdentity identity) {
        assertValid();
        LockHandle writeLock = aquireWriteLock(version);
        try {
            String message = String.format("Remove profile from version: %s => %s", version, identity);
            Profile profile = getRequiredProfile(version, identity);
            LinkedProfileVersion linkedVersion = getProfileVersion(version);
            DefaultProfileVersionBuilder builder = new DefaultProfileVersionBuilder(linkedVersion);
            builder.removeProfile(identity);
            deleteProfilePath(version, identity);
            writeProfileVersion(builder.getProfileVersion(), message);
            return profile;
        } finally {
            writeLock.unlock();
        }
    }

    URLConnection getProfileURLConnection(URL url) throws IOException {
        assertValid();
        String path = url.getPath();
        String verstr = url.getHost();
        String prfstr = path.substring(1, path.lastIndexOf('/'));
        String item = path.substring(path.lastIndexOf('/') + 1);
        VersionIdentity version = VersionIdentity.createFrom(verstr);
        ProfileIdentity profile = ProfileIdentity.createFrom(prfstr);
        LockHandle readLock = aquireReadLock(version);
        try {
            Path profilePath = getRequiredProfilePath(version, profile);
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
                for (String resver : itemPath.toFile().list()) {
                    Version nextver = Version.parseVersion(resver);
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

    private LinkedProfileVersion writeProfileVersion(LinkedProfileVersion profileVersion, String message) {

        VersionIdentity version = profileVersion.getIdentity();
        profileVersionCache.invalidate(version);

        repository.writeProfileVersion(profileVersion, message);

        return getProfileVersionInternal(version);
    }

    private Path getProfilePath(VersionIdentity version, ProfileIdentity identity) {
        return workspace.resolve(identity.getSymbolicName());
    }

    private Path getRequiredProfilePath(VersionIdentity version, ProfileIdentity identity) {
        Path profilePath = getProfilePath(version, identity);
        IllegalStateAssertion.assertTrue(profilePath.toFile().exists(), "Cannot find profile directory: " + profilePath);
        return profilePath;
    }

    private void deleteProfilePath(VersionIdentity version, ProfileIdentity identity) {
        Path profilePath = getProfilePath(version, identity);
        try {
            FileUtils.deleteRecursively(profilePath);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot delete profile path: " + profilePath);
        }
    }

    void bindRuntimeService(RuntimeService service) {
        runtimeService.bind(service);
    }
    void unbindRuntimeService(RuntimeService service) {
        runtimeService.unbind(service);
    }

    private class ProfileVersionCache {

        private final Map<VersionIdentity, LinkedProfileVersion> cacheMap = new HashMap<>();

        LinkedProfileVersion getProfileVersion(VersionIdentity version) {
            synchronized (cacheMap) {
                LinkedProfileVersion linkedVersion = cacheMap.get(version);
                if (linkedVersion == null) {
                    linkedVersion = repository.getProfileVersion(version);
                    cacheMap.put(version, linkedVersion);
                }
                return linkedVersion;
            }
        }

        void invalidate(VersionIdentity version) {
            synchronized (cacheMap) {
                cacheMap.remove(version);
            }
        }
    }

    private static class ResourceItemContentHandler extends AbstractContentHandler {

        private final Path versionPath;

        ResourceItemContentHandler(Path versionPath) {
            this.versionPath = versionPath;
        }

        @Override
        public Map<String, Object> process(ContentCapability ccap) throws IOException {
            Profile profile = getContextItem(Profile.class);
            ResourceItem resItem = getContextItem(ResourceItem.class);
            Path profilePath = versionPath.resolve(profile.getIdentity().getSymbolicName());
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

    private static class GitRepository {

        private final Path workspace;
        private final Git git;

        GitRepository(Path workspace) throws IOException {
            this.git = openOrInitWorkspace(workspace);
            this.workspace = workspace;

        }

        LinkedProfileVersion getProfileVersion(VersionIdentity version) {

            // git reset --hard
            resetHard();

            // git checkout [version]
            checkoutBranch(version.getVersion(), false);

            ProfileVersionBuilder builder = new DefaultProfileVersionBuilder(version);
            FileInputStream fis = null;
            try {
                File metadataFile = workspace.resolve(PROFILES_METADATA_FILE).toFile();
                fis = new FileInputStream(metadataFile);
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

            return builder.getProfileVersion();
        }

        void writeProfileVersion(LinkedProfileVersion profileVersion, String message) {

            // git reset --hard
            resetHard();

            // git checkout -b [version]
            Version version = profileVersion.getIdentity().getVersion();
            checkoutBranch(version, true);

            FileOutputStream fos = null;
            try {
                File metadataFile = workspace.resolve(PROFILES_METADATA_FILE).toFile();
                fos = new FileOutputStream(metadataFile);
                ContentHandler contentHandler = new ResourceItemContentHandler(workspace);
                DefaultProfileXMLWriter writer = new DefaultProfileXMLWriter(fos, contentHandler);
                writer.writeProfileVersion(profileVersion);
                writer.close();
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot add profile version: " + profileVersion, ex);
            } finally {
                IOUtils.safeClose(fos);
            }

            // git add --all
            addAll();

            // git commit
            commit(message);
        }

        DirCache addAll() {
            try {
                AddCommand addCmd = git.add().addFilepattern(".");
                return addCmd.call();
            } catch (GitAPIException ex) {
                throw new IllegalStateException("Cannot add files", ex);
            }
        }

        Ref checkoutBranch(Version version, boolean allowCreate) {
            String branch = Version.emptyVersion != version ? version.toString() : "master";
            try {
                if (!allowCreate || hasBranch(version)) {
                    CheckoutCommand checkoutCmd = git.checkout().setName(branch);
                    return checkoutCmd.call();
                } else {
                    CheckoutCommand checkoutCmd = git.checkout().setCreateBranch(true).setName(branch);
                    return checkoutCmd.call();
                }
            } catch (GitAPIException ex) {
                throw new IllegalStateException("Cannot checkout branch: " + version, ex);
            }
        }

        RevCommit commit(String message) {
            try {
                CommitCommand commitCmd = git.commit().setAll(true).setMessage(message).setCommitter(getDefaultCommiter());
                return commitCmd.call();
            } catch (GitAPIException ex) {
                throw new IllegalStateException("Cannot commit: " + message, ex);
            }
        }

        List<String> deleteBranch(Version version) {
            String branch = version.toString();
            try {
                DeleteBranchCommand deleteCmd = git.branchDelete().setBranchNames(branch).setForce(true);
                return deleteCmd.call();
            } catch (GitAPIException ex) {
                throw new IllegalStateException("Cannot checkout branch: " + version, ex);
            }
        }

        boolean hasBranch(Version version) {
            String branch = version.toString();
            return listBranches().contains(branch);
        }

        Set<String> listBranches() {
            Set<String> branches = new LinkedHashSet<>();
            try {
                for (Ref ref : git.branchList().call()) {
                    String name = ref.getName();
                    if (name.startsWith("refs/heads/")) {
                        name = name.substring(11);
                        branches.add(name);
                    }
                }
            } catch (GitAPIException ex) {
                throw new IllegalStateException("Cannot list branches", ex);
            }
            return branches;
        }

        Ref resetHard() {
            try {
                ResetCommand resetCmd = git.reset().setMode(ResetType.HARD);
                return resetCmd.call();
            } catch (GitAPIException ex) {
                throw new IllegalStateException("Cannot reset workspace", ex);
            }
        }

        private Git openOrInitWorkspace(Path workspace) throws IOException {
            try {
                return Git.open(workspace.toFile());
            } catch (RepositoryNotFoundException e) {
                try {
                    Git git = Git.init().setDirectory(workspace.toFile()).call();
                    git.commit().setMessage("First Commit").setCommitter(getDefaultCommiter()).call();
                    return git;
                } catch (GitAPIException ex) {
                    throw new IOException(ex);
                }
            }
        }

        private PersonIdent getDefaultCommiter() {
            return new PersonIdent("fabric", "user@fabric");
        }
    }
}
