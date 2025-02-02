package io.quarkus.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Consumer;

import org.apache.maven.execution.MavenSession;
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
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.DependencyLoggingConfig;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.components.QuarkusWorkspaceProvider;
import io.quarkus.maven.dependency.ArtifactCoords;

/**
 * Displays Quarkus application build dependency tree including the deployment ones.
 */
@Mojo(name = "dependency-tree", defaultPhase = LifecyclePhase.NONE, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
public class DependencyTreeMojo extends AbstractMojo {

    @Component
    protected QuarkusWorkspaceProvider workspaceProvider;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    /**
     * Target launch mode corresponding to {@link io.quarkus.runtime.LaunchMode} for which the dependency tree should be built.
     * {@code io.quarkus.runtime.LaunchMode.NORMAL} is the default.
     */
    @Parameter(property = "mode", defaultValue = "prod")
    String mode;

    /**
     * INCUBATING option, enabled with {@code -Dquarkus.bootstrap.incubating-model-resolver} system or project property.
     * <p>
     * Whether to log dependency properties, such as on which classpath they belong, whether they are hot-reloadable in dev
     * mode, etc.
     */
    @Parameter(property = "verbose")
    boolean verbose;

    /**
     * INCUBATING option, enabled with {@code -Dquarkus.bootstrap.incubating-model-resolver} system or project property.
     * <p>
     * Whether to log all dependencies of each dependency node in a tree, adding {@code [+]} suffix
     * to those whose dependencies are not expanded.
     */
    @Parameter(property = "graph")
    boolean graph;

    /**
     * If specified, this parameter will cause the dependency tree to be written to the path specified, instead of writing to
     * the console.
     */
    @Parameter(property = "outputFile", required = false)
    File outputFile;

    /**
     * Whether to append outputs into the output file or overwrite it.
     */
    @Parameter(property = "appendOutput", required = false, defaultValue = "false")
    boolean appendOutput;

    /**
     * Whether to log only the runtime dependencies of the Quarkus application
     */
    @Parameter(property = "runtimeOnly")
    boolean runtimeOnly;

    protected MavenArtifactResolver resolver;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        BufferedWriter writer = null;
        final Consumer<String> log;
        if (outputFile == null) {
            log = s -> getLog().info(s);
        } else {
            final BufferedWriter bw;
            try {
                Files.createDirectories(outputFile.toPath().getParent());
                final OpenOption[] openOptions = appendOutput && outputFile.exists()
                        ? new OpenOption[] { StandardOpenOption.APPEND }
                        : new OpenOption[0];
                bw = writer = Files.newBufferedWriter(outputFile.toPath(), openOptions);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to initialize file output writer", e);
            }
            log = s -> {
                try {
                    bw.write(s);
                    bw.newLine();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to log the dependency tree to a file", e);
                }
            };
        }
        try {
            logTree(log);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    getLog().debug("Failed to close the output file", e);
                }
            }
        }
    }

    private void logTree(final Consumer<String> log) throws MojoExecutionException {
        log.accept("Quarkus application " + mode.toUpperCase() + " mode build dependency tree:");

        final ArtifactCoords appArtifact = ArtifactCoords.pom(project.getGroupId(), project.getArtifactId(),
                project.getVersion());
        final BootstrapAppModelResolver modelResolver;
        try {
            modelResolver = new BootstrapAppModelResolver(resolver())
                    .setRuntimeModelOnly(runtimeOnly);
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
            modelResolver.setLegacyModelResolver(BootstrapAppModelResolver.isLegacyModelResolver(project.getProperties()));
            modelResolver.setDepLogConfig(DependencyLoggingConfig.builder()
                    .setMessageConsumer(log)
                    .setVerbose(verbose)
                    .setGraph(graph)
                    .build());
            modelResolver.resolveModel(appArtifact);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve application model " + appArtifact + " dependencies", e);
        }
    }

    protected MavenArtifactResolver resolver() {
        return resolver == null
                ? resolver = workspaceProvider.createArtifactResolver(BootstrapMavenContext.config()
                        .setUserSettings(session.getRequest().getUserSettingsFile())
                        // The system needs to be initialized with the bootstrap model builder to properly interpolate system properties set on the command line
                        // e.g. -Dquarkus.platform.version=xxx
                        //.setRepositorySystem(repoSystem)
                        // The session should be initialized with the loaded workspace
                        //.setRepositorySystemSession(repoSession)
                        .setRemoteRepositories(repos)
                        // To support multi-module projects that haven't been installed
                        .setPreferPomsFromWorkspace(true))
                : resolver;
    }
}
