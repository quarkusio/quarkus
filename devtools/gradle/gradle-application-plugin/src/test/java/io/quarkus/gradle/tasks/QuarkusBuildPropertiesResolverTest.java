package io.quarkus.gradle.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.QuarkusPlugin;
import io.quarkus.gradle.extension.QuarkusPluginExtension;

class QuarkusBuildPropertiesResolverTest {

    @TempDir
    Path projectDir;

    @Test
    void resolvesDefaultsAndAddsApplicationProperties() throws IOException {
        Project project = buildProject(projectDir);
        QuarkusPluginExtension extension = project.getExtensions().getByType(QuarkusPluginExtension.class);
        extension.getFinalName().set("app-final-name");
        writeApplicationProperties("""
                quarkus.http.port=9090
                quarkus.package.jar.type=uber-jar
                quarkus.package.jar.runner-suffix=-custom
                foo=bar
                """);

        Map<String, String> properties = QuarkusPropertiesResolver.resolve(project, extension);

        assertThat(properties)
                .containsEntry("quarkus.native.enabled", "false")
                .containsEntry("quarkus.package.jar.enabled", "true")
                .containsEntry("quarkus.package.output-directory", QuarkusPlugin.DEFAULT_OUTPUT_DIRECTORY)
                .containsEntry("quarkus.package.output-name", "app-final-name")
                .containsEntry("quarkus.package.jar.add-runner-suffix", "true")
                .containsEntry("quarkus.native.sources-only", "false")
                .containsEntry("quarkus.package.jar.type", "fast-jar")
                .containsEntry("quarkus.package.jar.runner-suffix", "-runner")
                .containsEntry("quarkus.http.port", "9090");
        assertThat(properties).doesNotContainKey("foo");
    }

    @Test
    void systemPropertiesOverrideDefaults() throws IOException {
        Project project = buildProject(projectDir);
        QuarkusPluginExtension extension = project.getExtensions().getByType(QuarkusPluginExtension.class);
        extension.getFinalName().set("default-name");
        writeApplicationProperties("quarkus.http.host=0.0.0.0");

        setSystemProperty("quarkus.native.enabled", "true");
        setSystemProperty("quarkus.package.output-name", "from-system");
        setSystemProperty("quarkus.package.jar.type", "legacy-jar");
        setSystemProperty("quarkus.package.jar.runner-suffix", "-sys");

        try {
            Map<String, String> properties = QuarkusPropertiesResolver.resolve(project, extension);

            assertThat(properties)
                    .containsEntry("quarkus.native.enabled", "true")
                    .containsEntry("quarkus.package.output-name", "from-system")
                    .containsEntry("quarkus.package.jar.type", "legacy-jar")
                    .containsEntry("quarkus.package.jar.runner-suffix", "-sys")
                    .containsEntry("quarkus.http.host", "0.0.0.0");
        } finally {
            clearSystemProperty("quarkus.native.enabled");
            clearSystemProperty("quarkus.package.output-name");
            clearSystemProperty("quarkus.package.jar.type");
            clearSystemProperty("quarkus.package.jar.runner-suffix");
        }
    }

    private Project buildProject(Path projectDir) throws IOException {
        Files.createDirectories(projectDir.resolve("src/main/resources"));
        Project project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();
        project.setVersion("1.0.0");
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(QuarkusPlugin.ID);
        return project;
    }

    private void writeApplicationProperties(String contents) throws IOException {
        Path propertiesPath = projectDir.resolve("src/main/resources/application.properties");
        Files.createDirectories(propertiesPath.getParent());
        Files.writeString(propertiesPath, contents);
    }

    private static void setSystemProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private static void clearSystemProperty(String key) {
        System.clearProperty(key);
    }
}
