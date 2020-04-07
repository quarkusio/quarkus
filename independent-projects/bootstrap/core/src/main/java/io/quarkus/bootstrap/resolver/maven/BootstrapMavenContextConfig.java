package io.quarkus.bootstrap.resolver.maven;

import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import java.io.File;
import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public class BootstrapMavenContextConfig<T extends BootstrapMavenContextConfig<?>> {

    protected String localRepo;
    protected Boolean offline;
    protected LocalWorkspace workspace;
    protected boolean workspaceDiscovery = true;
    protected RepositorySystem repoSystem;
    protected RepositorySystemSession repoSession;
    protected List<RemoteRepository> remoteRepos;
    protected String alternativePomName;
    protected File userSettings;
    protected boolean artifactTransferLogging = true;

    /**
     * Local repository location
     * 
     * @param localRepo local repository location
     * @return this instance
     */
    @SuppressWarnings("unchecked")
    public T setLocalRepository(String localRepo) {
        this.localRepo = localRepo;
        return (T) this;
    }

    /**
     * Whether to operate offline
     * 
     * @param offline whether to operate offline
     * @return
     */
    @SuppressWarnings("unchecked")
    public T setOffline(boolean offline) {
        this.offline = offline;
        return (T) this;
    }

    /**
     * Workspace in the context of which this configuration is being initialized
     * 
     * @param workspace current workspace
     * @return
     */
    @SuppressWarnings("unchecked")
    public T setWorkspace(LocalWorkspace workspace) {
        this.workspace = workspace;
        return (T) this;
    }

    /**
     * Enables or disables workspace discovery.
     * By default, workspace discovery is enabled, meaning that when
     * the resolver is created in the context of a Maven project, the Maven's project
     * POM configuration will be picked up by the resolver and all the local projects
     * belonging to the workspace will be resolved at their original locations instead of
     * the actually artifacts installed in the repository.
     * Note, that if {@link #workspace} is provided, this setting will be ignored.
     *
     * @param workspaceDiscovery enables or disables workspace discovery
     * @return this instance of the builder
     */
    @SuppressWarnings("unchecked")
    public T setWorkspaceDiscovery(boolean workspaceDiscovery) {
        this.workspaceDiscovery = workspaceDiscovery;
        return (T) this;
    }

    /**
     * RepositorySystem that should be used by the resolver
     * 
     * @param repoSystem
     * @return
     */
    @SuppressWarnings("unchecked")
    public T setRepositorySystem(RepositorySystem repoSystem) {
        this.repoSystem = repoSystem;
        return (T) this;
    }

    /**
     * RepositorySystemSession that should be used by the resolver
     * 
     * @param repoSystem
     * @return
     */
    @SuppressWarnings("unchecked")
    public T setRepositorySystemSession(RepositorySystemSession repoSession) {
        this.repoSession = repoSession;
        return (T) this;
    }

    /**
     * Remote repositories that should be used by the resolver
     * 
     * @param repoSystem
     * @return
     */
    @SuppressWarnings("unchecked")
    public T setRemoteRepositories(List<RemoteRepository> remoteRepos) {
        this.remoteRepos = remoteRepos;
        return (T) this;
    }

    /**
     * The meaning of this option is equivalent to alternative POM in Maven,
     * which can be specified with command line argument '-f'.
     *
     * @param currentProject
     * @return
     */
    @SuppressWarnings("unchecked")
    public T setCurrentProject(String currentProject) {
        this.alternativePomName = currentProject;
        return (T) this;
    }

    /**
     * User Maven settings file location
     * 
     * @param userSettings
     * @return
     */
    @SuppressWarnings("unchecked")
    public T setUserSettings(File userSettings) {
        this.userSettings = userSettings;
        return (T) this;
    }

    /**
     * Whether to enable progress logging of artifact transfers.
     * The default value is true.
     *
     * @param artifactTransferLogging
     * @return
     */
    @SuppressWarnings("unchecked")
    public T setArtifactTransferLogging(boolean artifactTransferLogging) {
        this.artifactTransferLogging = artifactTransferLogging;
        return (T) this;
    }
}
