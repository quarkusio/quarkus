package io.quarkus.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.maven.components.QuarkusWorkspaceProvider;
import io.quarkus.maven.dependency.ArtifactCoords;

/**
 * This goal downloads all the Maven artifact dependencies required to build, run, test and
 * launch the application dev mode.
 */
@Mojo(name = "go-offline", threadSafe = true)
public class GoOfflineMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    MavenSession session;

    @Component
    RepositorySystem repoSystem;

    @Component
    RemoteRepositoryManager remoteRepositoryManager;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    List<RemoteRepository> repos;

    @Component
    QuarkusWorkspaceProvider workspaceProvider;

    /**
     * Target launch mode corresponding to {@link io.quarkus.runtime.LaunchMode} for which the dependencies should be resolved.
     * {@code io.quarkus.runtime.LaunchMode.TEST} is the default, since it includes both {@code provided} and {@code test}
     * dependency scopes.
     */
    @Parameter(property = "mode", required = false, defaultValue = "test")
    String mode;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        final Artifact pom = new DefaultArtifact(project.getArtifact().getGroupId(),
                project.getArtifactId(),
                ArtifactCoords.TYPE_POM,
                project.getVersion());

        final MavenArtifactResolver resolver = getResolver();
        final BootstrapAppModelResolver appModelResolver = new BootstrapAppModelResolver(resolver);

        final Set<String> excludedScopes;
        if (mode.equalsIgnoreCase("test")) {
            appModelResolver.setTest(true);
            excludedScopes = Set.of();
        } else if (mode.equalsIgnoreCase("dev") || mode.equalsIgnoreCase("development")) {
            appModelResolver.setDevMode(true);
            excludedScopes = Set.of("test");
        } else if (mode.equalsIgnoreCase("prod") || mode.isEmpty()) {
            excludedScopes = Set.of("test", "provided");
        } else {
            throw new IllegalArgumentException(
                    "Unrecognized mode '" + mode + "', supported values are test, dev, development, prod");
        }

        final DependencyNode root;
        try {
            root = resolver.getSystem().collectDependencies(
                    resolver.getSession(),
                    resolver.newCollectManagedRequest(pom, List.of(), List.of(), List.of(), List.of(), excludedScopes))
                    .getRoot();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to collect dependencies of " + pom, e);
        }

        final LocalWorkspace workspace = resolver.getMavenContext().getWorkspace();
        final List<Path> createdDirs = new ArrayList<>(workspace.getProjects().size());
        try {
            ensureResolvableModule(root, workspace, createdDirs);
            appModelResolver.resolveModel(ArtifactCoords.of(pom.getGroupId(), pom.getArtifactId(), pom.getClassifier(),
                    pom.getExtension(), pom.getVersion()));
        } catch (AppModelResolverException e) {
            throw new MojoExecutionException("Failed to resolve Quarkus application model for " + project.getArtifact(), e);
        } finally {
            for (Path d : createdDirs) {
                IoUtils.recursiveDelete(d);
            }
        }
    }

    private MavenArtifactResolver getResolver() throws MojoExecutionException {
        return workspaceProvider.createArtifactResolver(BootstrapMavenContext.config()
                .setUserSettings(session.getRequest().getUserSettingsFile())
                .setCurrentProject(project.getBasedir().toString())
                .setRemoteRepositoryManager(remoteRepositoryManager)
                .setRemoteRepositories(repos)
                .setPreferPomsFromWorkspace(true));
    }

    private static void ensureResolvableModule(DependencyNode node, LocalWorkspace workspace, List<Path> createdDirs)
            throws MojoExecutionException {
        Artifact artifact = node.getArtifact();
        if (artifact != null) {
            final LocalProject module = workspace.getProject(artifact.getGroupId(), artifact.getArtifactId());
            if (module != null && !module.getRawModel().getPackaging().equals(ArtifactCoords.TYPE_POM)) {
                final Path classesDir;
                if (artifact.getClassifier().equals(ArtifactSources.TEST)) {
                    classesDir = module.getTestClassesDir();
                } else {
                    classesDir = module.getClassesDir();
                }
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
}
