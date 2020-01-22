package io.quarkus.maven;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.runner.bootstrap.GenerateConfigTask;

/**
 * Generates an example application-config.properties, with all properties commented out
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

    public GenerateConfigMojo() {
        MojoLogger.logSupplier = this::getLog;
    }

    @Override
    public void execute() throws MojoExecutionException {

        if (project.getPackaging().equals("pom")) {
            getLog().info("Type of the artifact is POM, skipping generate-config goal");
            return;
        }
        if (project.getResources().isEmpty()) {
            throw new MojoExecutionException("No resources directory, cannot create application.properties");
        }

        try {
            MavenArtifactResolver resolver = MavenArtifactResolver.builder()
                    .setRepositorySystem(repoSystem)
                    .setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos)
                    .build();

            try (CuratedApplication curatedApplication = QuarkusBootstrap
                    .builder(Paths.get(project.getBuild().getOutputDirectory()))
                    .setMavenArtifactResolver(resolver)
                    .setProjectRoot(project.getBasedir().toPath())
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
                GenerateConfigTask generateConfigTask = new GenerateConfigTask(configFile);
                generateConfigTask.run(curatedApplication);

            } catch (Exception e) {
                throw new MojoExecutionException("Failed to generate config file", e);
            }
        } catch (AppModelResolverException e) {
            throw new RuntimeException(e);
        }
    }
}
