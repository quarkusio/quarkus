package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class BasicJavaLibraryModuleTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("basic-java-library-module");

        runGradleWrapper(projectDir, "clean", ":application:build");

        final Path commonLibs = projectDir.toPath().resolve("library").resolve("build").resolve("libs");
        assertThat(commonLibs).exists();
        assertThat(commonLibs.resolve("library-1.0.0-SNAPSHOT.jar")).exists();

        final Path applicationLib = projectDir.toPath().resolve("application").resolve("build").resolve("quarkus-app")
                .resolve("lib").resolve("main");
        assertThat(applicationLib).exists();
        assertThat(applicationLib.resolve("org.acme.library-1.0.0-SNAPSHOT.jar")).exists();
    }
}
