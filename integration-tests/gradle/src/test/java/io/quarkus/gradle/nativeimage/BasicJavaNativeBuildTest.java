package io.quarkus.gradle.nativeimage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.devmode.util.DevModeTestUtils;

public class BasicJavaNativeBuildTest extends QuarkusNativeGradleTestBase {

    @Test
    public void shouldBuildNativeImage() throws Exception {
        final File projectDir = getProjectDir("basic-java-native-module");

        BuildResult build = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("clean", "quarkusBuild", "-Dquarkus.package.type=native"))
                .withProjectDir(projectDir)
                .build();

        assertThat(build.task(":quarkusBuild").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
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
