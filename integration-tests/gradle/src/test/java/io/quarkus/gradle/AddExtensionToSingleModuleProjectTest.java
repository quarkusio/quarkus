package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class AddExtensionToSingleModuleProjectTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testAddAndRemoveExtension() throws IOException, URISyntaxException, InterruptedException {

        final File projectDir = getProjectDir("add-remove-extension-single-module");

        runGradleWrapper(projectDir, ":addExtension", "--extensions=hibernate-orm");

        final Path build = projectDir.toPath().resolve("build.gradle");
        assertThat(build).exists();
        assertThat(new String(Files.readAllBytes(build)))
                .contains("implementation 'io.quarkus:quarkus-hibernate-orm'")
                .doesNotContain("implementation enforcedPlatform('io.quarkus:quarkus-bom:")
                .doesNotContain("implementation 'io.quarkus:quarkus-bom:");

        runGradleWrapper(projectDir, ":removeExtension", "--extensions=hibernate-orm");
        assertThat(new String(Files.readAllBytes(build))).doesNotContain("implementation 'io.quarkus:quarkus-hibernate-orm'");

    }

    @Test
    public void testRemoveNonExistentExtension() throws IOException, URISyntaxException, InterruptedException {

        final File projectDir = getProjectDir("add-remove-extension-single-module");

        runGradleWrapper(projectDir, "clean", "build");

        final Path build = projectDir.toPath().resolve("build.gradle");
        assertThat(build).exists();
        assertThat(new String(Files.readAllBytes(build))).doesNotContain("implementation 'io.quarkus:quarkus-hibernate-orm'");

        runGradleWrapper(projectDir, ":removeExtension", "--extensions=hibernate-orm");

        assertThat(new String(Files.readAllBytes(build))).doesNotContain("implementation 'io.quarkus:quarkus-hibernate-orm'");

    }

}
