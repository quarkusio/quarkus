package io.quarkus.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.runtime.LaunchMode;

/**
 * This goal downloads all the Maven artifact dependencies required to build, run, test and
 * launch the application dev mode.
 */
@Mojo(name = "go-offline")
public class GoOfflineMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Component
    private RepositorySystem repoSystem;

    @Component
    RemoteRepositoryManager remoteRepositoryManager;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        final Artifact pom = new DefaultArtifact(project.getArtifact().getGroupId(),
                project.getArtifactId(),
                ArtifactCoords.TYPE_POM,
                project.getVersion());

        final MavenArtifactResolver resolver = getResolver();
        final DependencyNode root;
        try {
            root = resolver.collectDependencies(pom, Collections.emptyList()).getRoot();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to collect dependencies of " + pom, e);
        }

        final List<Path> createdDirs = new ArrayList<>();
        try {
            ensureResolvableModule(root, resolver.getMavenContext().getWorkspace(), createdDirs);
            final ArtifactCoords appArtifact = new GACTV(pom.getGroupId(), pom.getArtifactId(), pom.getClassifier(),
                    pom.getExtension(), pom.getVersion());
            resolveAppModel(resolver, appArtifact, LaunchMode.NORMAL);
            resolveAppModel(resolver, appArtifact, LaunchMode.DEVELOPMENT);
            resolveAppModel(resolver, appArtifact, LaunchMode.TEST);
        } finally {
            for (Path d : createdDirs) {
                IoUtils.recursiveDelete(d);
            }
        }
    }

    private void resolveAppModel(final MavenArtifactResolver resolver, final ArtifactCoords appArtifact, LaunchMode mode)
            throws MojoExecutionException {
        final BootstrapAppModelResolver appModelResolver = new BootstrapAppModelResolver(resolver);
        if (mode == LaunchMode.DEVELOPMENT) {
            appModelResolver.setDevMode(true);
        } else if (mode == LaunchMode.TEST) {
            appModelResolver.setTest(true);
        }
        try {
            appModelResolver.resolveModel(appArtifact);
        } catch (AppModelResolverException e) {
            throw new MojoExecutionException("Failed to resolve Quarkus application model for " + project.getArtifact(), e);
        }
    }

    private MavenArtifactResolver getResolver() throws MojoExecutionException {
        try {
            return MavenArtifactResolver.builder()
                    .setRemoteRepositoryManager(remoteRepositoryManager)
                    .setRemoteRepositories(repos)
                    .setPreferPomsFromWorkspace(true)
                    .build();
        } catch (BootstrapMavenException e) {
            throw new MojoExecutionException("Failed to initialize Maven artifact resolver", e);
        }
    }

    private static void ensureResolvableModule(DependencyNode node, LocalWorkspace workspace, List<Path> createdDirs)
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
}
