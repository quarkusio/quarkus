package io.quarkus.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.cli.transfer.QuietMavenTransferListener;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyNode;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.maven.components.QuarkusWorkspaceProvider;
import io.quarkus.maven.dependency.ArtifactCoords;

public abstract class QuarkusProjectStateMojoBase extends QuarkusProjectMojoBase {

    @Component
    QuarkusWorkspaceProvider workspaceProvider;

    /**
     * If true, the information will be logged per each relevant module of the project
     * instead of an overall summary
     */
    @Parameter(property = "perModule", required = false)
    boolean perModule;

    @Override
    public void doExecute(QuarkusProject quarkusProject, MessageWriter log) throws MojoExecutionException {
        if (project.getFile() == null) {
            throw new MojoExecutionException("This goal requires a project");
        }

        if (!QuarkusProjectHelper.isRegistryClientEnabled()) {
            throw new MojoExecutionException("This goal requires a Quarkus extension registry client to be enabled");
        }

        final Collection<Path> createdDirs = ensureResolvable(new DefaultArtifact(project.getGroupId(),
                project.getArtifactId(), ArtifactCoords.TYPE_POM, project.getVersion()));
        try {
            processProjectState(quarkusProject);
        } finally {
            for (Path p : createdDirs) {
                IoUtils.recursiveDelete(p);
            }
        }
    }

    protected abstract void processProjectState(QuarkusProject quarkusProject) throws MojoExecutionException;

    protected ApplicationModel resolveApplicationModel() throws MojoExecutionException {
        try {
            return new BootstrapAppModelResolver(artifactResolver())
                    .resolveModel(ArtifactCoords.pom(project.getGroupId(), project.getArtifactId(), project.getVersion()));
        } catch (AppModelResolverException e) {
            throw new MojoExecutionException("Failed to resolve the Quarkus application model for project "
                    + ArtifactCoords.pom(project.getGroupId(), project.getArtifactId(), project.getVersion()), e);
        }
    }

    private Collection<Path> ensureResolvable(Artifact a) throws MojoExecutionException {
        final DependencyNode root;
        try {
            root = artifactResolver().collectDependencies(a, List.of()).getRoot();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to collect dependencies of " + a, e);
        }
        final List<Path> createdDirs = new ArrayList<>();
        ensureResolvableModule(root, artifactResolver().getMavenContext().getWorkspace(), createdDirs);
        return createdDirs;
    }

    private void ensureResolvableModule(DependencyNode node, LocalWorkspace workspace, List<Path> createdDirs)
            throws MojoExecutionException {
        Artifact artifact = node.getArtifact();
        if (artifact != null) {
            final LocalProject module = workspace.getProject(artifact.getGroupId(), artifact.getArtifactId());
            if (module != null && !module.getRawModel().getPackaging().equals(ArtifactCoords.TYPE_POM)) {
                final Path classesDir = module.getClassesDir();
                if (!Files.exists(classesDir)) {
                    Path topDirToCreate = classesDir;
                    while (!Files.exists(topDirToCreate.getParent())) {
                        topDirToCreate = topDirToCreate.getParent();
                    }
                    try {
                        Files.createDirectories(classesDir);
                        createdDirs.add(topDirToCreate);
                    } catch (IOException e) {
                        throw new MojoExecutionException("Failed to create " + classesDir, e);
                    }
                }
            }
        }
        for (DependencyNode c : node.getChildren()) {
            ensureResolvableModule(c, workspace, createdDirs);
        }
    }

    @Override
    protected MavenArtifactResolver catalogArtifactResolver() throws MojoExecutionException {
        if (getLog().isDebugEnabled()) {
            return artifactResolver();
        } else {
            try {
                final MavenArtifactResolver baseResolver = artifactResolver();
                final DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(
                        baseResolver.getSession());
                session.setTransferListener(new QuietMavenTransferListener());
                final BootstrapMavenContext ctx = new BootstrapMavenContext(BootstrapMavenContext.config()
                        .setRepositorySystem(baseResolver.getSystem())
                        .setRemoteRepositoryManager(baseResolver.getRemoteRepositoryManager())
                        .setRemoteRepositories(baseResolver.getRepositories())
                        .setWorkspaceDiscovery(false)
                        .setRepositorySystemSession(session));
                return new MavenArtifactResolver(ctx);
            } catch (BootstrapMavenException e) {
                throw new MojoExecutionException("Failed to initialize Maven artifact resolver", e);
            }
        }
    }

    @Override
    protected MavenArtifactResolver initArtifactResolver() throws MojoExecutionException {
        return workspaceProvider.createArtifactResolver(BootstrapMavenContext.config()
                .setUserSettings(session.getRequest().getUserSettingsFile())
                .setRemoteRepositoryManager(workspaceProvider.getRemoteRepositoryManager())
                // The system needs to be initialized with the bootstrap model builder to properly interpolate system properties set on the command line
                // e.g. -Dquarkus.platform.version=xxx
                //.setRepositorySystem(workspaceProvider.getRepositorySystem())
                // The session should be initialized with the loaded workspace
                //.setRepositorySystemSession(repoSession)
                .setRemoteRepositories(repos)
                // To support multi-module projects that haven't been installed
                .setPreferPomsFromWorkspace(true)
                // to support profiles
                .setEffectiveModelBuilder(true)
                // to initialize WorkspaceModule parents and BOM modules
                .setWorkspaceModuleParentHierarchy(true));
    }
}
