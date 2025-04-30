package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CustomConfigSourcesTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testCustomConfigSources() throws Exception {
        var projectDir = getProjectDir("custom-config-sources");

        // The test is successful, if the build works, see https://github.com/quarkusio/quarkus/issues/36716
        runGradleWrapper(projectDir, "clean", "build", "--no-build-cache");

        var p = projectDir.toPath().resolve("build").resolve("quarkus-app").resolve("quarkus-run.jar");
        assertThat(p).exists();
    }
}
