package io.quarkus.gradle.tasks;

import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.gradle.testing.BaseGradleTest;

@ExtendWith(SoftAssertionsExtension.class)
public class CachingTest extends BaseGradleTest {
    private static final Map<String, TaskOutcome> ALL_SUCCESS = Map.of(
            ":quarkusGenerateCode", TaskOutcome.SUCCESS,
            ":quarkusGenerateCodeTests", TaskOutcome.SUCCESS,
            ":quarkusAppPartsBuild", TaskOutcome.SUCCESS,
            ":quarkusDependenciesBuild", TaskOutcome.SUCCESS,
            ":quarkusBuild", TaskOutcome.SUCCESS,
            ":build", TaskOutcome.SUCCESS);
    private static final Map<String, TaskOutcome> ALL_UP_TO_DATE = Map.of(
            ":quarkusGenerateCode", TaskOutcome.UP_TO_DATE,
            // intentionally omit ":quarkusGenerateCodeDev", it can be UP_TO_DATE or SUCCESS
            ":quarkusGenerateCodeTests", TaskOutcome.UP_TO_DATE,
            ":quarkusAppPartsBuild", TaskOutcome.UP_TO_DATE,
            ":quarkusDependenciesBuild", TaskOutcome.UP_TO_DATE,
            ":quarkusBuild", TaskOutcome.UP_TO_DATE,
            ":build", TaskOutcome.UP_TO_DATE);
    public static final Map<String, TaskOutcome> FROM_CACHE = Map.of(
            ":quarkusGenerateCode", TaskOutcome.FROM_CACHE,
            ":quarkusGenerateCodeTests", TaskOutcome.FROM_CACHE,
            ":quarkusAppPartsBuild", TaskOutcome.FROM_CACHE,
            ":quarkusDependenciesBuild", TaskOutcome.SUCCESS,
            ":quarkusBuild", TaskOutcome.FROM_CACHE,
            ":build", TaskOutcome.UP_TO_DATE);

    @InjectSoftAssertions
    SoftAssertions soft;

    @Test
    void envChangeInvalidatesBuild() throws Exception {
        // Declare the environment variables FOO_ENV_VAR and FROM_DOT_ENV_FILE as relevant for the build.
        prepareGradleBuildProject(String.join("\n",
                "cachingRelevantProperties.add(\"FOO_ENV_VAR\")",
                "cachingRelevantProperties.add(\"FROM_DOT_ENV_FILE\")"));

        String[] arguments = List.of("build", "--info", "--stacktrace", "--build-cache",
                "-Dquarkus.package.jar.type=fast-jar",
                "-Dquarkus.randomized.value=" + UUID.randomUUID())
                .toArray(new String[0]);

        Map<String, String> env = Map.of();
        assertBuildResult("initial", buildResult(env, rerunTasks(arguments)), ALL_SUCCESS);
        assertBuildResult("initial rebuild", buildResult(env, arguments), ALL_UP_TO_DATE);

        // Change the relevant environment, must rebuild
        env = Map.of("FOO_ENV_VAR", "some-value");
        assertBuildResult("set FOO_ENV_VAR", buildResult(env, arguments), ALL_SUCCESS);
        assertBuildResult("set FOO_ENV_VAR rebuild", buildResult(env, arguments), ALL_UP_TO_DATE);

        // Change the environment file again, must rebuild
        env = Map.of("FOO_ENV_VAR", "some-other-value");
        assertBuildResult("change FOO_ENV_VAR", buildResult(env, arguments), ALL_SUCCESS);
        assertBuildResult("change FOO_ENV_VAR rebuild", buildResult(env, arguments), ALL_UP_TO_DATE);

        // Change an unrelated environment variable, all up-to-date
        env = Map.of("FOO_ENV_VAR", "some-other-value", "SOME_UNRELATED", "meep");
        assertBuildResult("SOME_UNRELATED", buildResult(env, arguments), ALL_UP_TO_DATE);
    }

