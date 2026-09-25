package io.quarkus.gradle.nativeimage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.BuildResult;
import io.quarkus.test.devmode.util.DevModeClient;

public class BasicJavaNativeBuildIT extends QuarkusNativeGradleITBase {

    public static final String NATIVE_IMAGE_NAME = "foo-1.0.0-SNAPSHOT-runner";
    private DevModeClient devModeClient = new DevModeClient();

    @Test
    public void shouldBuildNativeImage() throws Exception {
        final File projectDir = getProjectDir("basic-java-native-module");

        final BuildResult build = runGradleWrapper(projectDir, "clean", "buildNative");

        assertThat(build.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
        final String buildOutput = build.getOutput();
        // make sure the output log during the build contains some expected logs from the native-image process
        CharSequence[] expectedOutput = new CharSequence[] { "Initializing...", "Performing analysis...",
                "Finished generating '" + NATIVE_IMAGE_NAME + "' in" };
        assertThat(buildOutput)
                .withFailMessage("native-image build log is missing certain expected log messages: \n\n %s", buildOutput)
                .contains(expectedOutput);
        Path nativeImagePath = projectDir.toPath().resolve("build").resolve(NATIVE_IMAGE_NAME);
        assertThat(nativeImagePath).exists();
        Process nativeImageProcess = runNativeImage(nativeImagePath.toAbsolutePath().toString());
        try {
            final String response = devModeClient.getHttpResponse("/hello");
            assertThat(response)
                    .withFailMessage("Response %s for /hello was expected to contain the hello, but didn't", response)
                    .contains("hello");
        } finally {
            nativeImageProcess.destroy();
        }

    }

    @Test
    public void shouldBuildNativeImageWithCustomName() throws Exception {
        final File projectDir = getProjectDir("basic-java-native-module");

        final BuildResult build = runGradleWrapper(projectDir, "clean", "buildNative",
                "-Dquarkus.package.output-name=test");

        assertThat(build.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
        final String buildOutput = build.getOutput();
        // make sure the output log during the build contains some expected logs from the native-image process
        CharSequence[] expectedOutput = new CharSequence[] { "Initializing...", "Performing analysis...",
                "Finished generating 'test-runner' in" };
        assertThat(buildOutput)
                .withFailMessage("native-image build log is missing certain expected log messages: \n\n %s", buildOutput)
                .contains(expectedOutput)
                .doesNotContain("Finished generating '" + NATIVE_IMAGE_NAME + "' in");
        Path nativeImagePath = projectDir.toPath().resolve("build").resolve("test-runner");
        assertThat(nativeImagePath).exists();
        Process nativeImageProcess = runNativeImage(nativeImagePath.toAbsolutePath().toString());
        try {
            final String response = devModeClient.getHttpResponse("/hello");
            assertThat(response)
                    .withFailMessage("Response %s for /hello was expected to contain the hello, but didn't", response)
                    .contains("hello");
        } finally {
            nativeImageProcess.destroy();
        }

    }

    @Test
    public void shouldBuildNativeImageWithCustomNameWithoutSuffix() throws Exception {
        final File projectDir = getProjectDir("basic-java-native-module");

        final BuildResult build = runGradleWrapper(projectDir, "clean", "buildNative",
                "-Dquarkus.package.output-name=test", "-Dquarkus.package.jar.add-runner-suffix=false");

        assertThat(build.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
        final String buildOutput = build.getOutput();
        // make sure the output log during the build contains some expected logs from the native-image process
        CharSequence[] expectedOutput = new CharSequence[] { "Initializing...", "Performing analysis...",
                "Finished generating 'test' in" };
        assertThat(buildOutput)
                .withFailMessage("native-image build log is missing certain expected log messages: \n\n %s", buildOutput)
                .contains(expectedOutput)
                .doesNotContain("Finished generating '" + NATIVE_IMAGE_NAME + "' in");
        Path nativeImagePath = projectDir.toPath().resolve("build").resolve("test");
        assertThat(nativeImagePath).exists();
        Process nativeImageProcess = runNativeImage(nativeImagePath.toAbsolutePath().toString());
        try {
            final String response = devModeClient.getHttpResponse("/hello");
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
