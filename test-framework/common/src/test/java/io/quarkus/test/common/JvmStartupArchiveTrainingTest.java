package io.quarkus.test.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveType;
import io.quarkus.test.common.JvmStartupArchiveTraining.ExecutionTarget;

class JvmStartupArchiveTrainingTest {

    @TempDir
    Path tempDirectory;

    @Test
    void absentMetadataRetainsLegacyBehavior() {
        assertThat(JvmStartupArchiveTraining.fromMetadata(new Properties())).isEmpty();
    }

    @Test
    void parsesHostAotTraining() {
        Path destination = tempDirectory.resolve("training").resolve("app.aot");
        Properties metadata = metadata("AOT", destination, "HOST_JVM");

        JvmStartupArchiveTraining training = JvmStartupArchiveTraining.fromMetadata(metadata).orElseThrow();

        assertThat(training.type()).isEqualTo(JvmStartupOptimizerArchiveType.AOT);
        assertThat(training.destination()).isEqualTo(destination);
        assertThat(training.executionTarget()).isEqualTo(ExecutionTarget.HOST_JVM);
        assertThat(training.containerDirectory()).isEmpty();
        assertThat(training.aotConfigurationDestination()).isEqualTo(destination.resolveSibling("app.aotconf"));
    }

    @Test
    void parsesBaseImageSccTrainingWithoutInterpretingHostPathAsContainerPath() {
        Path destination = tempDirectory.resolve("directory with spaces").resolve("app-scc");
        Properties metadata = metadata("SCC", destination, "BASE_IMAGE");
        metadata.setProperty(JvmStartupArchiveTraining.CONTAINER_DIRECTORY_PROPERTY, "/work/training-output");

        JvmStartupArchiveTraining training = JvmStartupArchiveTraining.fromMetadata(metadata).orElseThrow();

        assertThat(training.type()).isEqualTo(JvmStartupOptimizerArchiveType.SCC);
        assertThat(training.destination()).isEqualTo(destination);
        assertThat(training.hostDirectory()).isEqualTo(destination.getParent());
        assertThat(training.containerDirectory()).contains("/work/training-output");
        assertThat(training.containerArchivePath()).isEqualTo("/work/training-output/app-scc");
    }

