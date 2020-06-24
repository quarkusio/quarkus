package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class FastJarFormatWorksTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testFastJarFormatWorks() throws Exception {

        final File projectDir = getProjectDir("test-that-fast-jar-format-works");

        runGradleWrapper(projectDir, "clean", "build");

        final Path quarkusApp = projectDir.toPath().resolve("build").resolve("quarkus-app");
        assertThat(quarkusApp).exists();
        Path jar = quarkusApp.resolve("quarkus-run.jar");
        assertThat(jar).exists();
    }

}
