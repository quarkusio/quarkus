package io.quarkus.gradle.nativeimage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.BuildResult;

public class NativeIntegrationTestIT extends QuarkusNativeGradleITBase {

    @Test
    public void nativeTestShouldRunIntegrationTest() throws Exception {
        File projectDir = getProjectDir("it-test-basic-project");

        BuildResult testResult = runGradleWrapper(projectDir, "clean", "testNative");

        assertThat(testResult.getTasks().get(":testNative")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
    }

}
