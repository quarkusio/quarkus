package io.quarkus.gradle.nativeimage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
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
        assertTrue(buildOutput.contains("(clinit):") && buildOutput.contains("(typeflow):") && buildOutput.contains("[total]:"),
                "native-image build log is missing certain expected log messages");
        Path nativeImagePath = projectDir.toPath().resolve("build").resolve("foo-1.0.0-SNAPSHOT-runner");
        assertThat(nativeImagePath).exists();
        Process nativeImageProcess = runNativeImage(nativeImagePath.toAbsolutePath().toString());
        try {
            final String response = DevModeTestUtils.getHttpResponse("/hello");
            Assertions.assertTrue(response.contains("hello"),
                    "Response " + response + " for /hello was expected to contain the hello, but didn't");
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