    @Test
    //@DisabledOnOs(OS.WINDOWS)
    @Disabled("This test is going to be unstable until we fix our build reproducibility")
    void dotEnvChangeInvalidatesBuild() throws Exception {
        var dotEnvFile = Paths.get(System.getProperty("user.dir"), ".env");
        // If the local environment has a ~/.env file, then skip this test - do not mess up a user's environment.
        assumeThat(dotEnvFile)
                .describedAs("Gradle plugin CachingTest.dotEnvChangeInvalidatesBuild requires missing ~/.env file");

        try {
            // Declare the environment variables FOO_ENV_VAR and FROM_DOT_ENV_FILE as relevant for the build.
            prepareGradleBuildProject(String.join("\n",
                    "cachingRelevantProperties.add(\"FOO_ENV_VAR\")",
                    "cachingRelevantProperties.add(\"FROM_DOT_ENV_FILE\")"));

            String[] arguments = List.of("build", "--info", "--stacktrace", "--build-cache",
                    "-Dquarkus.package.jar.type=fast-jar",
                    "-Dquarkus.randomized.value=" + UUID.randomUUID())
                    .toArray(new String[0]);

            Map<String, String> env = Map.of();

            assertBuildResult("initial", buildResult(env, rerunTasks(arguments)), ALL_SUCCESS);
            assertBuildResult("initial rebuild", buildResult(env, arguments), ALL_UP_TO_DATE);

            // Change the .env file, must rebuild

            Files.write(dotEnvFile, List.of("FROM_DOT_ENV_FILE=env file value"));
            assertBuildResult("set FROM_DOT_ENV_FILE", buildResult(env, arguments), ALL_SUCCESS);
            assertBuildResult("set FROM_DOT_ENV_FILE rebuild", buildResult(env, arguments), ALL_UP_TO_DATE);

            // Change the .env file again, must rebuild

            Files.write(dotEnvFile, List.of("FROM_DOT_ENV_FILE=new value"));
            assertBuildResult("change FROM_DOT_ENV_FILE", buildResult(env, arguments), ALL_SUCCESS);
            assertBuildResult("change FROM_DOT_ENV_FILE rebuild", buildResult(env, arguments), ALL_UP_TO_DATE);

            // OTHER_ENV_VAR is not declared as relevant for the build, skipping its check
            Files.write(dotEnvFile, List.of("FROM_DOT_ENV_FILE=new value", "OTHER_ENV_VAR=hello"));
            assertBuildResult("OTHER_ENV_VAR", buildResult(env, arguments), ALL_UP_TO_DATE);

            // remove relevant var from .env file
            Files.write(dotEnvFile, List.of("OTHER_ENV_VAR=hello"));
            assertBuildResult("remove FROM_DOT_ENV_FILE", buildResult(env, arguments), FROM_CACHE);

            // Delete the .env file, must rebuild

            Files.deleteIfExists(dotEnvFile);

            BuildResult result = buildResult(env, arguments);
            assertBuildResult("delete .env file", result, ALL_UP_TO_DATE);
        } finally {
            Files.deleteIfExists(dotEnvFile);
        }
    }

    static Stream<Arguments> gradleCaching() {
        return Stream.of("fast-jar", "uber-jar", "mutable-jar", "legacy-jar", "native-sources")
                .flatMap(packageType -> Stream.of(arguments(packageType, true), arguments(packageType, false)))
                .flatMap(args -> Stream.of(arguments(args.get()[0], args.get()[1], null),
                        arguments(args.get()[0], args.get()[1], "some-output-dir")));
    }

