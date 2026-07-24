package io.quarkus.gradle.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BaseGradleTest {

    public static final String CONFIGURATION_CACHE = "--configuration-cache";
    public static final String NO_CONFIGURATION_CACHE = "--no-configuration-cache";
    public static final String ISOLATED_PROJECTS = "-Dorg.gradle.unsafe.isolated-projects=true";
    public static final String BUILD_CACHE = "--build-cache";
    public static final String STACKTRACE = "--stacktrace";

    @TempDir
    protected Path testProjectDir;

    protected BaseGradleTest() {
    }

    public static List<String> defaultGradleArguments(String... arguments) {
        return defaultGradleArguments(Arrays.asList(arguments));
    }

    public static List<String> defaultGradleArguments(List<String> arguments) {
        List<String> gradleArguments = new ArrayList<>(arguments);
        if (!gradleArguments.contains(CONFIGURATION_CACHE) && !gradleArguments.contains(NO_CONFIGURATION_CACHE)) {
            gradleArguments.add(CONFIGURATION_CACHE);
        }
        return gradleArguments;
    }

    public static List<String> isolatedProjectsGradleArguments(String... arguments) {
        return isolatedProjectsGradleArguments(Arrays.asList(arguments));
    }

    public static List<String> isolatedProjectsGradleArguments(List<String> arguments) {
        List<String> gradleArguments = defaultGradleArguments(arguments);
        if (!gradleArguments.contains(ISOLATED_PROJECTS)) {
            gradleArguments.add(ISOLATED_PROJECTS);
        }
        return gradleArguments;
    }

    public void writeFile(String fileName, String content) throws IOException {
        writeFile(testProjectDir.resolve(fileName), content);
    }

    public static void writeFile(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        try (var writer = Files.newBufferedWriter(file)) {
            writer.write(content);
        }
    }

    public static boolean containsFileNamed(Path root, String fileName) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.anyMatch(path -> path.getFileName().toString().equals(fileName));
        }
    }

    protected BuildResult buildResult(String task, String... extraArgs) {
        return buildResult(task, List.of(extraArgs));
    }

    protected BuildResult buildResult(String task, List<String> extraArgs) {
        return buildResult(task, extraArgs, Map.of());
    }

    protected BuildResult buildResult(String task, List<String> extraArgs, Map<String, String> env) {
        List<String> args = new ArrayList<>();
        args.add(task);
        args.add("--info");
        args.add(STACKTRACE);
        args.add(BUILD_CACHE);
        args.addAll(extraArgs);
        return buildResult(env, args);
    }

    protected BuildResult buildResultWithIsolatedProjects(String... args) {
        return prepareBuildWithIsolatedProjects(args).build();
    }

    protected BuildResult buildResult(Map<String, String> env, String... args) {
        return buildResult(env, List.of(args));
    }

    protected BuildResult buildResult(Map<String, String> env, List<String> args) {
        return prepareBuild(env, args).build();
    }

    protected BuildResult buildAndFailResult(String... args) {
        return buildAndFailResult(Map.of(), List.of(args));
    }

    protected BuildResult buildAndFailResult(Map<String, String> env, String... args) {
        return buildAndFailResult(env, List.of(args));
    }

    protected BuildResult buildAndFailResult(Map<String, String> env, List<String> args) {
        List<String> gradleArguments = new ArrayList<>();
        gradleArguments.add("--info");
        gradleArguments.add(STACKTRACE);
        gradleArguments.addAll(args);
        return prepareBuild(env, gradleArguments).buildAndFail();
    }

    protected GradleRunner prepareBuildWithIsolatedProjects(String... args) {
        List<String> gradleArguments = isolatedProjectsGradleArguments(args);
        if (!gradleArguments.contains(STACKTRACE)) {
            gradleArguments.add(STACKTRACE);
        }
        return prepareBuild(Map.of(), gradleArguments);
    }

    protected GradleRunner prepareBuild(Map<String, String> env, List<String> args) {
        GradleRunner gradleRunner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(defaultGradleArguments(args));
        if (!env.isEmpty()) {
            gradleRunner.withEnvironment(env);
        }
        return gradleRunner;
    }

    public static void assertTaskOutcomes(BuildResult result, TaskOutcome expectedOutcome, String... taskPaths) {
        Map<String, TaskOutcome> expectedOutcomes = new LinkedHashMap<>();
        for (String taskPath : taskPaths) {
            expectedOutcomes.put(taskPath, expectedOutcome);
        }
        assertTaskOutcomes(result, expectedOutcomes);
    }

    public static void assertTaskOutcomes(BuildResult result, Map<String, TaskOutcome> expectedOutcomes) {
        assertThat(taskOutcomes(result))
                .as("Gradle task outcomes")
                .containsAllEntriesOf(expectedOutcomes);
    }

    public static Map<String, TaskOutcome> taskOutcomes(BuildResult result) {
        return result.getTasks().stream()
                .collect(Collectors.toMap(BuildTask::getPath, BuildTask::getOutcome, (first, second) -> second,
                        LinkedHashMap::new));
    }
}
