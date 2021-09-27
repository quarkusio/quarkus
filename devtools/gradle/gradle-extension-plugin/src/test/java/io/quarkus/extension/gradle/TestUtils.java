package io.quarkus.extension.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;

public class TestUtils {

    public static final String DEFAULT_BUILD_GRADLE_CONTENT = "plugins {\n" +
            "id 'java'\n" +
            "id 'io.quarkus.extension'\n" +
            "}\n" +
            "group 'org.acme'\n" +
            "version '1.0.0'\n";

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
        writeFile(new File(runtimeModule, "build.gradle"), DEFAULT_BUILD_GRADLE_CONTENT);

        File deploymentModule = new File(testProjectDir, "deployment");
        deploymentModule.mkdir();
        writeFile(new File(deploymentModule, "build.gradle"), "plugins { id 'java'}");

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
}
