package io.quarkus.gradle;

import java.io.File;
import java.nio.file.Path;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class BasicJavaPlatformModuleTest extends QuarkusGradleTestBase {

    @Test
    public void testBasicPlatformModuleBuild() throws Exception {

        final File projectDir = getProjectDir("basic-java-platform-module");

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("clean", ":application:build"))
                .withProjectDir(projectDir)
                .build();

        Path p = projectDir.toPath().resolve("application").resolve("build").resolve("libs");
        assertThat(p).exists();
        assertThat(p.resolve("application-1.0.0-SNAPSHOT.jar")).exists();

        p = projectDir.toPath().resolve("application").resolve("build").resolve("application-1.0.0-SNAPSHOT-runner.jar");
        assertThat(p).exists();
    }
}