    @ParameterizedTest
    @MethodSource
    void gradleCaching(String packageType, boolean simulateCI, String outputDir, @TempDir Path saveDir) throws Exception {
        prepareGradleBuildProject("");

        Map<String, String> env = cachingTestEnvironment(simulateCI);

        List<String> args = new ArrayList<>();
        Collections.addAll(args, "build", "--info", "--stacktrace", "--build-cache", "--no-configuration-cache");
        if (packageType.equals("native-sources")) {
            args.add("-Dquarkus.native.enabled=true");
            args.add("-Dquarkus.native.sources-only=true");
            args.add("-Dquarkus.package.jar.enabled=false");
        } else {
            args.add("-Dquarkus.package.jar.type=" + packageType);
        }
        if (outputDir != null) {
            args.add("-Dquarkus.package.output-directory=" + outputDir);
        }
        String[] arguments = args.toArray(new String[0]);

        assertBuildResult("initial", buildResult(env, rerunTasks(arguments)), ALL_SUCCESS);
        assertBuildResult("initial rebuild", buildResult(env, arguments), ALL_UP_TO_DATE);

        // Purge the whole build/ directory

        Path buildDir = testProjectDir.resolve("build");

        Path saveBuildDir = saveDir.resolve("build");
        FileUtils.moveDirectory(buildDir.toFile(), saveBuildDir.toFile());

        soft.assertThat(buildDir).doesNotExist();

        // A follow-up 'build', without a build/ directory should fetch everything from the cache / pull the dependencies

        BuildResult result = buildResult(env, arguments);
        Map<String, TaskOutcome> taskResults = taskResults(result);

        Path quarkusBuildGen = Paths.get("quarkus-build", "gen");
        boolean isFastJar = "fast-jar".equals(packageType);
        boolean isFastOrLegacyJar = isFastJar || "legacy-jar".equals(packageType);
        Predicate<Path> filter = isFastOrLegacyJar ? p -> !p.startsWith(quarkusBuildGen) : p -> true;
        soft.assertThat(directoryContents(buildDir))
                .describedAs("output: %s", result.getOutput())
                .containsExactlyElementsOf(directoryContents(saveBuildDir, filter));

        soft.assertThat(taskResults)
                .describedAs("output: %s", result.getOutput())
                .containsEntry(":compileJava", TaskOutcome.FROM_CACHE)
                .containsEntry(":quarkusGenerateCode", TaskOutcome.FROM_CACHE)
                .doesNotContainKey(":quarkusGenerateCodeDev")
                .containsEntry(":quarkusAppPartsBuild", isFastOrLegacyJar ? TaskOutcome.FROM_CACHE : TaskOutcome.UP_TO_DATE)
                .containsEntry(":quarkusDependenciesBuild", isFastOrLegacyJar ? TaskOutcome.SUCCESS : TaskOutcome.UP_TO_DATE)
                .containsEntry(":quarkusBuild", simulateCI || isFastJar ? TaskOutcome.SUCCESS : TaskOutcome.FROM_CACHE);

        // A follow-up 'build' does nothing, everything's up-to-date

        result = buildResult(env, arguments);
        assertBuildResult("follow-up", result, ALL_UP_TO_DATE);
    }

    /**
     * Cache entries must be reusable from a different working directory. The serialized application
     * model embeds absolute paths, so hashing it as an {@code @InputFile} used to make the cache key a
     * function of the checkout directory, and nothing could be resolved from a cache populated
     * elsewhere - which is exactly the shared/remote cache case.
     */
    @Test
    void buildCacheIsReusedFromADifferentProjectDirectory(@TempDir Path otherProjectDir, @TempDir Path localCacheDir)
            throws Exception {
        prepareGradleBuildProject("");
        enableLocalBuildCache(testProjectDir, localCacheDir);

        Map<String, String> env = cachingTestEnvironment(false);
        String[] arguments = List.of("build", "--info", "--stacktrace", "--build-cache", "--no-configuration-cache",
                "-Dquarkus.package.jar.type=fast-jar")
                .toArray(new String[0]);

        assertBuildResult("populate cache", buildResult(env, rerunTasks(arguments)), ALL_SUCCESS);

        // Same sources and same shared cache, but a different project directory.
        FileUtils.copyDirectory(testProjectDir.toFile(), otherProjectDir.toFile());
        FileUtils.deleteDirectory(otherProjectDir.resolve("build").toFile());
        FileUtils.deleteDirectory(otherProjectDir.resolve(".gradle").toFile());

        BuildResult result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(otherProjectDir.toFile())
                .withArguments(defaultGradleArguments(List.of(arguments)))
                .withEnvironment(env)
                .build();

        soft.assertThat(taskResults(result))
                .describedAs("output: %s", result.getOutput())
                .containsEntry(":quarkusGenerateCode", TaskOutcome.FROM_CACHE)
                .containsEntry(":quarkusAppPartsBuild", TaskOutcome.FROM_CACHE);
    }

