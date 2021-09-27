package io.quarkus.gradle.nativeimage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.BuildResult;
import io.quarkus.test.devmode.util.DevModeTestUtils;

public class BasicJavaNativeBuildIT extends QuarkusNativeGradleITBase {

    @Test
    public void shouldBuildNativeImage() throws Exception {
        final File projectDir = getProjectDir("basic-java-native-module");

        final BuildResult build = runGradleWrapper(projectDir, "clean", "quarkusBuild", "-Dquarkus.package.type=native");

        assertThat(build.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
        final String buildOutput = build.getOutput();
        // make sure the output log during the build contains some expected logs from the native-image process
        assertThat(buildOutput)
                .withFailMessage("native-image build log is missing certain expected log messages: \n\n %s", buildOutput)
                .contains("(clinit):", "(typeflow):", "[total]:");
        Path nativeImagePath = projectDir.toPath().resolve("build").resolve("foo-1.0.0-SNAPSHOT-runner");
        assertThat(nativeImagePath).exists();
        Process nativeImageProcess = runNativeImage(nativeImagePath.toAbsolutePath().toString());
        try {
            final String response = DevModeTestUtils.getHttpResponse("/hello");
            assertThat(response)
                    .withFailMessage("Response %s for /hello was expected to contain the hello, but didn't", response)
                    .contains("hello");
        } finally {
            nativeImageProcess.destroy();
        }

    }

    private Process runNativeImage(String nativeImage) throws IOException {
        final ProcessBuilder processBuilder = new ProcessBuilder(nativeImage);
        processBuilder.inheritIO();
        return processBuilder.start();
    }
}