    @Test
    void rejectsIncompleteMetadata() {
        Properties metadata = new Properties();
        metadata.setProperty(JvmStartupArchiveTraining.TYPE_PROPERTY, "AOT");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> JvmStartupArchiveTraining.fromMetadata(metadata))
                .withMessageContaining(JvmStartupArchiveTraining.DESTINATION_PROPERTY);
    }

    @Test
    void rejectsRelativeHostDestination() {
        Properties metadata = metadata("AOT", Path.of("build/training/app.aot"), "HOST_JVM");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> JvmStartupArchiveTraining.fromMetadata(metadata))
                .withMessageContaining("must be absolute");
    }

    @Test
    void rejectsWrongArtifactName() {
        Properties metadata = metadata("SCC", tempDirectory.resolve("not-the-cache"), "HOST_JVM");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> JvmStartupArchiveTraining.fromMetadata(metadata))
                .withMessageContaining("must end with 'app-scc'");
    }

    @Test
    void rejectsAppCdsTraining() {
        for (String spelling : List.of("AppCDS", "APP_CDS")) {
            Properties metadata = metadata(spelling, tempDirectory.resolve("app-cds.jsa"), "HOST_JVM");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> JvmStartupArchiveTraining.fromMetadata(metadata))
                    .withMessageContaining("AppCDS is not supported");
        }
    }

    @Test
    void rejectsMissingOrNonNormalizedContainerDirectory() {
        Properties missing = metadata("AOT", tempDirectory.resolve("app.aot"), "BASE_IMAGE");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> JvmStartupArchiveTraining.fromMetadata(missing))
                .withMessageContaining("requires a container directory");

        Properties nonNormalized = metadata("AOT", tempDirectory.resolve("app.aot"), "BASE_IMAGE");
        nonNormalized.setProperty(JvmStartupArchiveTraining.CONTAINER_DIRECTORY_PROPERTY, "/work/../training");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> JvmStartupArchiveTraining.fromMetadata(nonNormalized))
                .withMessageContaining("absolute normalized container path");

        Properties whitespace = metadata("AOT", tempDirectory.resolve("app.aot"), "BASE_IMAGE");
        whitespace.setProperty(JvmStartupArchiveTraining.CONTAINER_DIRECTORY_PROPERTY, "/work/training output");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> JvmStartupArchiveTraining.fromMetadata(whitespace))
                .withMessageContaining("without whitespace");
    }

    @Test
    void rejectsContainerDirectoryForHostTraining() {
        Properties metadata = metadata("SCC", tempDirectory.resolve("app-scc"), "HOST_JVM");
        metadata.setProperty(JvmStartupArchiveTraining.CONTAINER_DIRECTORY_PROPERTY, "/work/training");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> JvmStartupArchiveTraining.fromMetadata(metadata))
                .withMessageContaining("must not declare a container directory");
    }

    @Test
    void calculatesAotContainerCommandsAndMount() {
        JvmStartupArchiveTraining training = baseImageTraining(JvmStartupOptimizerArchiveType.AOT, "app.aot");

        DefaultDockerContainerLauncher.StartupArchiveContainerPlan plan = DefaultDockerContainerLauncher.StartupArchiveContainerPlan
                .create(training, List.of("-XX:+UnlockDiagnosticVMOptions"));

        assertThat(plan.hostDirectory()).isEqualTo(training.destination().getParent());
        assertThat(plan.containerDirectory()).isEqualTo("/work/training");
        assertThat(plan.recordingJavaToolOptions()).isEqualTo(
                "-XX:AOTMode=record -XX:AOTConfiguration=/work/training/app.aotconf -XX:+UnlockDiagnosticVMOptions");
        assertThat(plan.createJavaToolOptions()).contains(
                "-XX:AOTMode=create -XX:AOTConfiguration=/work/training/app.aotconf "
                        + "-XX:AOTCache=/work/training/app.aot -XX:+UnlockDiagnosticVMOptions");
    }

    @Test
    void calculatesSccContainerCommandAndMountWithoutCreatePhase() {
        JvmStartupArchiveTraining training = baseImageTraining(JvmStartupOptimizerArchiveType.SCC, "app-scc");

        DefaultDockerContainerLauncher.StartupArchiveContainerPlan plan = DefaultDockerContainerLauncher.StartupArchiveContainerPlan
                .create(training, List.of("-Xjit"));

        assertThat(plan.recordingJavaToolOptions())
                .isEqualTo("-Xshareclasses:name=quarkus-app,cacheDir=/work/training/app-scc -Xjit");
        assertThat(plan.createJavaToolOptions()).isEmpty();
    }

    @Test
    void appendsTrainingOptionToInheritedJavaToolOptions() {
        assertThat(DefaultDockerContainerLauncher.appendJavaToolOptions("-Xmx256m", "-XX:AOTMode=record"))
                .isEqualTo("-Xmx256m -XX:AOTMode=record");
        assertThat(DefaultDockerContainerLauncher.appendJavaToolOptions("", "-XX:AOTMode=record"))
                .isEqualTo("-XX:AOTMode=record");
    }

    @Test
    void preparesAndValidatesExactAotOutput() throws IOException {
        Path destination = tempDirectory.resolve("aot").resolve("app.aot");
        JvmStartupArchiveTraining training = new JvmStartupArchiveTraining(JvmStartupOptimizerArchiveType.AOT, destination,
                ExecutionTarget.HOST_JVM, Optional.empty());
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, "stale");
        Files.writeString(training.aotConfigurationDestination(), "stale");

        training.prepareHostOutput();

        assertThat(destination).doesNotExist();
        assertThat(training.aotConfigurationDestination()).doesNotExist();
        assertThatIllegalStateException()
                .isThrownBy(training::validateProducedArchive)
                .withMessageContaining("non-empty file");

        Files.writeString(destination, "archive");
        assertThatNoException().isThrownBy(training::validateProducedArchive);
    }

    @Test
    void rejectsEmptySccOutput() throws IOException {
        Path destination = tempDirectory.resolve("scc").resolve("app-scc");
        JvmStartupArchiveTraining training = new JvmStartupArchiveTraining(JvmStartupOptimizerArchiveType.SCC, destination,
                ExecutionTarget.HOST_JVM, Optional.empty());
        Files.createDirectories(destination.resolve("nested"));
        Files.writeString(destination.resolve("nested/stale-cache"), "stale");

        training.prepareHostOutput();

        assertThat(destination).isDirectory();
        assertThat(destination).isEmptyDirectory();
        assertThatIllegalStateException()
                .isThrownBy(training::validateProducedArchive)
                .withMessageContaining("empty directory");
        Files.writeString(destination.resolve("cache"), "archive");
        assertThatNoException().isThrownBy(training::validateProducedArchive);
    }

    private JvmStartupArchiveTraining baseImageTraining(JvmStartupOptimizerArchiveType type, String fileName) {
        return new JvmStartupArchiveTraining(type, tempDirectory.resolve(fileName), ExecutionTarget.BASE_IMAGE,
                Optional.of("/work/training"));
    }

    private static Properties metadata(String type, Path destination, String executionTarget) {
        Properties properties = new Properties();
        properties.setProperty(JvmStartupArchiveTraining.TYPE_PROPERTY, type);
        properties.setProperty(JvmStartupArchiveTraining.DESTINATION_PROPERTY, destination.toString());
        properties.setProperty(JvmStartupArchiveTraining.EXECUTION_TARGET_PROPERTY, executionTarget);
        return properties;
    }
}
