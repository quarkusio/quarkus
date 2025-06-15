package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;

public class BasicCompositeBuildQuarkusBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("basic-composite-build-project");

        final File appProperties = new File(projectDir, "application/gradle.properties");
        final File libsProperties = new File(projectDir, "libraries/gradle.properties");
        final Path projectProperties = projectDir.toPath().resolve("gradle.properties");

        try {
            Files.copy(projectProperties, appProperties.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(projectProperties, libsProperties.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to copy gradle.properties file", e);
        }

        runGradleWrapper(projectDir, ":application:quarkusBuild");

        final Path libA = projectDir.toPath().resolve("libraries").resolve("libraryA").resolve("build").resolve("libs");
        assertThat(libA).exists();
        assertThat(libA.resolve("libraryA-1.0-SNAPSHOT.jar")).exists();

        final Path libB = projectDir.toPath().resolve("libraries").resolve("libraryB").resolve("build").resolve("libs");
        assertThat(libB).exists();
        assertThat(libB.resolve("libraryB-1.0-SNAPSHOT.jar")).exists();

        final Path applicationLib = projectDir.toPath().resolve("application").resolve("build").resolve("quarkus-app");
        assertThat(applicationLib.resolve("lib").resolve("main").resolve("org.acme.libs.libraryA-1.0-SNAPSHOT.jar"))
                .exists();
        assertThat(applicationLib.resolve("lib").resolve("main").resolve("org.acme.libs.libraryB-1.0-SNAPSHOT.jar"))
                .exists();

        assertThat(applicationLib.resolve("app").resolve("application-1.0-SNAPSHOT.jar")).exists();
    }
}
