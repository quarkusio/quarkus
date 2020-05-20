package io.quarkus.gradle;

import java.io.File;
import java.nio.file.Path;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;

import static org.assertj.core.api.Assertions.assertThat;


public class TestResourcesInBuildStepsTest extends QuarkusGradleTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("test-resources-in-build-steps");

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("clean", "build"))
                .withProjectDir(projectDir)
                .build();

        final Path buildDir = projectDir.toPath().resolve("application").resolve("build");

        final Path libDir = buildDir.resolve("lib");
        assertThat(libDir).exists();
        assertThat(libDir.resolve("org.acme.runtime-1.0-SNAPSHOT.jar")).exists();

        final Path prodResourcesTxt = buildDir.resolve(LaunchMode.NORMAL + "-resources.txt");
        assertThat(prodResourcesTxt).hasContent("main");


        final Path testResourcesTxt = buildDir.resolve(LaunchMode.TEST + "-resources.txt");
        assertThat(testResourcesTxt).hasContent("test");
    }
}
