package io.quarkus.extension.gradle.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.extension.gradle.QuarkusExtensionPlugin;
import io.quarkus.extension.gradle.TestUtils;

public class ExtensionDescriptorTest {

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
    public void shouldCreateFileWithDefaultValues() throws IOException {
        TestUtils.writeFile(buildFile, TestUtils.DEFAULT_BUILD_GRADLE_CONTENT);
        TestUtils.runExtensionDescriptorTask(testProjectDir);

        File extensionPropertiesFile = new File(testProjectDir, "build/resources/main/META-INF/quarkus-extension.properties");
        assertThat(extensionPropertiesFile).exists();

        Properties extensionProperty = TestUtils.readPropertyFile(extensionPropertiesFile.toPath());
        assertThat(extensionProperty).containsEntry("deployment-artifact", "org.acme:test-deployment:1.0.0");
    }

    @Test
    public void shouldUseCustomDeploymentArtifactName() throws IOException {
        String buildFileContent = TestUtils.DEFAULT_BUILD_GRADLE_CONTENT
                + QuarkusExtensionPlugin.EXTENSION_DESCRIPTOR_TASK_NAME + " { " +
                "deployment = 'custom.group:custom-deployment-artifact:0.1.0'" +
                "}";
        TestUtils.writeFile(buildFile, buildFileContent);
        TestUtils.runExtensionDescriptorTask(testProjectDir);

        File extensionPropertiesFile = new File(testProjectDir, "build/resources/main/META-INF/quarkus-extension.properties");
        assertThat(extensionPropertiesFile).exists();

        Properties extensionProperty = TestUtils.readPropertyFile(extensionPropertiesFile.toPath());
        assertThat(extensionProperty).containsEntry("deployment-artifact", "custom.group:custom-deployment-artifact:0.1.0");
    }

    @Test
    public void shouldContainsConditionalDependencies() throws IOException {
        String buildFileContent = TestUtils.DEFAULT_BUILD_GRADLE_CONTENT
                + QuarkusExtensionPlugin.EXTENSION_DESCRIPTOR_TASK_NAME + " { " +
                "conditionalDependencies= ['org.acme:ext-a:0.1.0', 'org.acme:ext-b:0.1.0']" +
                "}";
        TestUtils.writeFile(buildFile, buildFileContent);
        TestUtils.runExtensionDescriptorTask(testProjectDir);

        File extensionPropertiesFile = new File(testProjectDir, "build/resources/main/META-INF/quarkus-extension.properties");
        assertThat(extensionPropertiesFile).exists();

        Properties extensionProperty = TestUtils.readPropertyFile(extensionPropertiesFile.toPath());
        assertThat(extensionProperty).containsEntry("deployment-artifact", "org.acme:test-deployment:1.0.0");
        assertThat(extensionProperty).containsEntry("conditional-dependencies",
                "org.acme:ext-a::jar:0.1.0 org.acme:ext-b::jar:0.1.0");
    }

    @Test
    public void shouldContainsParentFirstArtifacts() throws IOException {
        String buildFileContent = TestUtils.DEFAULT_BUILD_GRADLE_CONTENT
                + QuarkusExtensionPlugin.EXTENSION_DESCRIPTOR_TASK_NAME + " { " +
                "parentFirstArtifacts = ['org.acme:ext-a:0.1.0', 'org.acme:ext-b:0.1.0']" +
                "}";
        TestUtils.writeFile(buildFile, buildFileContent);
        TestUtils.runExtensionDescriptorTask(testProjectDir);

        File extensionPropertiesFile = new File(testProjectDir, "build/resources/main/META-INF/quarkus-extension.properties");
        assertThat(extensionPropertiesFile).exists();

        Properties extensionProperty = TestUtils.readPropertyFile(extensionPropertiesFile.toPath());
        assertThat(extensionProperty).containsEntry("deployment-artifact", "org.acme:test-deployment:1.0.0");
        assertThat(extensionProperty).containsEntry("parent-first-artifacts", "org.acme:ext-a:0.1.0,org.acme:ext-b:0.1.0");
    }
}
