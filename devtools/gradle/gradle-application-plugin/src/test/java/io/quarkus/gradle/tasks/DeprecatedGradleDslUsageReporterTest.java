package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.QuarkusPlugin.QUARKUS_BUILD_TASK_NAME;
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

class DeprecatedGradleDslUsageReporterTest {

    @Test
    @SuppressWarnings("removal")
    void nativeArgsWritesDeprecatedDslDiagnostics(@TempDir Path projectDir) throws IOException {
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDir.toFile())
                .build();
        project.getPluginManager().apply(QuarkusPlugin.ID);

        QuarkusPluginExtension extension = project.getExtensions().getByType(QuarkusPluginExtension.class);
        extension.setFinalName("custom-name");

        QuarkusBuild quarkusBuild = (QuarkusBuild) project.getTasks().getByName(QUARKUS_BUILD_TASK_NAME);
        quarkusBuild.nativeArgs(args -> {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Map<String, Object> mutableArgs = (Map) args;
            mutableArgs.put("containerBuild", true);
        });
        quarkusBuild.reportDeprecatedDslUsages();

        Path report = projectDir.resolve("build").resolve(DeprecatedGradleDslUsageReporter.REPORT_PATH);
        assertThat(report).exists();
        assertThat(Files.readString(report))
                .contains("Deprecated Quarkus Gradle DSL/API usage detected")
                .contains("QuarkusBuild.nativeArgs(Action<Map<String, ?>>)")
                .contains("QuarkusPluginExtension.setFinalName(String)")
                .contains("Use quarkus.nativeArguments instead")
                .contains(DeprecatedGradleDslUsageReporter.MIGRATION_GUIDE_URL)
                .contains("nativeArgsWritesDeprecatedDslDiagnostics");
    }
}
