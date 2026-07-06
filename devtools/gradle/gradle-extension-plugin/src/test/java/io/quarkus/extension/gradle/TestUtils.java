package io.quarkus.extension.gradle;

import static io.quarkus.gradle.testing.BaseGradleTest.defaultGradleArguments;
import static io.quarkus.gradle.testing.BaseGradleTest.writeFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

public class TestUtils {

    private static final String GENERATED_EXTENSION_RESOURCES = "build/generated/resources/quarkus-extension/main";

    public static String getDefaultGradleBuildFileContent(boolean disableValidation, List<String> implementationDependencies,
            String customPluginConfiguration)
            throws IOException {
        StringBuilder implementationBuilder = new StringBuilder();
        for (String implementationDependency : implementationDependencies) {
            implementationBuilder.append("implementation(\"").append(implementationDependency).append("\")\n");
        }
        return "plugins {\n" +
                "id 'java'\n" +
                "id 'io.quarkus.extension'\n" +
                "}\n" +
                "group = 'org.acme'\n" +
                "version = '1.0.0'\n" +
                "repositories { \n" +
                "mavenCentral()\n" +
                "mavenLocal()\n" +
                "}\n" +
                QuarkusExtensionPlugin.EXTENSION_CONFIGURATION_NAME + " { \n" +
                "disableValidation = " + disableValidation + "\n" +
                customPluginConfiguration +
                "}\n" +
                "dependencies { \n" +
                "implementation enforcedPlatform(\"io.quarkus:quarkus-bom:" + getCurrentQuarkusVersion() + "\")\n" +
                "implementation \"io.quarkus:quarkus-arc\" \n" +
                implementationBuilder +
                "}\n";
    }

    public static String getDefaultDeploymentBuildFileContent(List<String> implementationDependencies) throws IOException {

        StringBuilder implementationBuilder = new StringBuilder();
        for (String implementationDependency : implementationDependencies) {
            implementationBuilder.append("implementation(\"").append(implementationDependency).append("\")\n");
        }

        return "plugins {\n" +
                "id 'io.quarkus.extension.deployment'\n" +
                "}\n" +
                "group = 'org.acme'\n" +
                "version = '1.0.0'\n" +
                "repositories { \n" +
                "mavenCentral()\n" +
                "mavenLocal()\n" +
                "}\n" +
                "dependencies {\n" +
                "implementation enforcedPlatform(\"io.quarkus:quarkus-bom:" + getCurrentQuarkusVersion() + "\")\n" +
                "implementation \"io.quarkus:quarkus-arc-deployment\" \n" +
                "implementation project(\":runtime\") \n" +
                implementationBuilder +
                "}\n";
    }

    public static BuildResult runExtensionDescriptorTask(Path testProjectDir) {
        BuildResult extensionDescriptorResult = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(defaultGradleArguments(QuarkusExtensionPlugin.EXTENSION_DESCRIPTOR_TASK_NAME, "-S"))
                .build();

        assertThat(extensionDescriptorResult.task(":" + QuarkusExtensionPlugin.EXTENSION_DESCRIPTOR_TASK_NAME).getOutcome())
                .isEqualTo(TaskOutcome.SUCCESS);
        return extensionDescriptorResult;
    }

    public static Path generatedExtensionResources(Path testProjectDir) {
        return testProjectDir.resolve(GENERATED_EXTENSION_RESOURCES);
    }

    public static void createExtensionProject(Path testProjectDir, boolean disableValidation, List<String> runtimeDependencies,
            List<String> deploymentDependencies) throws IOException {
        createExtensionProject(testProjectDir, disableValidation, runtimeDependencies, deploymentDependencies,
                "deploymentArtifact = 'org.acme:test-deployment:1.0.0'\n");
    }

    public static void createExtensionProjectWithLocalDeployment(Path testProjectDir, boolean disableValidation,
            List<String> runtimeDependencies, List<String> deploymentDependencies) throws IOException {
        createExtensionProject(testProjectDir, disableValidation, runtimeDependencies, deploymentDependencies, "");
    }

    private static void createExtensionProject(Path testProjectDir, boolean disableValidation,
            List<String> runtimeDependencies, List<String> deploymentDependencies, String customPluginConfiguration)
            throws IOException {
        var runtimeModule = testProjectDir.resolve("runtime");
        var runtimeTestFile = runtimeModule.resolve("src/main/java/runtime/Test.java");
        var deploymentModule = testProjectDir.resolve("deployment");
        var deploymentTestFile = deploymentModule.resolve("src/main/java/deployment/Test.java");
        Files.createDirectories(runtimeTestFile.getParent());
        Files.createDirectories(deploymentTestFile.getParent());

        writeFile(runtimeModule.resolve("build.gradle"),
                getDefaultGradleBuildFileContent(disableValidation, runtimeDependencies, customPluginConfiguration));
        writeFile(runtimeTestFile, "package runtime; public class Test {}");

        writeFile(deploymentModule.resolve("build.gradle"), getDefaultDeploymentBuildFileContent(deploymentDependencies));
        writeFile(deploymentTestFile, "package deployment; public class Test {}");

        writeFile(testProjectDir.resolve("settings.gradle"), "include 'runtime', 'deployment'");
    }

    public static Properties readPropertyFile(Path propertyFile) throws IOException {
        final Properties extensionProperties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(propertyFile)) {
            extensionProperties.load(reader);
        }
        return extensionProperties;
    }

    public static ObjectNode readExtensionFile(Path extensionFile) throws IOException {
        ObjectMapper mapper = YAMLMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .build();
        try (InputStream is = Files.newInputStream(extensionFile)) {
            return mapper.readValue(is, ObjectNode.class);
        } catch (IOException io) {
            throw new IOException("Failed to parse " + extensionFile, io);
        }
    }

    public static String getCurrentQuarkusVersion() throws IOException {
        final Path curDir = Paths.get("").toAbsolutePath().normalize();
        final Path gradlePropsFile = curDir.getParent().resolve("gradle.properties");
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(gradlePropsFile)) {
            props.load(is);
        }
        final String quarkusVersion = props.getProperty("version");
        if (quarkusVersion == null) {
            throw new IllegalStateException("Failed to locate Quarkus version in " + gradlePropsFile);
        }
        return quarkusVersion;
    }
}
