package io.quarkus.test.junit.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DockerContainerLauncherProviderTest {

    private static final Path BUILD_OUTPUT_DIRECTORY = Path.of("checkout", "build").toAbsolutePath();

    @Test
    void relativeOutputDirectoryIsResolvedAgainstTheBuildOutputDirectory() {
        assertThat(DockerContainerLauncherProvider.outputTargetDirectory(BUILD_OUTPUT_DIRECTORY, "."))
                .isEqualTo(BUILD_OUTPUT_DIRECTORY.toString());
    }

    @Test
    void absoluteOutputDirectoryIsKept() {
        // as recorded by earlier versions
        String elsewhere = Path.of("elsewhere", "build").toAbsolutePath().toString();

        assertThat(DockerContainerLauncherProvider.outputTargetDirectory(BUILD_OUTPUT_DIRECTORY, elsewhere))
                .isEqualTo(elsewhere);
    }

    @Test
    void missingOutputDirectoryStaysMissing() {
        assertThat(DockerContainerLauncherProvider.outputTargetDirectory(BUILD_OUTPUT_DIRECTORY, null)).isNull();
    }
}
