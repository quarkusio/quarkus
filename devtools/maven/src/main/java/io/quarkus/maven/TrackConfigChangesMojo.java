package io.quarkus.maven;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.zip.Checksum;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.runtime.LaunchMode;

/**
 * Maven goal that is executed before the {@link BuildMojo}.
 * The goal looks for a file that contains build time configuration options read during the previous build.
 * If that file exists, the goal will check whether the configuration options used during the previous build
 * have changed in the current configuration and will persist their current values to another file, so that
 * both configuration files could be compared by tools caching build goal outcomes to check whether the previous
 * outcome of the {@link BuildMojo} needs to be rebuilt.
 */
@Mojo(name = "track-config-changes", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class TrackConfigChangesMojo extends QuarkusBootstrapMojo {

    /**
     * Skip the execution of this mojo
     */
    @Parameter(defaultValue = "false", property = "quarkus.track-config-changes.skip")
    boolean skip = false;

    @Parameter(property = "launchMode")
    String mode;

    @Parameter(property = "quarkus.track-config-changes.outputDirectory", defaultValue = "${project.build.directory}")
    File outputDirectory;

    @Parameter(property = "quarkus.track-config-changes.outputFile", required = false)
    File outputFile;

    @Parameter(property = "quarkus.recorded-build-config.directory", defaultValue = "${basedir}/.quarkus")
    File recordedBuildConfigDirectory;

    @Parameter(property = "quarkus.recorded-build-config.file", required = false)
    String recordedBuildConfigFile;

    /**
     * Whether to dump the current build configuration in case the configuration from the previous build isn't found
     */
    @Parameter(defaultValue = "false", property = "quarkus.track-config-changes.dump-current-when-recorded-unavailable")
    boolean dumpCurrentWhenRecordedUnavailable;

    /**
     * Whether to dump Quarkus application dependencies along with their checksums
     */
    @Parameter(defaultValue = "true", property = "quarkus.track-config-changes.dump-dependencies")
    boolean dumpDependencies;

    /**
     * Dependency dump file
     */
    @Parameter(property = "quarkus.track-config-changes.dependencies-file")
    File dependenciesFile;

    @Override
    protected boolean beforeExecute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping config dump");
            return false;
        }
        return true;
    }

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        final String lifecyclePhase = mojoExecution.getLifecyclePhase();
        if (mode == null) {
            if (lifecyclePhase == null) {
                mode = "NORMAL";
            } else {
                mode = lifecyclePhase.contains("test") ? "TEST" : "NORMAL";
            }
        }
        final LaunchMode launchMode = LaunchMode.valueOf(mode);
        if (getLog().isDebugEnabled()) {
            getLog().debug("Bootstrapping Quarkus application in mode " + launchMode);
        }

        final Path compareFile = resolvePreviousBuildConfigDump(launchMode);
        final boolean prevConfigExists = Files.exists(compareFile);
        if (!prevConfigExists && !dumpCurrentWhenRecordedUnavailable && !dumpDependencies) {
            getLog().info("Config dump from the previous build does not exist at " + compareFile);
            return;
        }

        CuratedApplication curatedApplication = null;
        QuarkusClassLoader deploymentClassLoader = null;
        final ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        final boolean clearNativeEnabledSystemProperty = setNativeEnabledIfNativeProfileEnabled();
        try {
            curatedApplication = bootstrapApplication(launchMode);
            if (prevConfigExists || dumpCurrentWhenRecordedUnavailable) {
                final Path targetFile = getOutputFile(outputFile, launchMode.getDefaultProfile(), "-config-check");
                Properties compareProps = new Properties();
                if (prevConfigExists) {
                    try (BufferedReader reader = Files.newBufferedReader(compareFile)) {
                        compareProps.load(reader);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read " + compareFile, e);
                    }
                }

                deploymentClassLoader = curatedApplication.createDeploymentClassLoader();
                Thread.currentThread().setContextClassLoader(deploymentClassLoader);

                final Class<?> codeGenerator = deploymentClassLoader.loadClass("io.quarkus.deployment.CodeGenerator");
                final Method dumpConfig = codeGenerator.getMethod("dumpCurrentConfigValues", ApplicationModel.class,
                        String.class,
                        Properties.class, QuarkusClassLoader.class, Properties.class, Path.class);
                dumpConfig.invoke(null, curatedApplication.getApplicationModel(),
                        launchMode.name(), getBuildSystemProperties(true),
                        deploymentClassLoader, compareProps, targetFile);
            }

            if (dumpDependencies) {
                final List<Path> deps = new ArrayList<>();
                for (var d : curatedApplication.getApplicationModel().getDependencies(DependencyFlags.DEPLOYMENT_CP)) {
                    for (Path resolvedPath : d.getResolvedPaths()) {
                        deps.add(resolvedPath.toAbsolutePath());
                    }
                }
                Collections.sort(deps);
                final Path targetFile = getOutputFile(dependenciesFile, launchMode.getDefaultProfile(),
                        "-dependencies.txt");
                Files.createDirectories(targetFile.getParent());
                try (BufferedWriter writer = Files.newBufferedWriter(targetFile)) {
                    for (var dep : deps) {
                        writer.write(dep.toString());
                        writer.newLine();
                    }
                }
            }
        } catch (Exception any) {
            throw new MojoExecutionException("Failed to bootstrap Quarkus application", any);
        } finally {
            if (clearNativeEnabledSystemProperty) {
                System.clearProperty("quarkus.native.enabled");
            }
            Thread.currentThread().setContextClassLoader(originalCl);
            if (deploymentClassLoader != null) {
                deploymentClassLoader.close();
            }
        }
    }

    private Path resolvePreviousBuildConfigDump(LaunchMode launchMode) {
        final Path previousBuildConfigDump = this.recordedBuildConfigFile == null ? null
                : Path.of(this.recordedBuildConfigFile);
        if (previousBuildConfigDump == null) {
            return recordedBuildConfigDirectory.toPath()
                    .resolve("quarkus-" + launchMode.getDefaultProfile() + "-config-dump");
        }
        if (previousBuildConfigDump.isAbsolute()) {
            return previousBuildConfigDump;
        }
        return recordedBuildConfigDirectory.toPath().resolve(previousBuildConfigDump);
    }

    private Path getOutputFile(File outputFile, String profile, String fileNameSuffix) {
        if (outputFile == null) {
            return outputDirectory.toPath().resolve("quarkus-" + profile + fileNameSuffix);
        }
        if (outputFile.isAbsolute()) {
            return outputFile.toPath();
        }
        return outputDirectory.toPath().resolve(outputFile.toPath());
    }

    private static void updateChecksum(Checksum checksum, Iterable<Path> pc) throws IOException {
        for (var path : sort(pc)) {
            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    updateChecksum(checksum, stream);
                }
            } else {
                checksum.update(Files.readAllBytes(path));
            }
        }
    }

    private static Iterable<Path> sort(Iterable<Path> original) {
        var i = original.iterator();
        if (!i.hasNext()) {
            return List.of();
        }
        var o = i.next();
        if (!i.hasNext()) {
            return List.of(o);
        }
        final List<Path> sorted = new ArrayList<>();
        sorted.add(o);
        while (i.hasNext()) {
            sorted.add(i.next());
        }
        Collections.sort(sorted);
        return sorted;
    }
}
