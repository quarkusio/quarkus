package io.quarkus.extension.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Properties;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.util.ZipUtils;

public class QuarkusExtensionPluginTest {

    @TempDir
    File testProjectDir;
    private File buildFile;

    @BeforeEach
    public void setupProject() throws IOException {
        buildFile = new File(testProjectDir, "build.gradle");

        File settingFile = new File(testProjectDir, "settings.gradle");
        String settingsContent = "rootProject.name = 'test'";
        TestUtils.writeFile(settingFile, settingsContent);
    }

    @Test
    public void jarShouldContainsExtensionPropertiesFile() throws IOException {
        TestUtils.writeFile(buildFile, TestUtils.DEFAULT_BUILD_GRADLE_CONTENT);
        BuildResult jarResult = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir)
                .withArguments("jar", "-S")
                .build();

        assertThat(jarResult.task(":jar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(jarResult.task(":" + QuarkusExtensionPlugin.EXTENSION_DESCRIPTOR_TASK_NAME).getOutcome())
                .isEqualTo(TaskOutcome.SUCCESS);

        File jarFile = new File(testProjectDir, "build/libs/test-1.0.0.jar");
        assertThat(jarFile).exists();
        assertThat(jarFile).satisfies(f -> {
            try (FileSystem jarFs = ZipUtils.newFileSystem(f.toPath())) {
                Path descriptorPath = jarFs.getPath(BootstrapConstants.DESCRIPTOR_PATH);
                assertThat(descriptorPath).exists();

                Properties extensionProperty = TestUtils.readPropertyFile(descriptorPath);
                assertThat(extensionProperty).containsEntry("deployment-artifact", "org.acme:test-deployment:1.0.0");

            } catch (IOException e) {
                Assertions.fail("Unable to read jar file");
            }
        });
    }

    @Test
    public void pluginShouldAddAnnotationProcessor() throws IOException {
        TestUtils.writeFile(buildFile, TestUtils.DEFAULT_BUILD_GRADLE_CONTENT);
        BuildResult dependencies = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir)
                .withArguments("dependencies", "--configuration", "annotationProcessor")
                .build();

        assertThat(dependencies.getOutput()).contains(QuarkusExtensionPlugin.QUARKUS_ANNOTATION_PROCESSOR);
    }

    @Test
    public void pluginShouldAddAnnotationProcessorToDeploymentModule() throws IOException {
        TestUtils.createExtensionProject(testProjectDir);
        BuildResult dependencies = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir)
                .withArguments(":deployment:dependencies", "--configuration", "annotationProcessor")
                .build();
        assertThat(dependencies.getOutput()).contains(QuarkusExtensionPlugin.QUARKUS_ANNOTATION_PROCESSOR);
    }
}
