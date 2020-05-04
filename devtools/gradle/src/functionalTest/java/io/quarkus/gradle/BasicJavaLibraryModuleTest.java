package io.quarkus.gradle;

import java.io.File;
import java.nio.file.Path;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class BasicJavaLibraryModuleTest extends QuarkusGradleTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("basic-java-library-module");

        GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments("clean", ":application:build"))
                .withProjectDir(projectDir)
                .build();

        final Path commonLibs = projectDir.toPath().resolve("library").resolve("build").resolve("libs");
        assertThat(commonLibs).exists();
        assertThat(commonLibs.resolve("library-1.0.0-SNAPSHOT.jar")).exists();

        final Path applicationLib = projectDir.toPath().resolve("application").resolve("build").resolve("lib");
        assertThat(applicationLib).exists();
        assertThat(applicationLib.resolve("org.acme.library-1.0.0-SNAPSHOT.jar")).exists();
    }
}
