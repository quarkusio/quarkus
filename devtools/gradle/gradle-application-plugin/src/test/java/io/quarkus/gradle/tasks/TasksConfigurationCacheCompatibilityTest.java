package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.QuarkusPlugin.DEPLOY_TASK_NAME;
import static io.quarkus.gradle.QuarkusPlugin.QUARKUS_BUILD_APP_PARTS_TASK_NAME;
import static io.quarkus.gradle.QuarkusPlugin.QUARKUS_BUILD_DEP_TASK_NAME;
import static io.quarkus.gradle.QuarkusPlugin.QUARKUS_BUILD_TASK_NAME;
import static io.quarkus.gradle.QuarkusPlugin.QUARKUS_GENERATE_CODE_DEV_TASK_NAME;
import static io.quarkus.gradle.QuarkusPlugin.QUARKUS_GENERATE_CODE_TASK_NAME;
import static io.quarkus.gradle.QuarkusPlugin.QUARKUS_GENERATE_CODE_TESTS_TASK_NAME;
import static io.quarkus.gradle.QuarkusPlugin.QUARKUS_SHOW_EFFECTIVE_CONFIG_TASK_NAME;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TasksConfigurationCacheCompatibilityTest {

    @TempDir
    Path testProjectDir;

    private static Stream<String> compatibleTasks() {
        return Stream.of(
                QUARKUS_GENERATE_CODE_TASK_NAME,
                QUARKUS_GENERATE_CODE_TESTS_TASK_NAME,
                QUARKUS_GENERATE_CODE_DEV_TASK_NAME,
                QUARKUS_BUILD_DEP_TASK_NAME,
                QUARKUS_BUILD_APP_PARTS_TASK_NAME,
                QUARKUS_SHOW_EFFECTIVE_CONFIG_TASK_NAME,
                QUARKUS_BUILD_TASK_NAME,
                "build");
    }

    private static Stream<String> nonCompatibleQuarkusBuildTasks() {
        return Stream.of(DEPLOY_TASK_NAME);
    }

    @Test
    @Order(1)
    public void quarkusBuildFooTest() throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/configurationcache/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(QUARKUS_GENERATE_CODE_TASK_NAME, "--info", "--stacktrace", "--build-cache",
                        "--configuration-cache")
                .build();
        assertTrue(true);
    }

    @ParameterizedTest
    @Order(2)
    @MethodSource("compatibleTasks")
    public void configurationCacheIsReusedTest(String taskName) throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/configurationcache/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        BuildResult firstBuild = buildResult(taskName, "--configuration-cache");
        assertTrue(firstBuild.getOutput().contains("Configuration cache entry stored"));

        BuildResult secondBuild = buildResult(taskName, "--configuration-cache");
        assertTrue(secondBuild.getOutput().contains("Reusing configuration cache."));
    }

    @ParameterizedTest
    @Order(3)
    @MethodSource("compatibleTasks")
    public void configurationCacheIsReusedWhenProjectIsolationIsUsedTest(String taskName)
            throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/configurationcache/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        BuildResult firstBuild = buildResult(taskName, "-Dorg.gradle.unsafe.isolated-projects=true");
        assertTrue(firstBuild.getOutput().contains("Configuration cache entry stored"));

        BuildResult secondBuild = buildResult(taskName, "-Dorg.gradle.unsafe.isolated-projects=true");
        assertTrue(secondBuild.getOutput().contains("Reusing configuration cache."));
    }

    @ParameterizedTest
    @Order(4)
    @MethodSource("nonCompatibleQuarkusBuildTasks")
    public void quarkusBuildTasksNonCompatibleWithConfigurationCacheNotFail(String taskName)
            throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/configurationcache/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        BuildResult build = buildResult(taskName);
        assertTrue(build.getOutput().contains("BUILD SUCCESSFUL"));

    }

    @ParameterizedTest
    @MethodSource("nonCompatibleQuarkusBuildTasks")
    @Order(5)
    public void quarkusBuildTasksNonCompatibleWithConfigurationCacheNotFailWhenUsingConfigurationCache(String taskName)
            throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/configurationcache/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        BuildResult build = buildResult(taskName, "--no-configuration-cache");
        assertTrue(build.getOutput().contains("BUILD SUCCESSFUL"));

    }

    private BuildResult buildResult(String task, String configurationCacheCommand) {
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(task, "--info", "--stacktrace", "--build-cache", configurationCacheCommand)
                .build();
    }

    private BuildResult buildResult(String task) {
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(task, "--info", "--stacktrace", "--build-cache")
                .build();
    }
}
