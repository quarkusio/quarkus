package io.quarkus.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.Artifact;
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

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.CuratedApplicationCreator;
import io.quarkus.creator.phase.generateconfig.GenerateConfigTask;

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

        final Artifact projectArtifact = project.getArtifact();
        final AppArtifact appArtifact = new AppArtifact(projectArtifact.getGroupId(), projectArtifact.getArtifactId(),
                projectArtifact.getClassifier(), "pom",
                projectArtifact.getVersion());
        final AppModel appModel;
        final BootstrapAppModelResolver modelResolver;
        try {
            LocalProject localProject = LocalProject.load(project.getBasedir().toPath());
            modelResolver = new BootstrapAppModelResolver(
                    MavenArtifactResolver.builder()
                            .setRepositorySystem(repoSystem)
                            .setRepositorySystemSession(repoSession)
                            .setRemoteRepositories(repos)
                            .setWorkspace(localProject.getWorkspace())
                            .build());
            appModel = modelResolver.resolveModel(appArtifact);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve application model " + appArtifact + " dependencies", e);
        }
        if (project.getResources().isEmpty()) {
            throw new MojoExecutionException("No resources directory, cannot create application.properties");
        }

        Resource res = project.getResources().get(0);
        File target = new File(res.getDirectory());

        String name = file;
        if (name == null || name.isEmpty()) {
            name = "application.properties.example";
        }

        try (CuratedApplicationCreator appCreationContext = CuratedApplicationCreator.builder()
                // configure the build phases we want the app to go through
                .setWorkDir(buildDir.toPath())
                .setModelResolver(modelResolver)
                .setAppArtifact(appModel.getAppArtifact())
                .build()) {

            appCreationContext.runTask(new GenerateConfigTask(new File(target, name).toPath()));
            getLog().info("Generated config file " + name);
        } catch (AppCreatorException e) {
            throw new MojoExecutionException("Failed to generate config file", e);
        }
    }
}
