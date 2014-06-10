package io.fabric8.git.internal;

import io.fabric8.git.GitListener;
import io.fabric8.git.GitService;
import io.fabric8.spi.RuntimeService;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.ValidatingReference;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.RepositoryCache;

@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(GitService.class)
public final class FabricGitServiceImpl extends AbstractComponent implements GitService {

    public static final Path DEFAULT_GIT_PATH = Paths.get("git", "local", "fabric");

    @Reference(referenceInterface = RuntimeService.class)
    private final ValidatingReference<RuntimeService> runtimeService = new ValidatingReference<RuntimeService>();

    private final List<GitListener> listeners = new CopyOnWriteArrayList<GitListener>();
    private File localRepo;
    private String remoteUrl;
    private Git git;

    @Activate
    void activate() throws IOException {
        localRepo = runtimeService.get().getDataPath().resolve(DEFAULT_GIT_PATH).toFile();
        if (!localRepo.exists() && !localRepo.mkdirs()) {
            throw new IOException("Failed to create local repository");
        }

        git = openOrInit(localRepo);

        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        RepositoryCache.clear();
    }


    private Git openOrInit(File repo) throws IOException {
        try {
            return Git.open(repo);
        } catch (RepositoryNotFoundException e) {
            try {
                Git git = Git.init().setDirectory(repo).call();
                git.commit().setMessage("First Commit").setCommitter("fabric", "user@fabric").call();
                return git;
            } catch (GitAPIException ex) {
                throw new IOException(ex);
            }
        }
    }

    @Override
    public Git get() throws IOException {
        assertValid();
        return git;
    }

    @Override
    public String getRemoteUrl() {
        assertValid();
        return remoteUrl;
    }


    @Override
    public void notifyRemoteChanged(String remoteUrl) {
        this.remoteUrl = remoteUrl;
        for (GitListener listener : listeners) {
            listener.onRemoteUrlChanged(remoteUrl);
        }
    }

    @Override
    public void notifyReceivePacket() {
        for (GitListener listener : listeners) {
            listener.onReceivePack();
        }
    }

    @Override
    public void addGitListener(GitListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeGitListener(GitListener listener) {
        listeners.remove(listener);
    }

    void bindRuntimeService(RuntimeService service) {
        this.runtimeService.bind(service);
    }
    void unbindRuntimeService(RuntimeService service) {
        this.runtimeService.unbind(service);
    }
}
