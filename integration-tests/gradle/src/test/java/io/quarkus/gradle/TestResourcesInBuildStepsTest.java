package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.LaunchMode;

public class TestResourcesInBuildStepsTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("test-resources-in-build-steps");

        runGradleWrapper(projectDir, "clean", ":application:publishAcmeExt");
        runGradleWrapper(projectDir, "build");

        final Path buildDir = projectDir.toPath().resolve("application").resolve("build");
        final Path libDir = buildDir.resolve("quarkus-app").resolve("lib").resolve("main");
        assertThat(libDir).exists();
        assertThat(libDir.resolve("org.acme.runtime-1.0-SNAPSHOT.jar")).exists();

        final Path prodResourcesTxt = buildDir.resolve(LaunchMode.NORMAL + "-resources.txt");
        assertThat(prodResourcesTxt).hasContent("main");

        final Path testResourcesTxt = buildDir.resolve(LaunchMode.TEST + "-resources.txt");
        assertThat(testResourcesTxt).hasContent("test");
    }
}
