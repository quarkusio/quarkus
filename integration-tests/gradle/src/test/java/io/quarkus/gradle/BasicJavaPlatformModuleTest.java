package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class BasicJavaPlatformModuleTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicPlatformModuleBuild() throws Exception {

        final File projectDir = getProjectDir("basic-java-platform-module");

        runGradleWrapper(projectDir, "clean", ":application:build");

        Path p = projectDir.toPath().resolve("application").resolve("build").resolve("libs");
        assertThat(p).exists();
        assertThat(p.resolve("application-1.0.0-SNAPSHOT.jar")).exists();

        p = projectDir.toPath().resolve("application").resolve("build").resolve("quarkus-app").resolve("quarkus-run.jar");
        assertThat(p).exists();
    }
}