    /**
     * The same relocation guarantee for a multi-module build. The jar of a sibling module lives under
     * the root directory rather than under the consuming module's own project or build directory, so a
     * fix that only accounts for the latter still leaks the checkout path through that dependency.
     */
    @Test
    void buildCacheIsReusedFromADifferentProjectDirectoryForAMultiModuleBuild(@TempDir Path otherProjectDir,
            @TempDir Path localCacheDir) throws Exception {
        prepareGradleBuildProject("");
        makeMultiModule(testProjectDir);
        enableLocalBuildCache(testProjectDir, localCacheDir);

        Map<String, String> env = cachingTestEnvironment(false);
        String[] arguments = List.of("build", "--info", "--stacktrace", "--build-cache", "--no-configuration-cache",
                "-Dquarkus.package.jar.type=fast-jar")
                .toArray(new String[0]);

        assertBuildResult("populate cache", buildResult(env, rerunTasks(arguments)), Map.of(
                ":app:quarkusGenerateCode", TaskOutcome.SUCCESS,
                ":app:quarkusAppPartsBuild", TaskOutcome.SUCCESS,
                ":app:quarkusBuild", TaskOutcome.SUCCESS));

        FileUtils.copyDirectory(testProjectDir.toFile(), otherProjectDir.toFile());
        FileUtils.deleteDirectory(otherProjectDir.resolve("build").toFile());
        FileUtils.deleteDirectory(otherProjectDir.resolve(".gradle").toFile());
        FileUtils.deleteDirectory(otherProjectDir.resolve("library").resolve("build").toFile());
        FileUtils.deleteDirectory(otherProjectDir.resolve("app").resolve("build").toFile());

        BuildResult result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(otherProjectDir.toFile())
                .withArguments(defaultGradleArguments(List.of(arguments)))
                .withEnvironment(env)
                .build();

        soft.assertThat(taskResults(result))
                .describedAs("output: %s", result.getOutput())
                // the model task itself is cacheable, so the build from the other directory resolves the
                // entry the first one stored instead of resolving the whole classpath again
                .containsEntry(":app:quarkusGenerateAppModel", TaskOutcome.FROM_CACHE)
                // the model task itself is cacheable, so the build from the other directory resolves the
                // entry the first one stored instead of resolving the whole classpath again
                .containsEntry(":app:quarkusGenerateAppModel", TaskOutcome.FROM_CACHE)
                .containsEntry(":app:quarkusGenerateCode", TaskOutcome.FROM_CACHE)
                .containsEntry(":app:quarkusAppPartsBuild", TaskOutcome.FROM_CACHE);
    }

