package io.quarkus.gradle.application.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.application.internal.deployment.DeploymentResult;
import io.quarkus.gradle.application.internal.execution.BuildOperations;
import io.quarkus.gradle.application.internal.execution.BuildRequest;
import io.quarkus.gradle.application.internal.execution.DeploymentRequest;
import io.quarkus.gradle.application.internal.execution.ImageRequest;
import io.quarkus.gradle.application.internal.execution.RunRequest;
import io.quarkus.gradle.application.internal.execution.StartupOptimizedImageRequest;
import io.quarkus.gradle.application.internal.image.BuiltContainerImage;
import io.quarkus.gradle.application.internal.nativeimage.NativeResult;
import io.quarkus.gradle.application.internal.packaging.PackageResult;
import io.quarkus.gradle.application.internal.packaging.PackageResultCodec;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;

class QuarkusApplicationPackageTaskTest {

    @TempDir
    Path directory;

    @Test
    void acceptsEveryJvmLauncherNamingFamilyInsideThePackageRoot() throws IOException {
        for (var launcher : List.of(
                new Launcher(QuarkusApplicationBuildType.FAST_JAR, "quarkus-run.jar"),
                new Launcher(QuarkusApplicationBuildType.AOT_JAR, "quarkus-run.jar"),
                new Launcher(QuarkusApplicationBuildType.MUTABLE_JAR, "quarkus-run.jar"),
                new Launcher(QuarkusApplicationBuildType.LEGACY_JAR, "application-runner.jar"),
                new Launcher(QuarkusApplicationBuildType.UBER_JAR, "application-runner.jar"))) {
            Path outputRoot = Files.createDirectories(directory.resolve(launcher.buildType().name()));
            Path jar = Files.writeString(outputRoot.resolve(launcher.fileName()), "jar");
            PackageResult result = result(launcher.buildType(), outputRoot, jar);

            assertThatCode(() -> QuarkusApplicationPackageTask.validatePackageOutput(
                    result, outputRoot, jar, "application", launcher.buildType()))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void rejectsReportedOutputRootThatDiffersFromConfiguredRoot() throws IOException {
        Path configuredRoot = Files.createDirectories(directory.resolve("configured"));
        Path reportedRoot = Files.createDirectories(directory.resolve("reported"));
        Path expectedJar = Files.writeString(configuredRoot.resolve("quarkus-run.jar"), "jar");

        assertThatThrownBy(() -> QuarkusApplicationPackageTask.validatePackageOutput(
                result(QuarkusApplicationBuildType.FAST_JAR, reportedRoot, expectedJar),
                configuredRoot, expectedJar, "fast", QuarkusApplicationBuildType.FAST_JAR))
                .hasMessageContaining("reported output root differs")
                .hasMessageContaining("fast")
                .hasMessageContaining("fast-jar");
    }

    @Test
    void rejectsReportedLauncherThatDiffersFromPredictedLauncher() throws IOException {
        Path outputRoot = Files.createDirectories(directory.resolve("package"));
        Path expectedJar = Files.writeString(outputRoot.resolve("quarkus-run.jar"), "expected");
        Path reportedJar = Files.writeString(outputRoot.resolve("other.jar"), "reported");

        assertThatThrownBy(() -> QuarkusApplicationPackageTask.validatePackageOutput(
                result(QuarkusApplicationBuildType.FAST_JAR, outputRoot, reportedJar),
                outputRoot, expectedJar, "fast", QuarkusApplicationBuildType.FAST_JAR))
                .hasMessageContaining("reported primary launcher differs")
                .hasMessageContaining("quarkus-run.jar")
                .hasMessageContaining(reportedJar.toAbsolutePath().normalize().toString());
    }

    @Test
    void rejectsMissingPackageRoot() {
        Path outputRoot = directory.resolve("missing");
        Path expectedJar = outputRoot.resolve("quarkus-run.jar");

        assertThatThrownBy(() -> QuarkusApplicationPackageTask.validatePackageOutput(
                result(QuarkusApplicationBuildType.FAST_JAR, outputRoot, expectedJar),
                outputRoot, expectedJar, "fast", QuarkusApplicationBuildType.FAST_JAR))
                .hasMessageContaining("package root is not a directory");
    }

    @Test
    void rejectsMissingOrNonRegularLauncher() throws IOException {
        Path missingRoot = Files.createDirectories(directory.resolve("missing-launcher"));
        Path missingJar = missingRoot.resolve("quarkus-run.jar");
        assertThatThrownBy(() -> QuarkusApplicationPackageTask.validatePackageOutput(
                result(QuarkusApplicationBuildType.FAST_JAR, missingRoot, missingJar),
                missingRoot, missingJar, "fast", QuarkusApplicationBuildType.FAST_JAR))
                .hasMessageContaining("primary launcher is not a regular file");

        Path directoryRoot = Files.createDirectories(directory.resolve("directory-launcher"));
        Path directoryJar = Files.createDirectories(directoryRoot.resolve("quarkus-run.jar"));
        assertThatThrownBy(() -> QuarkusApplicationPackageTask.validatePackageOutput(
                result(QuarkusApplicationBuildType.FAST_JAR, directoryRoot, directoryJar),
                directoryRoot, directoryJar, "fast", QuarkusApplicationBuildType.FAST_JAR))
                .hasMessageContaining("primary launcher is not a regular file");
    }

    @Test
    void rejectsPredictedLauncherOutsideThePackageRoot() throws IOException {
        Path outputRoot = Files.createDirectories(directory.resolve("package"));
        Path outsideJar = Files.writeString(directory.resolve("outside.jar"), "jar");

        assertThatThrownBy(() -> QuarkusApplicationPackageTask.validatePackageOutput(
                result(QuarkusApplicationBuildType.FAST_JAR, outputRoot, outsideJar),
                outputRoot, outsideJar, "fast", QuarkusApplicationBuildType.FAST_JAR))
                .hasMessageContaining("primary launcher is not contained")
                .hasMessageContaining(outsideJar.toAbsolutePath().normalize().toString());
    }

    @Test
    void taskWritesReceiptOnlyAfterCompletePackageValidation() throws IOException {
        QuarkusApplicationPackageTask successful = task("successful");
        Path successfulRoot = Files.createDirectories(successful.getOutputDirectory().get().getAsFile().toPath());
        Path successfulJar = Files.writeString(successfulRoot.resolve("quarkus-run.jar"), "jar");
        PackageResult successfulResult = result(QuarkusApplicationBuildType.FAST_JAR, successfulRoot, successfulJar);
        successful.getOperations().set(new PackageBuildOperations(successfulResult));

        successful.buildPackage();

        assertThatCode(() -> new PackageResultCodec()
                .read(successful.getPackageResultFile().get().getAsFile().toPath()))
                .doesNotThrowAnyException();

        QuarkusApplicationPackageTask invalid = task("invalid");
        Path invalidRoot = Files.createDirectories(invalid.getOutputDirectory().get().getAsFile().toPath());
        Path expectedJar = invalidRoot.resolve("quarkus-run.jar");
        PackageResult invalidResult = result(QuarkusApplicationBuildType.FAST_JAR, invalidRoot, expectedJar);
        invalid.getOperations().set(new PackageBuildOperations(invalidResult));

        assertThatThrownBy(invalid::buildPackage)
                .hasMessageContaining("primary launcher is not a regular file");
        assertThat(invalid.getPackageResultFile().get().getAsFile()).doesNotExist();
    }

    private QuarkusApplicationPackageTask task(String buildName) {
        Project project = ProjectBuilder.builder().withProjectDir(directory.resolve(buildName).toFile()).build();
        QuarkusApplicationPackageTask task = project.getTasks()
                .register("quarkus" + buildName + "Build", QuarkusApplicationPackageTask.class)
                .get();
        task.getBuildName().set(buildName);
        task.getBuildType().set(QuarkusApplicationBuildType.FAST_JAR);
        task.getApplicationName().set("application");
        task.getApplicationVersion().set("1.0");
        task.getOutputName().set("application");
        task.getQuarkusBuildProperties().convention(java.util.Map.of());
        task.getPackageOperationForcedProperties().convention(java.util.Map.of());
        task.getGradleBuildDirectory().set(project.getLayout().getBuildDirectory());
        task.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir("package"));
        task.getApplicationModel().set(project.getLayout().getBuildDirectory().file("app-model.dat"));
        task.getPackageResultFile().set(project.getLayout().getBuildDirectory().file("package-result.properties"));
        task.getRuntimeClasspath().setFrom(List.of());
        task.getSourceDirectories().setFrom(List.of());
        task.getGradlePropertyPrefixes().convention(List.of());
        task.getGradlePropertyNames().convention(List.of());
        task.getSystemPropertyPrefixes().convention(List.of());
        task.getSystemPropertyNames().convention(List.of());
        task.getEnvironmentVariablePrefixes().convention(List.of());
        task.getEnvironmentVariableNames().convention(List.of());
        return task;
    }

    private static PackageResult result(QuarkusApplicationBuildType buildType, Path outputRoot, Path jar) {
        return new PackageResult("application", buildType, outputRoot, "application", jar,
                Optional.empty(), Optional.empty(), buildType == QuarkusApplicationBuildType.MUTABLE_JAR,
                buildType == QuarkusApplicationBuildType.UBER_JAR, Optional.empty(), List.of());
    }

    private record Launcher(QuarkusApplicationBuildType buildType, String fileName) {
    }

    private record PackageBuildOperations(PackageResult result) implements BuildOperations {

        @Override
        public void build(BuildRequest request) {
        }

        @Override
        public PackageResult buildPackage(BuildRequest request, Path augmentResultFile) {
            return result;
        }

        @Override
        public NativeResult buildNative(BuildRequest request, Path augmentResultFile) {
            return null;
        }

        @Override
        public BuiltContainerImage buildStartupOptimizedImage(StartupOptimizedImageRequest request) {
            return null;
        }

        @Override
        public BuiltContainerImage pushStartupOptimizedImage(StartupOptimizedImageRequest request) {
            return null;
        }

        @Override
        public BuiltContainerImage buildImage(ImageRequest request) {
            return null;
        }

        @Override
        public BuiltContainerImage pushImage(ImageRequest request) {
            return null;
        }

        @Override
        public DeploymentResult deploy(DeploymentRequest request) {
            return null;
        }

        @Override
        public void run(RunRequest request) {
        }
    }
}
