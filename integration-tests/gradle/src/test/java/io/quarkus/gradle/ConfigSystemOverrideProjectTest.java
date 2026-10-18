package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class ConfigSystemOverrideProjectTest extends QuarkusGradleWrapperTestBase {
    @Test
    void configSystemOverrideProject() throws Exception {
        File projectDir = getProjectDir("custom-config-sources");

        runGradleWrapper(projectDir, "clean", "build", "--no-build-cache", "-Dquarkus.package.jar.type=fast-jar",
                "-Pquarkus.package.jar.type=uber-jar");

        var p = projectDir.toPath().resolve("build").resolve("quarkus-app").resolve("quarkus-run.jar");
        assertThat(p).exists();
    }
}
