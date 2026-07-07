package io.quarkus.test.junit.launcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveType;
import io.quarkus.test.common.JvmStartupArchiveTraining;
import io.quarkus.test.common.JvmStartupArchiveTraining.ExecutionTarget;

class JarLauncherProviderTest {

    @TempDir
    Path tempDirectory;

    @Test
    void createsExplicitAotCommandsAtTheRequestedDestination() {
        Path destination = tempDirectory.resolve("training").resolve("app.aot");
        JvmStartupArchiveTraining training = training(JvmStartupOptimizerArchiveType.AOT, destination);

        JarLauncherProvider.RecordingConfig config = JarLauncherProvider.buildRecordingConfig(training,
                List.of("-XX:+UnlockDiagnosticVMOptions"), false, 25);

        assertThat(config.recordingArgs()).containsExactly(
                "-XX:AOTMode=record",
                "-XX:AOTConfiguration=" + destination.resolveSibling("app.aotconf"),
                "-XX:+UnlockDiagnosticVMOptions");
        assertThat(config.postCloseCommand()).containsExactly(
                "-XX:AOTMode=create",
                "-XX:AOTConfiguration=" + destination.resolveSibling("app.aotconf"),
                "-XX:AOTCache=" + destination,
                "-XX:+UnlockDiagnosticVMOptions");
        assertThat(config.aotResultPath()).contains(destination);
        assertThat(config.aotResultDescription()).isEqualTo("AOT file");
    }

    @Test
    void createsExplicitSccCommandAtTheRequestedDestination() {
        Path destination = tempDirectory.resolve("training").resolve("app-scc");
        JvmStartupArchiveTraining training = training(JvmStartupOptimizerArchiveType.SCC, destination);

        JarLauncherProvider.RecordingConfig config = JarLauncherProvider.buildRecordingConfig(training,
                List.of("-Xjit"), true, 25);

        assertThat(config.recordingArgs())
                .containsExactly("-Xshareclasses:name=quarkus-app,cacheDir=" + destination, "-Xjit");
        assertThat(config.postCloseCommand()).isEmpty();
        assertThat(config.aotResultPath()).contains(destination);
        assertThat(config.aotResultDescription()).isEqualTo("SCC cache");
    }

    @Test
    void rejectsSccOnHotSpot() {
        JvmStartupArchiveTraining training = training(JvmStartupOptimizerArchiveType.SCC,
                tempDirectory.resolve("app-scc"));

        assertThatIllegalStateException()
                .isThrownBy(() -> JarLauncherProvider.buildRecordingConfig(training, List.of(), false, 25))
                .withMessageContaining("Semeru/OpenJ9");
    }

    @Test
    void rejectsAotOnSemeru() {
        JvmStartupArchiveTraining training = training(JvmStartupOptimizerArchiveType.AOT,
                tempDirectory.resolve("app.aot"));

        assertThatIllegalStateException()
                .isThrownBy(() -> JarLauncherProvider.buildRecordingConfig(training, List.of(), true, 25))
                .withMessageContaining("HotSpot/OpenJDK");
    }

    @Test
    void rejectsAotBeforeJava25() {
        JvmStartupArchiveTraining training = training(JvmStartupOptimizerArchiveType.AOT,
                tempDirectory.resolve("app.aot"));

        assertThatIllegalStateException()
                .isThrownBy(() -> JarLauncherProvider.buildRecordingConfig(training, List.of(), false, 24))
                .withMessageContaining("25 or newer")
                .withMessageContaining("Java 24");
    }

    private static JvmStartupArchiveTraining training(JvmStartupOptimizerArchiveType type, Path destination) {
        return new JvmStartupArchiveTraining(type, destination, ExecutionTarget.HOST_JVM, Optional.empty());
    }
}
