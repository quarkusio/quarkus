package io.quarkus.gradle.nativeimage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.BuildResult;

public class CustomNativeTestSourceSetIT extends QuarkusNativeGradleITBase {

    @Test
    public void runNativeTests() throws Exception {
        final File projectDir = getProjectDir("custom-java-native-sourceset-module");

        final BuildResult build = runGradleWrapper(projectDir, "clean", "testNative");
        assertThat(build.getTasks().get(":testNative")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
    }

}
