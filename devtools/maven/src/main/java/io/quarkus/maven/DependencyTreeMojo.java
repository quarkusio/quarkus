package io.quarkus.maven;

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.dependency.GACTV;

/**
 * Displays Quarkus application build dependency tree including the deployment ones.
 */
@Mojo(name = "dependency-tree", defaultPhase = LifecyclePhase.NONE, requiresDependencyResolution = ResolutionScope.NONE)
public class DependencyTreeMojo extends AbstractMojo {

    @Component
    protected QuarkusBootstrapProvider bootstrapProvider;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    /**
     * Target launch mode corresponding to {@link io.quarkus.runtime.LaunchMode} for which the dependency tree should be built.
     * {@code io.quarkus.runtime.LaunchMode.NORMAL} is the default.
     */
    @Parameter(property = "mode", required = false, defaultValue = "prod")
    String mode;

    protected MavenArtifactResolver resolver;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        String buf = "Quarkus application " + mode.toUpperCase() + " mode build dependency tree:";
        getLog().info(buf);

        final GACTV appArtifact = new GACTV(project.getGroupId(), project.getArtifactId(), null, "pom", project.getVersion());
        final BootstrapAppModelResolver modelResolver;
        try {
            modelResolver = new BootstrapAppModelResolver(resolver());
            if (mode != null) {
                if (mode.equalsIgnoreCase("test")) {
                    modelResolver.setTest(true);
                } else if (mode.equalsIgnoreCase("dev") || mode.equalsIgnoreCase("development")) {
                    modelResolver.setDevMode(true);
                } else if (mode.equalsIgnoreCase("prod") || mode.isEmpty()) {
                    // ignore, that's the default
                } else {
                    throw new MojoExecutionException(
                            "Parameter 'mode' was set to '" + mode + "' while expected one of 'dev', 'test' or 'prod'");
                }
            }
            modelResolver.setBuildTreeLogger(s -> getLog().info(s));
            modelResolver.resolveModel(appArtifact);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve application model " + appArtifact + " dependencies", e);
        }
    }

    protected MavenArtifactResolver resolver() throws BootstrapMavenException {
        return resolver == null
                ? resolver = MavenArtifactResolver.builder()
                        .setRemoteRepositoryManager(bootstrapProvider.remoteRepositoryManager())
                        // The system needs to be initialized with the bootstrap model builder to properly interpolate system properties set on the command line
                        // e.g. -Dquarkus.platform.version=xxx
                        //.setRepositorySystem(bootstrapProvider.repositorySystem())
                        // The session should be initialized with the loaded workspace
                        //.setRepositorySystemSession(repoSession)
                        .setRemoteRepositories(repos)
                        // To support multimodule projects that haven't been installed
                        .setPreferPomsFromWorkspace(true)
                        .build()
                : resolver;
    }

}
