package io.quarkus.gradle.application.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.application.QuarkusApplicationPlugin;
import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;

class QuarkusApplicationShowEffectiveConfigTaskTest {

    private static final String EXTERNAL_SYSTEM_PROPERTY = "quarkus.diagnostics.external-secret";
    private static final String EXTERNAL_SYSTEM_PROPERTY_VALUE = "should-not-be-reported";

    @TempDir
    Path projectDirectory;

    @Test
    void reportsKeysAndSourcesWithoutValuesByDefault() throws Exception {
        Path resources = Files.createDirectories(projectDirectory.resolve("src/main/resources"));
        Files.writeString(resources.resolve("application.properties"), "quarkus.from-file=file-value\n");

        Project project = ProjectBuilder.builder()
                .withName("diagnostic-app")
                .withProjectDir(projectDirectory.toFile())
                .build();
        project.setVersion("1.2.3");
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.getQuarkusBuildProperties().put("quarkus.shared", "common-value");
        extension.builds(builds -> builds.fastJar("fast", fast -> {
            fast.getQuarkusBuildProperties().put("quarkus.output", "output-value");
            fast.getManifest().getAttributes().put("Built-By", "manifest-dsl");
            fast.getManifest().sections(sections -> sections.section("Specification",
                    section -> section.getAttributes().put("Specification-Title", "Diagnostic App")));
        }));

        QuarkusApplicationShowEffectiveConfigTask task = (QuarkusApplicationShowEffectiveConfigTask) project.getTasks()
                .getByName("quarkusFastShowEffectiveConfig");

        String diagnostics = task.diagnostics();

        assertThat(diagnostics)
                .startsWith("Effective Quarkus configuration for named build 'fast' (FAST_JAR, profile 'prod'):")
                .contains("    quarkus.from-file    source=application.properties")
                .contains("    quarkus.output    source=Quarkus build DSL")
                .contains("    quarkus.package.jar.manifest.attributes.\"Built-By\"    source=task configuration")
                .contains("    quarkus.package.jar.manifest.sections.\"Specification\"."
                        + "\"Specification-Title\"    source=task configuration")
                .contains("    quarkus.package.output-timestamp    source=Quarkus build DSL")
                .contains("    quarkus.shared    source=Quarkus build DSL")
                .contains("Build-owned forced keys:")
                .contains("    quarkus.package.jar.type")
                .contains("Configuration sources:")
                .contains("application.properties")
                .contains("./gradlew :quarkusFastShowEffectiveConfig --show-values")
                .doesNotContain("file-value", "output-value", "manifest-dsl", "Diagnostic App",
                        "1970-01-02T00:00:00Z", "common-value", "fast-jar");
        assertThat(diagnostics.indexOf("    quarkus.output    source="))
                .isLessThan(diagnostics.indexOf("    quarkus.shared    source="));
    }

    @Test
    void reportsCapturedValuesOnlyWhenExplicitlyRequested() throws Exception {
        Path resources = Files.createDirectories(projectDirectory.resolve("src/main/resources"));
        Files.writeString(resources.resolve("application.properties"), "quarkus.from-file=line-one\\nline-two\n");

        Project project = ProjectBuilder.builder()
                .withName("diagnostic-app")
                .withProjectDir(projectDirectory.toFile())
                .build();
        project.setVersion("1.2.3");
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.builds(builds -> builds.fastJar("fast"));

        QuarkusApplicationShowEffectiveConfigTask task = (QuarkusApplicationShowEffectiveConfigTask) project.getTasks()
                .getByName("quarkusFastShowEffectiveConfig");
        task.setShowValues(true);

        assertThat(task.diagnostics())
                .contains("    quarkus.from-file=line-one\\nline-two    source=application.properties")
                .contains("Build-owned forced values:")
                .contains("    quarkus.package.jar.type=fast-jar")
                .doesNotContain("\nline-two");
    }

    @Test
    void omitsExternallyProvidedSystemPropertyValues() {
        String previousValue = System.getProperty(EXTERNAL_SYSTEM_PROPERTY);
        System.setProperty(EXTERNAL_SYSTEM_PROPERTY, EXTERNAL_SYSTEM_PROPERTY_VALUE);
        try {
            Project project = ProjectBuilder.builder()
                    .withName("diagnostic-app")
                    .withProjectDir(projectDirectory.toFile())
                    .build();
            project.setVersion("1.2.3");
            project.getPluginManager().apply(QuarkusApplicationPlugin.class);

            QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
            extension.builds(builds -> builds.fastJar("fast"));

            QuarkusApplicationShowEffectiveConfigTask task = (QuarkusApplicationShowEffectiveConfigTask) project.getTasks()
                    .getByName("quarkusFastShowEffectiveConfig");

            assertThat(task.diagnostics())
                    .contains("system-property/environment")
                    .contains("omitted>")
                    .doesNotContain(EXTERNAL_SYSTEM_PROPERTY)
                    .doesNotContain(EXTERNAL_SYSTEM_PROPERTY_VALUE);
        } finally {
            if (previousValue == null) {
                System.clearProperty(EXTERNAL_SYSTEM_PROPERTY);
            } else {
                System.setProperty(EXTERNAL_SYSTEM_PROPERTY, previousValue);
            }
        }
    }
}
