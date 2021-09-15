package io.quarkus.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.dependency.ResolvedArtifactDependency;
import io.quarkus.runner.bootstrap.GenerateConfigTask;

/**
 * Generates an example application.properties, with all properties commented out.
 *
 * If this is already present then it will be appended too, although only properties that were not already present
 *
 * @author Stuart Douglas
 */
@Mojo(name = "generate-config", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GenerateConfigMojo extends AbstractMojo {

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    @Component
    private RepositorySystem repoSystem;

    @Component
    private RemoteRepositoryManager remoteRepoManager;

    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of artifacts and their dependencies.
     *
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    /**
     * The project's remote repositories to use for the resolution of plugins and their dependencies.
     *
     * @parameter default-value="${project.remotePluginRepositories}"
     * @readonly
     */
    @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true, required = true)
    private List<RemoteRepository> pluginRepos;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;

    @Parameter(defaultValue = "${file}")
    private String file;

    @Override
    public void execute() throws MojoExecutionException {

        if (project.getPackaging().equals("pom")) {
            getLog().info("Type of the artifact is POM, skipping generate-config goal");
            return;
        }
        if (project.getResources().isEmpty()) {
            throw new MojoExecutionException("No resources directory, cannot create application.properties");
        }

        // Here we are creating the output dir if it does not exist just to be able to resolve
        // the root app artifact, otherwise the project has to be compiled at least
        final Path classesDir = Paths.get(project.getBuild().getOutputDirectory());
        if (!Files.exists(classesDir)) {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Creating empty " + classesDir + " just to be able to resolve the project's artifact");
            }
            try {
                Files.createDirectories(classesDir);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to create " + classesDir);
            }
        }

        try {
            MavenArtifactResolver resolver = MavenArtifactResolver.builder()
                    .setRepositorySystem(repoSystem)
                    .setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos)
                    .setRemoteRepositoryManager(remoteRepoManager)
                    .build();

            final Artifact projectArtifact = project.getArtifact();
            final ResolvedArtifactDependency appArtifact = new ResolvedArtifactDependency(projectArtifact.getGroupId(),
                    projectArtifact.getArtifactId(),
                    projectArtifact.getClassifier(), projectArtifact.getArtifactHandler().getExtension(),
                    projectArtifact.getVersion(), classesDir);

            try (CuratedApplication curatedApplication = QuarkusBootstrap
                    .builder()
                    .setAppArtifact(appArtifact)
                    .setProjectRoot(project.getBasedir().toPath())
                    .setMavenArtifactResolver(resolver)
                    .setBaseClassLoader(getClass().getClassLoader())
                    .setBuildSystemProperties(project.getProperties())
                    .build().bootstrap()) {

                Resource res = project.getResources().get(0);
                File target = new File(res.getDirectory());

                String name = file;
                if (name == null || name.isEmpty()) {
                    name = "application.properties.example";
                }
                Path configFile = new File(target, name).toPath();
                curatedApplication.runInAugmentClassLoader(GenerateConfigTask.class.getName(),
                        Collections.singletonMap(GenerateConfigTask.CONFIG_FILE, configFile));
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate config file", e);
        }
    }

    @Override
    public void setLog(Log log) {
        super.setLog(log);
        MojoLogger.delegate = log;
    }
}
