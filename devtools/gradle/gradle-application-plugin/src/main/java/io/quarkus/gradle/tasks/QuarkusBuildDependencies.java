package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gradle.QuarkusPlugin;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Collect the Quarkus app dependencies, the contents of the {@code quarkus-app/lib} folder, without making the task
 * cache anything, but still provide up-to-date checks.
 *
 * <p>
 * Caching dependency jars is wasted effort and unnecessarily pollutes the Gradle build cache.
 */
public abstract class QuarkusBuildDependencies extends QuarkusBuildTask {

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    public QuarkusBuildDependencies(QuarkusBuildConfiguration buildConfiguration) {
        super(buildConfiguration, "Collect dependencies for the Quarkus application, prefer the 'quarkusBuild' task");
    }

    /**
     * Points to {@code build/quarkus-build/dep}.
     */
    @OutputDirectory
    public File getDependenciesBuildDir() {
        return effectiveConfig().depBuildDir().toFile();
    }

    @TaskAction
    public void collectDependencies() {
        Path depDir = effectiveConfig().depBuildDir();

        // Caching and "up-to-date" checks depend on the inputs, this 'delete()' should ensure that the up-to-date
        // checks work against "clean" outputs, considering that the outputs depend on the package-type.
        getFileSystemOperations().delete(delete -> delete.delete(depDir));

        String packageType = effectiveConfig().packageType();
        if (QuarkusBuildConfiguration.isFastJarPackageType(packageType)) {
            fastJarDependencies();
        } else if (QuarkusBuildConfiguration.isLegacyJarPackageType(packageType)) {
            legacyJarDependencies();
        } else if (QuarkusBuildConfiguration.isMutableJarPackageType(packageType)) {
            getLogger().info(
                    "Falling back to 'full quarkus application build' for packaging type {}, this task's output is empty for this package type",
                    packageType);
        } else if (QuarkusBuildConfiguration.isUberJarPackageType(packageType)) {
            getLogger().info("Dependencies not needed for packaging type {}, this task's output is empty for this package type",
                    packageType);
        } else {
            throw new GradleException("Unsupported package type " + packageType);
        }
    }

    /**
     * Resolves and copies dependencies for the {@code jar}, {@code fast-jar} and {@code native} package types.
     * Does not work for {@code mutable-jar} ({@code lib/deployment/} missing).
     * Unnecessary for {@code uber-jar}.
     */
    private void fastJarDependencies() {
        Path depDir = effectiveConfig().depBuildDir();
        Path libBoot = depDir.resolve("lib/boot");
        Path libMain = depDir.resolve("lib/main");
        jarDependencies(libBoot, libMain);
    }

    /**
     * Resolves and copies the dependencies for the {@code legacy-jar} package type.
     *
     * <p>
     * Side node: Quarkus' {@code legacy-jar} package type produces {@code modified-*.jar} files for some
     * dependencies, but this implementation has no knowledge of which dependencies will be modified.
     */
    private void legacyJarDependencies() {
        Path depDir = effectiveConfig().depBuildDir();
        Path lib = depDir.resolve("lib");
        jarDependencies(lib, lib);
    }

    private void jarDependencies(Path libBoot, Path libMain) {
        Path depDir = effectiveConfig().depBuildDir();

        getLogger().info("Placing Quarkus application dependencies for package type {} in {}", effectiveConfig().packageType(),
                depDir);

        try {
            Files.createDirectories(libBoot);
            Files.createDirectories(libMain);
        } catch (IOException e) {
            throw new GradleException(String.format("Failed to create directories in %s", depDir), e);
        }

        ApplicationModel appModel = extension().getApplicationModel();

        // see https://quarkus.io/guides/class-loading-reference#configuring-class-loading
        String removedArtifactsProp = effectiveConfig()
                .configMap().getOrDefault(QuarkusPlugin.CLASS_LOADING_PARENT_FIRST_ARTIFACTS, "");
        java.util.Optional<Set<ArtifactKey>> optionalDependencies = java.util.Optional.ofNullable(
                effectiveConfig().configMap().getOrDefault(QuarkusPlugin.CLASS_LOADING_REMOVED_ARTIFACTS, null))
                .map(s -> Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(gact -> !gact.isEmpty())
                        .map(ArtifactKey::fromString)
                        .collect(Collectors.toSet()));
        Set<ArtifactKey> removedArtifacts = Arrays.stream(removedArtifactsProp.split(","))
                .map(String::trim)
                .filter(gact -> !gact.isEmpty())
                .map(ArtifactKey::fromString)
                .collect(Collectors.toSet());

        appModel.getRuntimeDependencies().stream()
                .filter(appDep -> {
                    // copied from io.quarkus.deployment.pkg.steps.JarResultBuildStep.includeAppDep
                    if (!appDep.isJar()) {
                        return false;
                    }
                    if (appDep.isOptional()) {
                        return optionalDependencies.map(appArtifactKeys -> appArtifactKeys.contains(appDep.getKey()))
                                .orElse(true);
                    }
                    return !removedArtifacts.contains(appDep.getKey());
                })
                .map(dep -> Map.entry(dep.isFlagSet(DependencyFlags.CLASSLOADER_RUNNER_PARENT_FIRST) ? libBoot : libMain, dep))
                .peek(depAndTarget -> {
                    ResolvedDependency dep = depAndTarget.getValue();
                    Path targetDir = depAndTarget.getKey();
                    dep.getResolvedPaths().forEach(p -> {
                        String file = dep.getGroupId() + '.' + p.getFileName();
                        Path target = targetDir.resolve(file);
                        if (!Files.exists(target)) {
                            getLogger().debug("Dependency {} : copying {} to {}",
                                    dep.toGACTVString(),
                                    p, target);
                            try {
                                Files.copy(p, target);
                            } catch (IOException e) {
                                throw new GradleException(String.format("Failed to copy %s to %s", p, target), e);
                            }
                        }
                    });
                })
                .collect(Collectors.toMap(Map.Entry::getKey, depAndTarget -> 1, Integer::sum))
                .forEach((path, count) -> getLogger().info("Copied {} files into {}", count, path));
    }
}
