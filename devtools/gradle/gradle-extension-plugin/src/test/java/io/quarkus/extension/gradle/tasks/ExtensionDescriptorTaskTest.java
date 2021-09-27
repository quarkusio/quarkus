package io.quarkus.extension.gradle.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.extension.gradle.QuarkusExtensionPlugin;
import io.quarkus.extension.gradle.TestUtils;

public class ExtensionDescriptorTaskTest {

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
    public void shouldCreateFilesWithDefaultValues() throws IOException {
        TestUtils.writeFile(buildFile, TestUtils.getDefaultGradleBuildFileContent());
        TestUtils.runExtensionDescriptorTask(testProjectDir);

        File extensionPropertiesFile = new File(testProjectDir, "build/resources/main/META-INF/quarkus-extension.properties");
        assertThat(extensionPropertiesFile).exists();

        Properties extensionProperty = TestUtils.readPropertyFile(extensionPropertiesFile.toPath());
        assertThat(extensionProperty).containsEntry("deployment-artifact", "org.acme:test-deployment:1.0.0");

        File extensionDescriptorFile = new File(testProjectDir, "build/resources/main/META-INF/quarkus-extension.yaml");
        assertThat(extensionDescriptorFile).exists();

        ObjectNode extensionDescriptor = TestUtils.readExtensionFile(extensionDescriptorFile.toPath());
        assertThat(extensionDescriptor.has("name")).isTrue();
        assertThat(extensionDescriptor.has("artifact")).isTrue();
        assertThat(extensionDescriptor.get("name").asText()).isEqualTo("test");
        assertThat(extensionDescriptor.get("artifact").asText()).isEqualTo("org.acme:test::jar:1.0.0");
        assertThat(extensionDescriptor.has("description")).isFalse();

        // Assert metadata node
        assertThat(extensionDescriptor.has("metadata")).isTrue();
        JsonNode metadata = extensionDescriptor.get("metadata");
        assertThat(metadata.has("built-with-quarkus-core")).isTrue();
        assertThat(metadata.get("built-with-quarkus-core").asText()).isEqualTo(TestUtils.getCurrentQuarkusVersion());
        assertThat(metadata.has("extension-dependencies")).isTrue();
        assertThat(metadata.get("extension-dependencies").isArray()).isTrue();

        ArrayNode extensionNodes = (ArrayNode) metadata.get("extension-dependencies");
        List<String> extensions = new ArrayList<>();
        for (JsonNode extension : extensionNodes) {
            extensions.add(extension.asText());
        }
        assertThat(extensions).hasSize(2);
        assertThat(extensions).contains("io.quarkus:quarkus-core", "io.quarkus:quarkus-arc");
    }

    @Test
    public void shouldUseCustomDeploymentArtifactName() throws IOException {
        String buildFileContent = TestUtils.getDefaultGradleBuildFileContent()
                + QuarkusExtensionPlugin.EXTENSION_CONFIGURATION_NAME + " { " +
                "deploymentArtifact = 'custom.group:custom-deployment-artifact:0.1.0'" +
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
        String buildFileContent = TestUtils.getDefaultGradleBuildFileContent()
                + QuarkusExtensionPlugin.EXTENSION_CONFIGURATION_NAME + " { " +
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
        String buildFileContent = TestUtils.getDefaultGradleBuildFileContent()
                + QuarkusExtensionPlugin.EXTENSION_CONFIGURATION_NAME + " { " +
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

    @Test
    public void shouldGenerateDescriptorBasedOnExistingFile() throws IOException {
        TestUtils.writeFile(buildFile, TestUtils.getDefaultGradleBuildFileContent());
        File metaInfDir = new File(testProjectDir, "src/main/resources/META-INF");
        metaInfDir.mkdirs();
        String description = "name: extension-name\n" +
                "description: this is a sample extension\n";
        TestUtils.writeFile(new File(metaInfDir, "quarkus-extension.yaml"), description);

        TestUtils.runExtensionDescriptorTask(testProjectDir);

        File extensionDescriptorFile = new File(testProjectDir, "build/resources/main/META-INF/quarkus-extension.yaml");
        assertThat(extensionDescriptorFile).exists();
        ObjectNode extensionDescriptor = TestUtils.readExtensionFile(extensionDescriptorFile.toPath());
        assertThat(extensionDescriptor.has("name")).isTrue();
        assertThat(extensionDescriptor.get("name").asText()).isEqualTo("extension-name");
        assertThat(extensionDescriptor.has("description")).isTrue();
        assertThat(extensionDescriptor.get("description").asText()).isEqualTo("this is a sample extension");
    }
}
