package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class AddExtensionToSingleModuleKtsProjectTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testAddAndRemoveExtension() throws IOException, URISyntaxException, InterruptedException {

        final File projectDir = getProjectDir("add-remove-extension-single-module-kts");

        runGradleWrapper(projectDir, ":addExtension", "--extensions=openshift");

        final Path buildKts = projectDir.toPath().resolve("build.gradle.kts");
        assertThat(buildKts).exists();
        assertThat(new String(Files.readAllBytes(buildKts)))
                .contains("implementation(\"io.quarkus:quarkus-openshift\")")
                .doesNotContain("implementation(enforcedPlatform(\"io.quarkus:quarkus-bom:")
                .doesNotContain("implementation(\"io.quarkus:quarkus-bom:");

        runGradleWrapper(projectDir, ":removeExtension", "--extensions=openshift");

        assertThat(new String(Files.readAllBytes(buildKts)))
                .doesNotContain("implementation(\"io.quarkus:quarkus-openshift\")");

        assertThat(projectDir.toPath().resolve("build.gradle")).doesNotExist();
    }

    @Test
    public void testRemoveNonExistentExtension() throws IOException, URISyntaxException, InterruptedException {

        final File projectDir = getProjectDir("add-remove-extension-single-module-kts");

        runGradleWrapper(projectDir, "clean", "build");

        final Path buildKts = projectDir.toPath().resolve("build.gradle.kts");
        assertThat(buildKts).exists();
        assertThat(new String(Files.readAllBytes(buildKts)))
                .doesNotContain("implementation(\"io.quarkus:quarkus-hibernate-orm\")");

        runGradleWrapper(projectDir, ":removeExtension", "--extensions=hibernate-orm");

        assertThat(new String(Files.readAllBytes(buildKts)))
                .doesNotContain("implementation(\"io.quarkus:quarkus-hibernate-orm\")");

        assertThat(projectDir.toPath().resolve("build.gradle")).doesNotExist();
    }

}
