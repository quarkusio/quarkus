package io.quarkus.extension.gradle.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.FAILED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.extension.gradle.QuarkusExtensionPlugin;
import io.quarkus.extension.gradle.TestUtils;
import io.quarkus.gradle.testing.BaseGradleTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public class ExtensionDescriptorTaskTest extends BaseGradleTest {

    @BeforeEach
    public void setupProject() throws IOException {
        writeFile("settings.gradle", "rootProject.name = 'test'");
    }

    @Test
    public void shouldBeUpToDateWhenInputsAndOutputsAreUnchanged() throws IOException {
        writeFile("build.gradle", TestUtils.getDefaultGradleBuildFileContent(true, Collections.emptyList(), ""));

        BuildResult firstRun = runExtensionDescriptorTask();
        assertTaskOutcomes(firstRun, SUCCESS, ":" + QuarkusExtensionPlugin.EXTENSION_DESCRIPTOR_TASK_NAME);

        BuildResult secondRun = runExtensionDescriptorTask();
        assertTaskOutcomes(secondRun, UP_TO_DATE, ":" + QuarkusExtensionPlugin.EXTENSION_DESCRIPTOR_TASK_NAME);
    }

    @Test
    public void descriptorInputsShouldInvalidateGeneration() throws IOException {
        writeFile("build.gradle", TestUtils.getDefaultGradleBuildFileContent(true, Collections.emptyList(), ""));
        writeFile("src/main/resources/META-INF/quarkus-extension.yaml", "name: first name\n");
        assertTaskOutcomes(runExtensionDescriptorTask(), SUCCESS, ":extensionDescriptor");

        writeFile("src/main/resources/META-INF/quarkus-extension.yaml", "name: second name\n");
        assertTaskOutcomes(runExtensionDescriptorTask(), SUCCESS, ":extensionDescriptor");
        var generatedResources = TestUtils.generatedExtensionResources(testProjectDir);
        assertThat(TestUtils.readExtensionFile(generatedResources.resolve("META-INF/quarkus-extension.yaml"))
                .get("name").asText()).isEqualTo("second name");

        writeFile("build.gradle", TestUtils.getDefaultGradleBuildFileContent(true, Collections.emptyList(),
                "deploymentArtifact = 'org.acme:custom-deployment:1.0.0'"));
        assertTaskOutcomes(runExtensionDescriptorTask(), SUCCESS, ":extensionDescriptor");
        assertThat(TestUtils.readPropertyFile(generatedResources.resolve("META-INF/quarkus-extension.properties")))
                .containsEntry("deployment-artifact", "org.acme:custom-deployment:1.0.0");

        writeFile("build.gradle", TestUtils.getDefaultGradleBuildFileContent(true,
                List.of("io.quarkus:quarkus-jdbc-h2"),
                "deploymentArtifact = 'org.acme:custom-deployment:1.0.0'"));
        assertTaskOutcomes(runExtensionDescriptorTask(), SUCCESS, ":extensionDescriptor");
        var dependencies = TestUtils.readExtensionFile(generatedResources.resolve("META-INF/quarkus-extension.yaml"))
                .get("metadata").get("extension-dependencies");
        assertThat(dependencies).anyMatch(dependency -> dependency.asText().equals("io.quarkus:quarkus-jdbc-h2"));
    }

    @Test
    public void processResourcesShouldRemainUpToDateAfterDescriptorGeneration() throws IOException {
        writeFile("build.gradle", TestUtils.getDefaultGradleBuildFileContent(true, Collections.emptyList(), ""));
        writeFile("src/main/resources/META-INF/quarkus-extension.yaml", "name: test extension\n");
        var staleGeneratedResource = TestUtils.generatedExtensionResources(testProjectDir).resolve("stale.txt");
        writeFile(staleGeneratedResource, "stale\n");

        BuildResult firstRun = buildResult("processResources", CONFIGURATION_CACHE, ISOLATED_PROJECTS);
        BuildResult secondRun = buildResult("processResources", CONFIGURATION_CACHE, ISOLATED_PROJECTS);

        assertTaskOutcomes(firstRun, SUCCESS,
                ":" + QuarkusExtensionPlugin.EXTENSION_DESCRIPTOR_TASK_NAME,
                ":processResources");
        assertTaskOutcomes(secondRun, UP_TO_DATE,
                ":" + QuarkusExtensionPlugin.EXTENSION_DESCRIPTOR_TASK_NAME,
                ":processResources");
        assertThat(secondRun.getOutput()).contains("Reusing configuration cache.");
        assertThat(staleGeneratedResource).doesNotExist();
        assertThat(testProjectDir.resolve("build/resources/main/META-INF/quarkus-extension.properties")).isRegularFile();
        var processedDescriptor = testProjectDir.resolve("build/resources/main/META-INF/quarkus-extension.yaml");
        assertThat(processedDescriptor).isRegularFile();
        assertThat(TestUtils.readExtensionFile(processedDescriptor).get("artifact").asText())
                .isEqualTo("org.acme:test::jar:1.0.0");
    }

    @Test
    public void shouldCreateFilesWithDefaultValues() throws IOException {
        writeFile("build.gradle", TestUtils.getDefaultGradleBuildFileContent(true, Collections.emptyList(), ""));
        TestUtils.runExtensionDescriptorTask(testProjectDir);

        var extensionPropertiesFile = TestUtils.generatedExtensionResources(testProjectDir)
                .resolve("META-INF/quarkus-extension.properties");
        assertThat(extensionPropertiesFile).exists();

        Properties extensionProperty = TestUtils.readPropertyFile(extensionPropertiesFile);
        assertThat(extensionProperty).containsEntry("deployment-artifact", "org.acme:test-deployment:1.0.0");

        var extensionDescriptorFile = TestUtils.generatedExtensionResources(testProjectDir)
                .resolve("META-INF/quarkus-extension.yaml");
        assertThat(extensionDescriptorFile).exists();

        ObjectNode extensionDescriptor = TestUtils.readExtensionFile(extensionDescriptorFile);
        assertThat(extensionDescriptor.has("name")).isTrue();
        assertThat(extensionDescriptor.has("artifact")).isTrue();
        assertThat(extensionDescriptor.get("name").asText()).isEqualTo("test");
        assertThat(extensionDescriptor.get("artifact").asText()).isEqualTo("org.acme:test::jar:1.0.0");
        assertThat(extensionDescriptor.has("description")).isFalse();

        // Assert JSON file is also generated
        var extensionJsonFile = TestUtils.generatedExtensionResources(testProjectDir)
                .resolve("META-INF/quarkus-extension.json");
        assertThat(extensionJsonFile).exists();

        JsonMapper jsonMapper = JsonMapper.builder().build();
        ObjectNode jsonDescriptor = jsonMapper.readValue(extensionJsonFile.toFile(), ObjectNode.class);
        assertThat(jsonDescriptor.get("name").asText()).isEqualTo("test");
        assertThat(jsonDescriptor.get("artifact").asText()).isEqualTo("org.acme:test::jar:1.0.0");

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
        assertThat(extensions).containsExactly("io.quarkus:quarkus-arc", "io.quarkus:quarkus-core");
        assertThat(testProjectDir.resolve("build/resources/main/META-INF/quarkus-extension.properties")).doesNotExist();
    }

    @Test
    public void shouldUseCustomDeploymentArtifactName() throws IOException {
        String buildFileContent = TestUtils.getDefaultGradleBuildFileContent(true, Collections.emptyList(),
                "deploymentArtifact = 'custom.group:custom-deployment-artifact:0.1.0'");
        writeFile("build.gradle", buildFileContent);
        TestUtils.runExtensionDescriptorTask(testProjectDir);

        var extensionPropertiesFile = TestUtils.generatedExtensionResources(testProjectDir)
                .resolve("META-INF/quarkus-extension.properties");
        assertThat(extensionPropertiesFile).isRegularFile();

        Properties extensionProperty = TestUtils.readPropertyFile(extensionPropertiesFile);
        assertThat(extensionProperty).containsEntry("deployment-artifact", "custom.group:custom-deployment-artifact:0.1.0");
    }

    @Test
    public void shouldContainsConditionalDependencies() throws IOException {
        String buildFileContent = TestUtils.getDefaultGradleBuildFileContent(true, Collections.emptyList(),
                "conditionalDependencies= ['org.acme:ext-a:0.1.0', 'org.acme:ext-b:0.1.0']");
        writeFile("build.gradle", buildFileContent);
        TestUtils.runExtensionDescriptorTask(testProjectDir);

        var extensionPropertiesFile = TestUtils.generatedExtensionResources(testProjectDir)
                .resolve("META-INF/quarkus-extension.properties");
        assertThat(extensionPropertiesFile).exists();

        Properties extensionProperty = TestUtils.readPropertyFile(extensionPropertiesFile);
        assertThat(extensionProperty).containsEntry("deployment-artifact", "org.acme:test-deployment:1.0.0");
        assertThat(extensionProperty).containsEntry("conditional-dependencies",
                "org.acme:ext-a::jar:0.1.0 org.acme:ext-b::jar:0.1.0");
    }

    @Test
    public void shouldContainsParentFirstArtifacts() throws IOException {
        String buildFileContent = TestUtils.getDefaultGradleBuildFileContent(true, Collections.emptyList(),
                "parentFirstArtifacts = ['org.acme:ext-a:0.1.0', 'org.acme:ext-b:0.1.0']");

        writeFile("build.gradle", buildFileContent);
        TestUtils.runExtensionDescriptorTask(testProjectDir);

        var extensionPropertiesFile = TestUtils.generatedExtensionResources(testProjectDir)
                .resolve("META-INF/quarkus-extension.properties");
        assertThat(extensionPropertiesFile).exists();

        Properties extensionProperty = TestUtils.readPropertyFile(extensionPropertiesFile);
        assertThat(extensionProperty).containsEntry("deployment-artifact", "org.acme:test-deployment:1.0.0");
        assertThat(extensionProperty).containsEntry("parent-first-artifacts", "org.acme:ext-a:0.1.0,org.acme:ext-b:0.1.0");
    }

    @Test
    public void shouldContainsRemoveResources() throws IOException {
        String buildFileContent = TestUtils.getDefaultGradleBuildFileContent(true, Collections.emptyList(),
                """
                        removedResources {\s
                        artifact('org.acme:acme-resources').resource('META-INF/a')\s
                        artifact('org.acme:acme-resources-two').resource('META-INF/b').resource('META-INF/c')\s
                        }
                        """);

        writeFile("build.gradle", buildFileContent);
        TestUtils.runExtensionDescriptorTask(testProjectDir);

        var extensionPropertiesFile = TestUtils.generatedExtensionResources(testProjectDir)
                .resolve("META-INF/quarkus-extension.properties");
        assertThat(extensionPropertiesFile).exists();

        Properties extensionProperty = TestUtils.readPropertyFile(extensionPropertiesFile);
        assertThat(extensionProperty).containsEntry("deployment-artifact", "org.acme:test-deployment:1.0.0");
        assertThat(extensionProperty).containsEntry("removed-resources.org.acme:acme-resources::jar", "META-INF/a");
        assertThat(extensionProperty).containsEntry("removed-resources.org.acme:acme-resources-two::jar",
                "META-INF/b,META-INF/c");
    }

    @Test
    public void shouldGenerateDescriptorBasedOnExistingFile() throws IOException {
        writeFile("build.gradle", TestUtils.getDefaultGradleBuildFileContent(true, Collections.emptyList(), ""));
        String description = """
                name: extension-name
                description: this is a sample extension
                """;
        writeFile("src/main/resources/META-INF/quarkus-extension.yaml", description);

        TestUtils.runExtensionDescriptorTask(testProjectDir);

        var extensionDescriptorFile = TestUtils.generatedExtensionResources(testProjectDir)
                .resolve("META-INF/quarkus-extension.yaml");
        assertThat(extensionDescriptorFile).exists();
        ObjectNode extensionDescriptor = TestUtils.readExtensionFile(extensionDescriptorFile);
        assertThat(extensionDescriptor.has("name")).isTrue();
        assertThat(extensionDescriptor.get("name").asText()).isEqualTo("extension-name");
        assertThat(extensionDescriptor.has("description")).isTrue();
        assertThat(extensionDescriptor.get("description").asText()).isEqualTo("this is a sample extension");
    }

    @Test
    public void shouldFailOnInvalidStatusArray() throws IOException {
        writeFile("build.gradle", TestUtils.getDefaultGradleBuildFileContent(true, Collections.emptyList(), ""));
        String invalid = """
                name: extension-name
                metadata:
                  status:
                  - stable
                  - deprecated
                """;
        writeFile("src/main/resources/META-INF/quarkus-extension.yaml", invalid);

        BuildResult result = buildAndFailResult(QuarkusExtensionPlugin.EXTENSION_DESCRIPTOR_TASK_NAME);

        assertTaskOutcomes(result, FAILED, ":extensionDescriptor");
        assertThat(result.getOutput()).contains("Invalid quarkus-extension.yaml metadata");
        assertThat(result.getOutput()).contains("status");
    }

    @Test
    public void shouldGenerateDescriptorWithCapabilities() throws IOException {
        String buildFileContent = TestUtils.getDefaultGradleBuildFileContent(true, Collections.emptyList(),
                """
                        capabilities {\s
                           provides 'org.acme:ext-a:0.1.0'\s
                           provides 'org.acme:ext-b:0.1.0' onlyIf(['org.acme:ext-b:0.1.0']) onlyIfNot(['org.acme:ext-c:0.1.0'])\s
                           requires 'sunshine' onlyIf(['org.acme:ext-b:0.1.0'])\s
                        }
                        """);

        writeFile("build.gradle", buildFileContent);
        TestUtils.runExtensionDescriptorTask(testProjectDir);

        var extensionPropertiesFile = TestUtils.generatedExtensionResources(testProjectDir)
                .resolve("META-INF/quarkus-extension.properties");
        assertThat(extensionPropertiesFile).exists();

        Properties extensionProperty = TestUtils.readPropertyFile(extensionPropertiesFile);
        assertThat(extensionProperty).containsEntry("provides-capabilities",
                "org.acme:ext-a:0.1.0,org.acme:ext-b:0.1.0?org.acme:ext-b:0.1.0?!org.acme:ext-c:0.1.0");
        assertThat(extensionProperty).containsEntry("requires-capabilities",
                "sunshine?org.acme:ext-b:0.1.0");
    }

    /*
     * This test will fail if run in an IDE without extra config - it needs an environment variable, and
     * that is increasingly hard to do on Java 17+; see https://github.com/junit-pioneer/junit-pioneer/issues/509
     */
    @Test
    public void shouldGenerateScmInformation() throws IOException {
        writeFile("build.gradle", TestUtils.getDefaultGradleBuildFileContent(true, Collections.emptyList(), ""));
        String description = """
                name: extension-name
                description: this is a sample extension
                """;
        writeFile("src/main/resources/META-INF/quarkus-extension.yaml", description);

        TestUtils.runExtensionDescriptorTask(testProjectDir);

        var extensionDescriptorFile = TestUtils.generatedExtensionResources(testProjectDir)
                .resolve("META-INF/quarkus-extension.yaml");
        assertThat(extensionDescriptorFile).exists();
        ObjectNode extensionDescriptor = TestUtils.readExtensionFile(extensionDescriptorFile);
        assertThat(extensionDescriptor.get("metadata").get("scm-url")).isNotNull();
        assertThat(extensionDescriptor.get("metadata").get("scm-url").asText())
                .as("Check source location %s", extensionDescriptor.get("scm-url"))
                .isEqualTo("https://github.com/some/repo");
    }

    private BuildResult runExtensionDescriptorTask(String... arguments) {
        List<String> gradleArguments = new ArrayList<>();
        Collections.addAll(gradleArguments, arguments);
        gradleArguments.add(CONFIGURATION_CACHE);
        gradleArguments.add(ISOLATED_PROJECTS);
        gradleArguments.add(QuarkusExtensionPlugin.EXTENSION_DESCRIPTOR_TASK_NAME);
        return buildResult(Map.of(), gradleArguments);
    }

}
