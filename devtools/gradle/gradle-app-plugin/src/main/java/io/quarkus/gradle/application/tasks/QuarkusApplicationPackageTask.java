package io.quarkus.gradle.application.tasks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.application.internal.packaging.PackageResult;
import io.quarkus.gradle.application.internal.packaging.PackageResult.Artifact;
import io.quarkus.gradle.application.internal.packaging.PackageResultCodec;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;

/**
 * Packages the JVM output of a named Quarkus application build.
 * <p>
 * The task validates that Quarkus produced the configured package root and predicted primary launcher. When startup
 * archive generation is requested, it also validates the reported archive kind, path, and contents. It writes a package
 * result descriptor for downstream run, test, image, deployment, and publication consumers. Package builds are not
 * build-cacheable yet.
 * <p>
 * The supported compatibility contract covers plugin-registered instances and the documented task names, properties,
 * and options. No compatibility commitment is made for direct construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Quarkus package builds are not build-cacheable yet")
public abstract class QuarkusApplicationPackageTask extends QuarkusApplicationBuildTask {

    /**
     * Returns the package result descriptor written after validation.
     *
     * @return the package result file
     */
    @OutputFile
    public abstract RegularFileProperty getPackageResultFile();

    /**
     * Returns operation-forced Quarkus properties derived by the plugin for this package build.
     * This is task wiring rather than an independent user configuration surface.
     *
     * @return the package operation properties
     */
    @Input
    public abstract MapProperty<String, String> getPackageOperationForcedProperties();

    /**
     * Returns the startup archive type whose package result must be validated, when configured.
     *
     * @return the optional startup archive type
     */
    @Input
    @Optional
    public abstract Property<QuarkusApplicationJvmStartupArchiveType> getPackageStartupArchiveType();

    /**
     * Predicts the package's primary executable JAR from the configured build type and output shape.
     *
     * @return a provider of the primary executable JAR
     */
    @Internal
    public Provider<File> getPrimaryJarFile() {
        return getOutputDirectory().flatMap(outputDirectory -> getBuildType().flatMap(buildType -> getOutputName()
                .flatMap(outputName -> getAdditionalDescriptorShapeProperties().map(properties -> primaryJarFile(
                        outputDirectory.getAsFile(), buildType, outputName, properties)))));
    }

    /**
     * Builds the package, validates its outputs, and writes the package result descriptor.
     */
    @TaskAction
    public void buildPackage() {
        Path packageResultFile = getPackageResultFile().get().getAsFile().toPath();
        Path augmentResultFile = packageResultFile.resolveSibling("package-augmentation-result.properties");
        var result = buildOperations().buildPackage(buildRequest(getPackageOperationForcedProperties().get()),
                augmentResultFile);
        validatePackageOutput(result);
        if (getPackageStartupArchiveType().isPresent()) {
            validateStartupArchive(result.artifacts(), getPackageStartupArchiveType().get());
        }
        new PackageResultCodec().write(packageResultFile, result);
    }

    private void validatePackageOutput(PackageResult result) {
        validatePackageOutput(result,
                getOutputDirectory().get().getAsFile().toPath(),
                getPrimaryJarFile().get().toPath(),
                getBuildName().get(),
                getBuildType().get());
    }

    static void validatePackageOutput(PackageResult result, Path configuredOutputRoot, Path expectedPrimaryJar,
            String buildName, QuarkusApplicationBuildType buildType) {
        Path outputRoot = configuredOutputRoot.toAbsolutePath().normalize();
        Path expectedJar = expectedPrimaryJar.toAbsolutePath().normalize();
        Path reportedRoot = result.outputRoot().toAbsolutePath().normalize();
        Path reportedJar = result.jarPath().toAbsolutePath().normalize();
        Path expectedRelativeJar;
        try {
            expectedRelativeJar = outputRoot.relativize(expectedJar);
        } catch (IllegalArgumentException e) {
            throw invalidPackageOutput(buildName, buildType, outputRoot, expectedJar, reportedRoot, reportedJar,
                    "the predicted primary launcher is outside the configured package root");
        }
        if (!reportedRoot.equals(outputRoot)) {
            throw invalidPackageOutput(buildName, buildType, outputRoot, expectedJar, reportedRoot, reportedJar,
                    "the reported output root differs from the configured package root");
        }
        if (!reportedJar.equals(expectedJar)) {
            throw invalidPackageOutput(buildName, buildType, outputRoot, expectedJar, reportedRoot, reportedJar,
                    "the reported primary launcher differs from the predicted launcher");
        }
        if (expectedRelativeJar.getNameCount() == 0 || expectedRelativeJar.startsWith("..")
                || !expectedJar.startsWith(outputRoot)) {
            throw invalidPackageOutput(buildName, buildType, outputRoot, expectedJar, reportedRoot, reportedJar,
                    "the primary launcher is not contained by the package root");
        }
        if (!Files.isDirectory(outputRoot)) {
            throw invalidPackageOutput(buildName, buildType, outputRoot, expectedJar, reportedRoot, reportedJar,
                    "the package root is not a directory");
        }
        if (!Files.isRegularFile(expectedJar)) {
            throw invalidPackageOutput(buildName, buildType, outputRoot, expectedJar, reportedRoot, reportedJar,
                    "the primary launcher is not a regular file");
        }
    }

    private static GradleException invalidPackageOutput(String buildName, QuarkusApplicationBuildType buildType,
            Path outputRoot, Path expectedJar, Path reportedRoot, Path reportedJar, String reason) {
        String expectedRelativeJar = expectedJar.startsWith(outputRoot)
                ? outputRoot.relativize(expectedJar).toString()
                : expectedJar.toString();
        return new GradleException("Quarkus package build '" + buildName + "' ("
                + buildType.jarType().orElse(buildType.name())
                + ") produced an invalid package: " + reason + ". Configured output root: " + outputRoot
                + "; expected launcher: " + expectedRelativeJar + "; reported output root: " + reportedRoot
                + "; reported launcher: " + reportedJar);
    }

    private static File primaryJarFile(File outputDirectory, QuarkusApplicationBuildType buildType, String outputName,
            Map<String, String> properties) {
        return switch (buildType) {
            case FAST_JAR, AOT_JAR, MUTABLE_JAR -> new File(outputDirectory, "quarkus-run.jar");
            case LEGACY_JAR, UBER_JAR -> new File(outputDirectory, outputName + runnerSuffix(properties) + ".jar");
            case NATIVE_EXECUTABLE, NATIVE_SOURCES -> throw new IllegalStateException(
                    "Build type " + buildType + " does not produce a primary JAR file");
        };
    }

    private void validateStartupArchive(List<Artifact> artifacts,
            QuarkusApplicationJvmStartupArchiveType type) {
        List<Artifact> matching = artifacts.stream()
                .filter(artifact -> "appCDS".equals(artifact.type()))
                .filter(artifact -> type.getCoreType().equals(artifact.metadata().get("archive-type")))
                .toList();
        if (matching.size() != 1) {
            throw new GradleException("Quarkus package build '" + getBuildName().get() + "' expected exactly one "
                    + type + " startup archive result, but found " + matching.size());
        }
        Path expected = getOutputDirectory().get().getAsFile().toPath().resolve(type.getDefaultName())
                .toAbsolutePath().normalize();
        Path actual = matching.get(0).path()
                .orElseThrow(() -> new GradleException("Quarkus package build '" + getBuildName().get()
                        + "' reported a " + type + " startup archive without a path"))
                .toAbsolutePath().normalize();
        if (!expected.equals(actual)) {
            throw new GradleException("Quarkus package build '" + getBuildName().get() + "' reported " + type
                    + " startup archive " + actual + ", expected " + expected);
        }
        String expectedKind = type.isDirectory() ? "DIRECTORY" : "FILE";
        if (!expectedKind.equals(matching.get(0).metadata().get("artifact-kind"))) {
            throw new GradleException("Quarkus package build '" + getBuildName().get() + "' reported " + type
                    + " startup archive with artifact kind " + matching.get(0).metadata().get("artifact-kind")
                    + ", expected " + expectedKind);
        }
        if (type.isDirectory() ? !Files.isDirectory(actual) : !Files.isRegularFile(actual)) {
            throw new GradleException("Quarkus package build '" + getBuildName().get() + "' did not create the "
                    + expectedKind.toLowerCase(Locale.ROOT) + " startup archive " + actual);
        }
        try {
            if (type.isDirectory()) {
                try (var children = Files.list(actual)) {
                    if (children.findAny().isEmpty()) {
                        throw new GradleException("Quarkus package build '" + getBuildName().get()
                                + "' created an empty startup archive directory " + actual);
                    }
                }
            } else if (Files.size(actual) == 0) {
                throw new GradleException("Quarkus package build '" + getBuildName().get()
                        + "' created an empty startup archive file " + actual);
            }
        } catch (java.io.IOException e) {
            throw new GradleException("Failed to inspect startup archive " + actual + " for Quarkus package build '"
                    + getBuildName().get() + "'", e);
        }
    }

    private static String runnerSuffix(Map<String, String> properties) {
        if (!Boolean.parseBoolean(properties.getOrDefault("quarkus.package.jar.add-runner-suffix", "true"))) {
            return "";
        }
        return properties.getOrDefault("quarkus.package.runner-suffix", "-runner");
    }
}
