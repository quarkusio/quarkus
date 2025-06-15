package io.quarkus.extension.gradle.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.extension.gradle.QuarkusExtensionPlugin;
import io.quarkus.extension.gradle.TestUtils;

public class ValidateExtensionTaskTest {

    @TempDir
    File testProjectDir;

    @Test
    public void shouldValidateExtensionDependencies() throws IOException {
        TestUtils.createExtensionProject(testProjectDir, false, List.of("io.quarkus:quarkus-core"),
                List.of("io.quarkus:quarkus-core-deployment"));

        BuildResult validationResult = GradleRunner.create().withPluginClasspath().withProjectDir(testProjectDir)
                .withArguments(QuarkusExtensionPlugin.VALIDATE_EXTENSION_TASK_NAME).build();

        assertThat(
                validationResult.task(":runtime:" + QuarkusExtensionPlugin.VALIDATE_EXTENSION_TASK_NAME).getOutcome())
                .isEqualTo(TaskOutcome.SUCCESS);
    }

    @Test
    public void shouldDetectMissionExtensionDependency() throws IOException {
        TestUtils.createExtensionProject(testProjectDir, false, List.of("io.quarkus:quarkus-jdbc-h2"), List.of());

        BuildResult validationResult = GradleRunner.create().withPluginClasspath().withProjectDir(testProjectDir)
                .withArguments(QuarkusExtensionPlugin.VALIDATE_EXTENSION_TASK_NAME).buildAndFail();

        assertThat(
                validationResult.task(":runtime:" + QuarkusExtensionPlugin.VALIDATE_EXTENSION_TASK_NAME).getOutcome())
                .isEqualTo(TaskOutcome.FAILED);
        assertThat(validationResult.getOutput()).contains("Quarkus Extension Dependency Verification Error");
        assertThat(validationResult.getOutput())
                .contains("The following deployment artifact(s) were found to be missing in the deployment module:");
        assertThat(validationResult.getOutput()).contains("- io.quarkus:quarkus-jdbc-h2-deployment");
    }

    @Test
    public void shouldDetectInvalidRuntimeDependency() throws IOException {
        TestUtils.createExtensionProject(testProjectDir, false,
                List.of("io.quarkus:quarkus-core", "io.quarkus:quarkus-core-deployment"), List.of());

        BuildResult validationResult = GradleRunner.create().withPluginClasspath().withProjectDir(testProjectDir)
                .withArguments(QuarkusExtensionPlugin.VALIDATE_EXTENSION_TASK_NAME).buildAndFail();

        assertThat(
                validationResult.task(":runtime:" + QuarkusExtensionPlugin.VALIDATE_EXTENSION_TASK_NAME).getOutcome())
                .isEqualTo(TaskOutcome.FAILED);
        assertThat(validationResult.getOutput()).contains("Quarkus Extension Dependency Verification Error");
        assertThat(validationResult.getOutput())
                .contains("The following deployment artifact(s) appear on the runtime classpath:");
        assertThat(validationResult.getOutput()).contains("- io.quarkus:quarkus-core-deployment");
    }

    @Test
    public void shouldSkipValidationWhenDisabled() throws IOException {
        TestUtils.createExtensionProject(testProjectDir, true,
                List.of("io.quarkus:quarkus-core", "io.quarkus:quarkus-core-deployment"), List.of());

        BuildResult validationResult = GradleRunner.create().withPluginClasspath().withProjectDir(testProjectDir)
                .withArguments(QuarkusExtensionPlugin.VALIDATE_EXTENSION_TASK_NAME).build();

        assertThat(
                validationResult.task(":runtime:" + QuarkusExtensionPlugin.VALIDATE_EXTENSION_TASK_NAME).getOutcome())
                .isEqualTo(TaskOutcome.SKIPPED);
    }
}
