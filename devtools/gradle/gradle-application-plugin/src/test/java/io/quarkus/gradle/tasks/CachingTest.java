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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
public class CachingTest {
    private static final Map<String, TaskOutcome> ALL_SUCCESS = Map.of(
            ":quarkusGenerateCode", TaskOutcome.SUCCESS,
            ":quarkusGenerateCodeDev", TaskOutcome.SUCCESS,
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
            ":quarkusGenerateCodeDev", TaskOutcome.SUCCESS,
            ":quarkusGenerateCodeTests", TaskOutcome.FROM_CACHE,
            ":quarkusAppPartsBuild", TaskOutcome.FROM_CACHE,
            ":quarkusDependenciesBuild", TaskOutcome.SUCCESS,
            ":quarkusBuild", TaskOutcome.SUCCESS,
            ":build", TaskOutcome.SUCCESS);

    @InjectSoftAssertions
    SoftAssertions soft;

    @TempDir
    Path testProjectDir;

    @Test
    void envChangeInvalidatesBuild() throws Exception {
        // Declare the environment variables FOO_ENV_VAR and FROM_DOT_ENV_FILE as relevant for the build.
        prepareGradleBuildProject(String.join("\n",
                "cachingRelevantProperties.add(\"FOO_ENV_VAR\")",
                "cachingRelevantProperties.add(\"FROM_DOT_ENV_FILE\")"));

        String[] arguments = List.of("build", "--info", "--stacktrace", "--build-cache", "--configuration-cache",
                "-Dquarkus.package.jar.type=fast-jar",
                "-Dquarkus.randomized.value=" + UUID.randomUUID())
                .toArray(new String[0]);

        Map<String, String> env = Map.of();
        assertBuildResult("initial", gradleBuild(rerunTasks(arguments), env), ALL_SUCCESS);
        assertBuildResult("initial rebuild", gradleBuild(arguments, env), ALL_UP_TO_DATE);

        // Change the relevant environment, must rebuild
        env = Map.of("FOO_ENV_VAR", "some-value");
        assertBuildResult("set FOO_ENV_VAR", gradleBuild(arguments, env), ALL_SUCCESS);
        assertBuildResult("set FOO_ENV_VAR rebuild", gradleBuild(arguments, env), ALL_UP_TO_DATE);

        // Change the environment file again, must rebuild
        env = Map.of("FOO_ENV_VAR", "some-other-value");
        assertBuildResult("change FOO_ENV_VAR", gradleBuild(arguments, env), ALL_SUCCESS);
        assertBuildResult("change FOO_ENV_VAR rebuild", gradleBuild(arguments, env), ALL_UP_TO_DATE);

        // Change an unrelated environment variable, all up-to-date
        env = Map.of("FOO_ENV_VAR", "some-other-value", "SOME_UNRELATED", "meep");
        assertBuildResult("SOME_UNRELATED", gradleBuild(arguments, env), ALL_UP_TO_DATE);
    }

    @Test
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

            String[] arguments = List.of("build", "--info", "--stacktrace", "--build-cache", "--configuration-cache",
                    "-Dquarkus.package.jar.type=fast-jar",
                    "-Dquarkus.randomized.value=" + UUID.randomUUID())
                    .toArray(new String[0]);

            Map<String, String> env = Map.of();

            assertBuildResult("initial", gradleBuild(rerunTasks(arguments), env), ALL_SUCCESS);
            assertBuildResult("initial rebuild", gradleBuild(arguments, env), ALL_UP_TO_DATE);

            // Change the .env file, must rebuild

            Files.write(dotEnvFile, List.of("FROM_DOT_ENV_FILE=env file value"));
            assertBuildResult("set FROM_DOT_ENV_FILE", gradleBuild(arguments, env), ALL_SUCCESS);
            assertBuildResult("set FROM_DOT_ENV_FILE rebuild", gradleBuild(arguments, env), ALL_UP_TO_DATE);

            // Change the .env file again, must rebuild

            Files.write(dotEnvFile, List.of("FROM_DOT_ENV_FILE=new value"));
            assertBuildResult("change FROM_DOT_ENV_FILE", gradleBuild(arguments, env), ALL_SUCCESS);
            assertBuildResult("change FROM_DOT_ENV_FILE rebuild", gradleBuild(arguments, env), ALL_UP_TO_DATE);

            // OTHER_ENV_VAR is not declared as relevant for the build, skipping its check
            Files.write(dotEnvFile, List.of("FROM_DOT_ENV_FILE=new value", "OTHER_ENV_VAR=hello"));
            assertBuildResult("OTHER_ENV_VAR", gradleBuild(arguments, env), ALL_UP_TO_DATE);

            // remove relevant var from .env file
            Files.write(dotEnvFile, List.of("OTHER_ENV_VAR=hello"));
            assertBuildResult("remove FROM_DOT_ENV_FILE", gradleBuild(arguments, env), FROM_CACHE);

            // Delete the .env file, must rebuild

            Files.deleteIfExists(dotEnvFile);

            BuildResult result = gradleBuild(arguments, env);
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

        Map<String, String> env = simulateCI ? Map.of("CI", "yes") : Map.of();

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

        assertBuildResult("initial", gradleBuild(rerunTasks(arguments), env), ALL_SUCCESS);
        assertBuildResult("initial rebuild", gradleBuild(arguments, env), ALL_UP_TO_DATE);

        // Purge the whole build/ directory

        Path buildDir = testProjectDir.resolve("build");

        Path saveBuildDir = saveDir.resolve("build");
        FileUtils.moveDirectory(buildDir.toFile(), saveBuildDir.toFile());

        soft.assertThat(buildDir).doesNotExist();

        // A follow-up 'build', without a build/ directory should fetch everything from the cache / pull the dependencies

        BuildResult result = gradleBuild(arguments, env);
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
                .containsEntry(":quarkusGenerateCodeDev", TaskOutcome.UP_TO_DATE)
                .containsEntry(":quarkusAppPartsBuild", isFastOrLegacyJar ? TaskOutcome.FROM_CACHE : TaskOutcome.UP_TO_DATE)
                .containsEntry(":quarkusDependenciesBuild", isFastOrLegacyJar ? TaskOutcome.SUCCESS : TaskOutcome.UP_TO_DATE)
                .containsEntry(":quarkusBuild", simulateCI || isFastJar ? TaskOutcome.SUCCESS : TaskOutcome.FROM_CACHE);

        // A follow-up 'build' does nothing, everything's up-to-date

        result = gradleBuild(arguments, env);
        assertBuildResult("follow-up", result, ALL_UP_TO_DATE);
    }

    private static String[] rerunTasks(String[] arguments) {
        String[] args = Arrays.copyOf(arguments, arguments.length + 1);
        args[arguments.length] = "--rerun-tasks";
        return args;
    }

    private BuildResult gradleBuild(String[] arguments, Map<String, String> env) {
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(arguments)
                .withEnvironment(env)
                .build();
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
