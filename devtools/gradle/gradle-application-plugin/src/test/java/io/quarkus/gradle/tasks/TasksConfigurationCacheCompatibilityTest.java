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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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

    @ParameterizedTest
    @MethodSource("compatibleTasks")
    public void configurationCacheIsReusedTest(String taskName) throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/configurationcache/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        buildResult(":help", "--configuration-cache");

        BuildResult firstBuild = buildResult(taskName, "--configuration-cache");
        assertTrue(firstBuild.getOutput().contains("Configuration cache entry stored"));

        BuildResult secondBuild = buildResult(taskName, "--configuration-cache");
        assertTrue(secondBuild.getOutput().contains("Reusing configuration cache."));
    }

    @ParameterizedTest
    @MethodSource("compatibleTasks")
    public void configurationCacheIsReusedWhenProjectIsolationIsUsedTest(String taskName)
            throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/configurationcache/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        buildResult(":help", "--configuration-cache");

        BuildResult firstBuild = buildResult(taskName, "-Dorg.gradle.unsafe.isolated-projects=true");
        assertTrue(firstBuild.getOutput().contains("Configuration cache entry stored"));

        BuildResult secondBuild = buildResult(taskName, "-Dorg.gradle.unsafe.isolated-projects=true");
        assertTrue(secondBuild.getOutput().contains("Reusing configuration cache."));
    }

    @ParameterizedTest
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
    public void quarkusBuildTasksNonCompatibleWithConfigurationCacheNotFailWhenUsingConfigurationCache(String taskName)
            throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/configurationcache/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        BuildResult build = buildResult(taskName, "--no-configuration-cache");
        assertTrue(build.getOutput().contains("BUILD SUCCESSFUL"));

    }

    @Test
    public void configurationCacheIsReusedWhenUnrelatedSystemPropertyChanges() throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/configurationcache/main");
        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());
        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        buildResult(":help", "--configuration-cache");

        // First build: store configuration cache entry with -Da=1
        BuildResult firstBuild = buildResult(QUARKUS_BUILD_TASK_NAME,
                Arrays.asList("--configuration-cache", "-Da=1"));
        assertTrue(firstBuild.getOutput().contains("Configuration cache entry stored"),
                "First build should store configuration cache entry");

        // Second build: change unrelated system property to -Da=2
        // The configuration cache should still be reused because the ValueSource filters
        // out non-quarkus system properties from its result.
        BuildResult secondBuild = buildResult(QUARKUS_BUILD_TASK_NAME,
                Arrays.asList("--configuration-cache", "-Da=2"));
        assertTrue(secondBuild.getOutput().contains("Reusing configuration cache."),
                "Configuration cache should be reused when only unrelated system properties change");
    }

    private BuildResult buildResult(String task, String configurationCacheCommand) {
        return buildResult(task, List.of(configurationCacheCommand));
    }

    private BuildResult buildResult(String task, List<String> extraArgs) {
        List<String> args = new java.util.ArrayList<>();
        args.add(task);
        args.add("--info");
        args.add("--stacktrace");
        args.add("--build-cache");
        args.addAll(extraArgs);
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(args)
                .build();
    }

    private BuildResult buildResult(String task) {
        return buildResult(task, List.of());
    }
}
