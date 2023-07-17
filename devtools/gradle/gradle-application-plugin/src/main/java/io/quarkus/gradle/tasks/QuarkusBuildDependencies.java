package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.TaskAction;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.pkg.PackageConfig;
import io.quarkus.fs.util.ZipUtils;
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
    static final String CLASS_LOADING_REMOVED_ARTIFACTS = "quarkus.class-loading.removed-artifacts";
    static final String CLASS_LOADING_PARENT_FIRST_ARTIFACTS = "quarkus.class-loading.parent-first-artifacts";
    static final String FILTER_OPTIONAL_DEPENDENCIES = "quarkus.package.filter-optional-dependencies";
    static final String INCLUDED_OPTIONAL_DEPENDENCIES = "quarkus.package.included-optional-dependencies";

    @Inject
    public QuarkusBuildDependencies() {
        super("Collect dependencies for the Quarkus application to be built. " +
                "Do not use this task directly, use '" + QuarkusPlugin.QUARKUS_BUILD_TASK_NAME + "'");
    }

    /**
     * Points to {@code build/quarkus-build/dep}.
     */
    @OutputDirectories
    public Map<String, File> getOutputDirectories() {
        Map<String, File> outputs = new HashMap<>();
        PackageConfig.BuiltInType packageType = packageType();
        switch (packageType) {
            case JAR:
            case FAST_JAR:
            case NATIVE:
            case LEGACY_JAR:
            case LEGACY:
                outputs.put("dependencies-dir", depBuildDir().toFile());
                break;
            case MUTABLE_JAR:
            case UBER_JAR:
            case NATIVE_SOURCES:
                break;
            default:
                throw new GradleException("Unsupported package type " + packageType);
        }
        return outputs;
    }

    @TaskAction
    public void collectDependencies() {
        Path depDir = depBuildDir();

        // Caching and "up-to-date" checks depend on the inputs, this 'delete()' should ensure that the up-to-date
        // checks work against "clean" outputs, considering that the outputs depend on the package-type.
        getFileSystemOperations().delete(delete -> delete.delete(depDir));

        PackageConfig.BuiltInType packageType = packageType();
        switch (packageType) {
            case JAR:
            case FAST_JAR:
            case NATIVE:
                fastJarDependencies();
                break;
            case LEGACY_JAR:
            case LEGACY:
                legacyJarDependencies();
                break;
            case MUTABLE_JAR:
            case UBER_JAR:
            case NATIVE_SOURCES:
                getLogger().info(
                        "Falling back to 'full quarkus application build' for package type {}, this task's output is empty for this package type",
                        packageType);
                break;
            default:
                throw new GradleException("Unsupported package type " + packageType);
        }
    }

    /**
     * Resolves and copies dependencies for the {@code jar}, {@code fast-jar} and {@code native} package types.
     * Does not work for {@code mutable-jar} ({@code lib/deployment/} missing).
     * Unnecessary for {@code uber-jar}.
     */
    private void fastJarDependencies() {
        Path depDir = depBuildDir();
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
        Path depDir = depBuildDir();
        Path lib = depDir.resolve("lib");
        jarDependencies(lib, lib);
    }

    private void jarDependencies(Path libBoot, Path libMain) {
        Path depDir = depBuildDir();

        getLogger().info("Placing Quarkus application dependencies for package type {} in {}", packageType(),
                depDir);

        try {
            Files.createDirectories(libBoot);
            Files.createDirectories(libMain);
        } catch (IOException e) {
            throw new GradleException(String.format("Failed to create directories in %s", depDir), e);
        }

        ApplicationModel appModel = resolveAppModelForBuild();
        Map<String, String> configMap = extension().buildEffectiveConfiguration(appModel.getAppArtifact()).configMap();

        // see https://quarkus.io/guides/class-loading-reference#configuring-class-loading
        Set<ArtifactKey> removedArtifacts = java.util.Optional.ofNullable(
                configMap.getOrDefault(CLASS_LOADING_REMOVED_ARTIFACTS, null))
                .map(QuarkusBuildDependencies::dependenciesListToArtifactKeySet)
                .orElse(Collections.emptySet());
        getLogger().info("Removed artifacts: {}", configMap.getOrDefault(CLASS_LOADING_REMOVED_ARTIFACTS, "(none)"));

        String parentFirstArtifactsProp = configMap.getOrDefault(CLASS_LOADING_PARENT_FIRST_ARTIFACTS, "");
        Set<ArtifactKey> parentFirstArtifacts = dependenciesListToArtifactKeySet(parentFirstArtifactsProp);
        getLogger().info("parent first artifacts: {}", configMap.getOrDefault(CLASS_LOADING_PARENT_FIRST_ARTIFACTS, "(none)"));

        String optionalDependenciesProp = configMap.getOrDefault(INCLUDED_OPTIONAL_DEPENDENCIES, "");
        boolean filterOptionalDependencies = Boolean
                .parseBoolean(configMap.getOrDefault(FILTER_OPTIONAL_DEPENDENCIES, "false"));
        Set<ArtifactKey> optionalDependencies = filterOptionalDependencies
                ? dependenciesListToArtifactKeySet(optionalDependenciesProp)
                : Collections.emptySet();

        appModel.getRuntimeDependencies().stream()
                .filter(appDep -> {
                    // copied from io.quarkus.deployment.pkg.steps.JarResultBuildStep.includeAppDep
                    if (!appDep.isJar()) {
                        return false;
                    }
                    if (filterOptionalDependencies && appDep.isOptional()) {
                        return optionalDependencies.contains(appDep.getKey());
                    }
                    return !removedArtifacts.contains(appDep.getKey());
                })
                .map(dep -> Map.entry(dep.isFlagSet(DependencyFlags.CLASSLOADER_RUNNER_PARENT_FIRST)
                        || parentFirstArtifacts.contains(dep.getKey()) ? libBoot : libMain, dep))
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
                            if (Files.isDirectory(p)) {
                                // This case can happen when we are building a jar from inside the Quarkus repository
                                // and Quarkus Bootstrap's localProjectDiscovery has been set to true. In such a case
                                // the non-jar dependencies are the Quarkus dependencies picked up on the file system
                                try {
                                    ZipUtils.zip(p, target);
                                } catch (IOException e) {
                                    throw new GradleException(
                                            String.format("Failed to archive classes at %s into %s", p, target), e);
                                }
                            } else {
                                try {
                                    Files.copy(p, target);
                                } catch (IOException e) {
                                    throw new GradleException(String.format("Failed to copy %s to %s", p, target), e);
                                }
                            }
                        }
                    });
                })
                .collect(Collectors.toMap(Map.Entry::getKey, depAndTarget -> 1, Integer::sum))
                .forEach((path, count) -> getLogger().info("Copied {} files into {}", count, path));
    }

    private static Set<ArtifactKey> dependenciesListToArtifactKeySet(String optionalDependenciesProp) {
        return Arrays.stream(optionalDependenciesProp.split(","))
                .map(String::trim)
                .filter(gact -> !gact.isEmpty())
                .map(ArtifactKey::fromString)
                .collect(Collectors.toSet());
    }
}
