package io.quarkus.extension.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class TestUtils {

    public static String getDefaultGradleBuildFileContent() throws IOException {
        return "plugins {\n" +
                "id 'java'\n" +
                "id 'io.quarkus.extension'\n" +
                "}\n" +
                "group 'org.acme'\n" +
                "version '1.0.0'\n" +
                "repositories { \n" +
                "mavenCentral()\n" +
                "mavenLocal()\n" +
                "}\n" +
                "dependencies { \n" +
                "implementation enforcedPlatform(\"io.quarkus:quarkus-bom:" + getCurrentQuarkusVersion() + "\")\n" +
                "implementation \"io.quarkus:quarkus-arc\" \n" +
                "}\n";
    }

    public static String getDefaultDeploymentBuildFileContent() throws IOException {
        return "plugins {\n" +
                "id 'java'\n" +
                "}\n" +
                "group 'org.acme'\n" +
                "version '1.0.0'\n" +
                "repositories { \n" +
                "mavenCentral()\n" +
                "mavenLocal()\n" +
                "}\n" +
                "dependencies {\n" +
                "implementation enforcedPlatform(\"io.quarkus:quarkus-bom:" + getCurrentQuarkusVersion() + "\")\n" +
                "implementation \"io.quarkus:quarkus-arc\" \n" +
                "implementation project(\":runtime\")" +
                "}\n";
    }

    public static BuildResult runExtensionDescriptorTask(File testProjectDir) {
        BuildResult extensionDescriptorResult = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir)
                .withArguments(QuarkusExtensionPlugin.EXTENSION_DESCRIPTOR_TASK_NAME, "-S")
                .build();

        assertThat(extensionDescriptorResult.task(":" + QuarkusExtensionPlugin.EXTENSION_DESCRIPTOR_TASK_NAME).getOutcome())
                .isEqualTo(TaskOutcome.SUCCESS);
        return extensionDescriptorResult;
    }

    public static void createExtensionProject(File testProjectDir) throws IOException {
        File runtimeModule = new File(testProjectDir, "runtime");
        runtimeModule.mkdir();
        writeFile(new File(runtimeModule, "build.gradle"), getDefaultGradleBuildFileContent());
        File runtimeTestFile = new File(runtimeModule, "src/main/java/runtime/Test.java");
        runtimeTestFile.getParentFile().mkdirs();
        writeFile(runtimeTestFile, "package runtime; public class Test {}");

        File deploymentModule = new File(testProjectDir, "deployment");
        deploymentModule.mkdir();
        writeFile(new File(deploymentModule, "build.gradle"), getDefaultDeploymentBuildFileContent());
        File deploymentTestFile = new File(deploymentModule, "src/main/java/deployment/Test.java");
        deploymentTestFile.getParentFile().mkdirs();
        writeFile(deploymentTestFile, "package deployment; public class Test {}");

        writeFile(new File(testProjectDir, "settings.gradle"), "include 'runtime', 'deployment'");

    }

    public static void writeFile(File destination, String content) throws IOException {
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(destination));
            output.write(content);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    public static Properties readPropertyFile(Path propertyFile) throws IOException {
        final Properties extensionProperties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(propertyFile)) {
            extensionProperties.load(reader);
        }
        return extensionProperties;
    }

    public static ObjectNode readExtensionFile(Path extensionFile) throws IOException {
        YAMLFactory yf = new YAMLFactory();
        ObjectMapper mapper = new ObjectMapper(yf)
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
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
