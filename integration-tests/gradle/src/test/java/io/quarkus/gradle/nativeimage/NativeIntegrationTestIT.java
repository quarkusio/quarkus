package io.quarkus.gradle.nativeimage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.gradle.BuildResult;

@ExtendWith(SoftAssertionsExtension.class)
public class NativeIntegrationTestIT extends QuarkusNativeGradleITBase {
    @InjectSoftAssertions
    SoftAssertions soft;

    @Test
    public void nativeTestShouldRunIntegrationTest() throws Exception {
        File projectDir = getProjectDir("it-test-basic-project");

        BuildResult testResult = runGradleWrapper(projectDir, "clean", "testNative");

        soft.assertThat(testResult.getTasks().get(":testNative")).isIn(BuildResult.SUCCESS_OUTCOME, BuildResult.FROM_CACHE);
        soft.assertThat(projectDir.toPath().resolve("build/code-with-quarkus-1.0.0-SNAPSHOT-runner")).isRegularFile()
                .isExecutable();
    }

    @Test
    public void runNativeTestsWithOutputName() throws Exception {
        final File projectDir = getProjectDir("it-test-basic-project");

        final BuildResult testResult = runGradleWrapper(projectDir, "clean", "testNative",
                "-Dquarkus.package.output-name=test");
        soft.assertThat(testResult.getTasks().get(":testNative")).isIn(BuildResult.SUCCESS_OUTCOME, BuildResult.FROM_CACHE);
        soft.assertThat(projectDir.toPath().resolve("build/test-runner")).isRegularFile().isExecutable();
    }

    @Test
    public void runNativeTestsWithoutRunnerSuffix() throws Exception {
        final File projectDir = getProjectDir("it-test-basic-project");

        final BuildResult testResult = runGradleWrapper(projectDir, "clean", "testNative",
                "-Dquarkus.package.jar.add-runner-suffix=false");
        soft.assertThat(testResult.getTasks().get(":testNative")).isIn(BuildResult.SUCCESS_OUTCOME, BuildResult.FROM_CACHE);
        soft.assertThat(projectDir.toPath().resolve("build/code-with-quarkus-1.0.0-SNAPSHOT")).isRegularFile()
                .isExecutable();
    }

}
