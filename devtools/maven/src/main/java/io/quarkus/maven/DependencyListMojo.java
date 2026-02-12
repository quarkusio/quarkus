package io.quarkus.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.components.QuarkusWorkspaceProvider;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Lists dependencies of a Quarkus application as resolved by the Quarkus bootstrap dependency resolver.
 */
@Mojo(name = "dependency-list", defaultPhase = LifecyclePhase.NONE, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
public class DependencyListMojo extends AbstractMojo {

    @Inject
    QuarkusWorkspaceProvider workspaceProvider;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    MavenSession session;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    List<RemoteRepository> repos;

    /**
     * Target launch mode corresponding to {@link io.quarkus.runtime.LaunchMode} for which the dependencies should be listed.
     * {@code io.quarkus.runtime.LaunchMode.NORMAL} is the default.
     */
    @Parameter(property = "mode", defaultValue = "prod")
    String mode;

    /**
     * Comma-separated list of dependency flag names from {@link DependencyFlags} indicating which dependencies should be
     * listed.
     * Only dependencies that have all the specified flags set will be included.
     * <p>
     * Accepted flag names (case-insensitive, both constant names and text names are supported):
     * {@code OPTIONAL}, {@code DIRECT}, {@code RUNTIME_CP} (or {@code runtime-cp}),
     * {@code DEPLOYMENT_CP} (or {@code deployment-cp}), {@code RUNTIME_EXTENSION_ARTIFACT} (or
     * {@code runtime-extension-artifact}),
     * {@code WORKSPACE_MODULE} (or {@code workspace-module}), {@code RELOADABLE},
     * {@code TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT} (or {@code top-level-runtime-extension-artifact}),
     * {@code CLASSLOADER_PARENT_FIRST} (or {@code classloader-parent-first}),
     * {@code CLASSLOADER_RUNNER_PARENT_FIRST} (or {@code classloader-runner-parent-first}),
     * {@code CLASSLOADER_LESSER_PRIORITY} (or {@code classloader-lesser-priority}),
     * {@code COMPILE_ONLY} (or {@code compile-only}).
     * <p>
     * If not specified, all dependencies returned by {@link ApplicationModel#getDependencies()} are listed.
     */
    @Parameter(property = "flags")
    String flags;

    /**
     * If specified, this parameter will cause the dependency list to be written to the path specified, instead of writing to
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
     * Whether to include dependency scope and flag names in the output for each dependency.
     */
    @Parameter(property = "verbose")
    boolean verbose;

    // Maps both Java constant names (upper-case with underscores) and text names (lower-case with hyphens) to flag values
    private static final Map<String, Integer> FLAG_NAMES = Map.ofEntries(
            Map.entry("optional", DependencyFlags.OPTIONAL),
            Map.entry("direct", DependencyFlags.DIRECT),
            Map.entry("runtime_cp", DependencyFlags.RUNTIME_CP),
            Map.entry("runtime-cp", DependencyFlags.RUNTIME_CP),
            Map.entry("deployment_cp", DependencyFlags.DEPLOYMENT_CP),
            Map.entry("deployment-cp", DependencyFlags.DEPLOYMENT_CP),
            Map.entry("runtime_extension_artifact", DependencyFlags.RUNTIME_EXTENSION_ARTIFACT),
            Map.entry("runtime-extension-artifact", DependencyFlags.RUNTIME_EXTENSION_ARTIFACT),
            Map.entry("workspace_module", DependencyFlags.WORKSPACE_MODULE),
            Map.entry("workspace-module", DependencyFlags.WORKSPACE_MODULE),
            Map.entry("reloadable", DependencyFlags.RELOADABLE),
            Map.entry("top_level_runtime_extension_artifact", DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT),
            Map.entry("top-level-runtime-extension-artifact", DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT),
            Map.entry("classloader_parent_first", DependencyFlags.CLASSLOADER_PARENT_FIRST),
            Map.entry("classloader-parent-first", DependencyFlags.CLASSLOADER_PARENT_FIRST),
            Map.entry("classloader_runner_parent_first", DependencyFlags.CLASSLOADER_RUNNER_PARENT_FIRST),
            Map.entry("classloader-runner-parent-first", DependencyFlags.CLASSLOADER_RUNNER_PARENT_FIRST),
            Map.entry("classloader_lesser_priority", DependencyFlags.CLASSLOADER_LESSER_PRIORITY),
            Map.entry("classloader-lesser-priority", DependencyFlags.CLASSLOADER_LESSER_PRIORITY),
            Map.entry("compile_only", DependencyFlags.COMPILE_ONLY),
            Map.entry("compile-only", DependencyFlags.COMPILE_ONLY));

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
                    throw new RuntimeException("Failed to log the dependency list to a file", e);
                }
            };
        }
        try {
            logDependencies(log);
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

    private void logDependencies(final Consumer<String> log) throws MojoExecutionException {
        final int parsedFlags = parseFlags();
        final ApplicationModel appModel = resolveApplicationModel(parsedFlags);
        final StringBuilder heading = new StringBuilder()
                .append("Quarkus application ").append(mode.toUpperCase()).append(" mode dependencies");
        if (flags != null && !flags.isBlank()) {
            heading.append(" with flags ").append(flags);
        }
        heading.append(":");
        log.accept(heading.toString());
        final Iterable<ResolvedDependency> deps = parsedFlags == 0
                ? appModel.getDependencies()
                : appModel.getDependencies(parsedFlags);
        final List<String> lines = new ArrayList<>();
        for (ResolvedDependency dep : deps) {
            if (verbose) {
                final StringBuilder sb = new StringBuilder().append(dep.toCompactCoords());
                sb.append(" [").append(dep.getScope()).append("] ")
                        .append(DependencyFlags.toNames(dep.getFlags()));
                lines.add(sb.toString());
            } else {
                lines.add(dep.toCompactCoords());
            }
        }
        lines.sort(null);
        for (String line : lines) {
            log.accept(line);
        }
    }

    private int parseFlags() throws MojoExecutionException {
        if (flags == null || flags.isBlank()) {
            return 0;
        }
        int result = 0;
        for (String name : flags.split(",")) {
            final String trimmed = name.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            final Integer flagValue = FLAG_NAMES.get(trimmed.toLowerCase(Locale.ROOT));
            if (flagValue == null) {
                throw new MojoExecutionException(
                        "Unrecognized dependency flag '" + trimmed + "'. Supported flags: " + String.join(", ",
                                "OPTIONAL", "DIRECT", "RUNTIME_CP", "DEPLOYMENT_CP",
                                "RUNTIME_EXTENSION_ARTIFACT", "WORKSPACE_MODULE", "RELOADABLE",
                                "TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT", "CLASSLOADER_PARENT_FIRST",
                                "CLASSLOADER_RUNNER_PARENT_FIRST", "CLASSLOADER_LESSER_PRIORITY",
                                "COMPILE_ONLY"));
            }
            result |= flagValue;
        }
        return result;
    }

    private ApplicationModel resolveApplicationModel(int flags) throws MojoExecutionException {
        final ArtifactCoords appArtifact = ArtifactCoords.pom(project.getGroupId(), project.getArtifactId(),
                project.getVersion());
        final BootstrapAppModelResolver modelResolver;
        try {
            modelResolver = new BootstrapAppModelResolver(resolver())
                    .setRuntimeModelOnly(((flags & DependencyFlags.RUNTIME_CP) > 0));
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
            modelResolver.setLegacyModelResolver(BootstrapAppModelResolver.isLegacyModelResolver(project.getProperties()));
            return modelResolver.resolveModel(appArtifact);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve application model " + appArtifact + " dependencies", e);
        }
    }

    protected MavenArtifactResolver resolver() {
        return resolver == null
                ? resolver = workspaceProvider.createArtifactResolver(BootstrapMavenContext.config()
                        .setUserSettings(session.getRequest().getUserSettingsFile())
                        .setRemoteRepositories(repos)
                        .setPreferPomsFromWorkspace(true))
                : resolver;
    }
}
