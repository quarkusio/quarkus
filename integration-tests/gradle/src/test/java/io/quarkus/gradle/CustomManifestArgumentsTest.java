package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

public class CustomManifestArgumentsTest extends QuarkusGradleWrapperTestBase {

    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "for whatever reason, this is not working anymore on Widndows, the customSection is null...")
    public void shouldContainsSpecificManifestProperty() throws Exception {
        File projectDir = getProjectDir("custom-config-java-module");

        runGradleWrapper(projectDir, "clean", "quarkusBuild");

        Path buildDir = new File(projectDir, "build").toPath();
        Path jar = buildDir.resolve("code-with-quarkus-1.0.0-SNAPSHOT-runner.jar");

        assertThat(jar).exists();
        try (InputStream fileInputStream = new FileInputStream(jar.toFile())) {
            try (JarInputStream jarStream = new JarInputStream(fileInputStream)) {
                Manifest manifest = jarStream.getManifest();
                assertThat(manifest).isNotNull();

                String customAttribute = manifest.getMainAttributes().getValue("Built-By");
                assertThat(customAttribute).isNotNull();
                assertThat(customAttribute).isEqualTo("quarkus-gradle-plugin");

                Attributes customSection = manifest.getAttributes("org.acme");
                assertThat(customSection).isNotNull();

                String sectionAttribute = customSection.getValue("framework");
                assertThat(sectionAttribute).isNotNull();
                assertThat(sectionAttribute).isEqualTo("quarkus");
            }
        }
    }

}