    /**
     * Turns the single-module fixture into a multi-module build: the Quarkus application is moved into
     * an {@code app} subproject and given a dependency on a sibling {@code library} subproject.
     * <p>
     * The application has to be a subproject rather than the root for this to test anything: the jar of
     * a sibling module then lies outside the application's own project directory, and is only covered
     * by expressing it relative to the root of the build.
     *
     * @return the path of the application subproject
     */
    private static Path makeMultiModule(Path projectDir) throws IOException {
        Path app = projectDir.resolve("app");
        Files.createDirectories(app);
        FileUtils.moveDirectory(projectDir.resolve("src").toFile(), app.resolve("src").toFile());
        FileUtils.moveFile(projectDir.resolve("build.gradle.kts").toFile(), app.resolve("build.gradle.kts").toFile());

        Path library = projectDir.resolve("library");
        Path sources = library.resolve("src/main/java/org/acme/lib");
        Files.createDirectories(sources);
        Files.writeString(sources.resolve("Lib.java"), """
                package org.acme.lib;

                public class Lib {
                    public String greet() {
                        return "hello";
                    }
                }
                """);
        Files.writeString(library.resolve("build.gradle.kts"), """
                plugins {
                    java
                }

                repositories {
                    mavenLocal()
                    mavenCentral()
                }
                """);

        Path settings = projectDir.resolve("settings.gradle.kts");
        Files.writeString(settings, Files.readString(settings) + "\ninclude(\"app\")\ninclude(\"library\")\n");

        Path buildScript = app.resolve("build.gradle.kts");
        Files.writeString(buildScript, Files.readString(buildScript)
                .replace("    implementation(\"jakarta.inject:jakarta.inject-api:2.0.1\")",
                        "    implementation(\"jakarta.inject:jakarta.inject-api:2.0.1\")\n"
                                + "    implementation(project(\":library\"))"));
        return app;
    }

    /**
     * Points the build at a build cache directory shared between project directories, so a build run
     * from one directory can resolve entries produced by another.
     */
    private static void enableLocalBuildCache(Path projectDir, Path cacheDir) throws IOException {
        Path settings = projectDir.resolve("settings.gradle.kts");
        String directory = cacheDir.toAbsolutePath().toString().replace("\\", "\\\\");
        Files.writeString(settings, Files.readString(settings) + String.format(
                "%nbuildCache {%n    local {%n        directory = \"%s\"%n        isEnabled = true%n    }%n}%n",
                directory));
    }

    private static Map<String, String> cachingTestEnvironment(boolean simulateCI) {
        Map<String, String> env = new HashMap<>(System.getenv());
        if (simulateCI) {
            env.put("CI", "yes");
        } else {
            env.remove("CI");
        }
        return env;
    }

    private static String[] rerunTasks(String[] arguments) {
        String[] args = Arrays.copyOf(arguments, arguments.length + 1);
        args[arguments.length] = "--rerun-tasks";
        return args;
    }

    private void assertBuildResult(String step, BuildResult result,
            Map<String, TaskOutcome> expected) {
        Map<String, TaskOutcome> taskResults = taskResults(result);
        soft.assertThat(taskResults)
                .describedAs("output: %s\n\nSTEP: %s", result.getOutput(), step)
                .containsAllEntriesOf(expected);
    }

    private void prepareGradleBuildProject(String additionalQuarkusConfig) throws IOException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/caching/main");

        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());

        // Randomize the build script
        String buildScript = Files.readString(testProjectDir.resolve("build.gradle.kts"));
        buildScript = buildScript.replace("// ADDITIONAL_CONFIG", additionalQuarkusConfig);
        Files.writeString(testProjectDir.resolve("build.gradle.kts"), buildScript);

        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());
    }

    static Map<String, TaskOutcome> taskResults(BuildResult result) {
        return result.getTasks().stream().collect(Collectors.toMap(BuildTask::getPath, BuildTask::getOutcome));
    }

    static List<Path> directoryContents(Path dir) throws IOException {
        return directoryContents(dir, p -> true);
    }

    static List<Path> directoryContents(Path dir, Predicate<Path> include) throws IOException {
        try (Stream<Path> saved = Files.walk(dir)) {
            return saved.map(dir::relativize).filter(include).sorted(Comparator.comparing(Path::toString))
                    .filter(p -> !p.toString().startsWith("reports" + File.separator + "configuration-cache" + File.separator))
                    .collect(Collectors.toList());
        }
    }
}
