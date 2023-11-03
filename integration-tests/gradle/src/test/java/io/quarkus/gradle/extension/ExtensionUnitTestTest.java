package io.quarkus.gradle.extension;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.BuildResult;
import io.quarkus.gradle.QuarkusGradleWrapperTestBase;

public class ExtensionUnitTestTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void shouldRunTestWithSuccess() throws Exception {
        gradleConfigurationCache(false);

        File projectDir = getProjectDir("extensions/simple-extension");

        BuildResult buildResult = runGradleWrapper(projectDir, "clean", ":deployment:test", "--no-build-cache");

        assertThat(buildResult.getTasks().get((":deployment:test"))).isEqualTo(BuildResult.SUCCESS_OUTCOME);
    }

}
