package io.quarkus.test.junit.launcher;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveType;
import io.quarkus.test.common.JvmStartupArchiveTraining;
import io.quarkus.test.common.JvmStartupArchiveTraining.ExecutionTarget;

class DockerContainerLauncherProviderTest {

    @TempDir
    Path tempDirectory;

    @Test
    void acceptsTrainingDirectoryWithinBaseImageWorkingDirectory() {
        JvmStartupArchiveTraining training = training(ExecutionTarget.BASE_IMAGE, "/work/training");

        assertThatNoException()
                .isThrownBy(() -> DockerContainerLauncherProvider.validateStartupArchiveTraining(training,
                        Optional.of("/work")));
    }

    @Test
    void rejectsHostTrainingAndMissingWorkingDirectory() {
        JvmStartupArchiveTraining hostTraining = training(ExecutionTarget.HOST_JVM, null);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DockerContainerLauncherProvider.validateStartupArchiveTraining(hostTraining,
                        Optional.of("/work")))
                .withMessageContaining("BASE_IMAGE");

        JvmStartupArchiveTraining baseImageTraining = training(ExecutionTarget.BASE_IMAGE, "/work/training");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DockerContainerLauncherProvider.validateStartupArchiveTraining(baseImageTraining,
                        Optional.empty()))
                .withMessageContaining("working directory");
    }

    @Test
    void rejectsTrainingDirectoryOutsideBaseImageWorkingDirectory() {
        JvmStartupArchiveTraining training = training(ExecutionTarget.BASE_IMAGE, "/other/training");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> DockerContainerLauncherProvider.validateStartupArchiveTraining(training,
                        Optional.of("/work/")))
                .withMessageContaining("must be within")
                .withMessageContaining("/work/");
    }

    private JvmStartupArchiveTraining training(ExecutionTarget executionTarget, String containerDirectory) {
        return new JvmStartupArchiveTraining(JvmStartupOptimizerArchiveType.AOT, tempDirectory.resolve("app.aot"),
                executionTarget, Optional.ofNullable(containerDirectory));
    }
}
