package io.quarkus.gradle.tasks;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(SoftAssertionsExtension.class)
public class CachingTest {
    @InjectSoftAssertions
    SoftAssertions soft;

    @TempDir
    Path testProjectDir;
    @TempDir
    Path saveDir;

    static Stream<Arguments> gradleCaching() {
        return Stream.of("fast-jar", "uber-jar", "mutable-jar", "legacy-jar", "native-sources")
                .flatMap(packageType -> Stream.of(arguments(packageType, true), arguments(packageType, false)))
                .flatMap(args -> Stream.of(arguments(args.get()[0], args.get()[1], null),
                        arguments(args.get()[0], args.get()[1], "some-output-dir")));
    }

    @ParameterizedTest
    @MethodSource
    void gradleCaching(String packageType, boolean simulateCI, String outputDir) throws Exception {
        URL url = getClass().getClassLoader().getResource("io/quarkus/gradle/tasks/caching/main");

        FileUtils.copyDirectory(new File(url.toURI()), testProjectDir.toFile());

        FileUtils.copyFile(new File("../gradle.properties"), testProjectDir.resolve("gradle.properties").toFile());

        Map<String, String> env = simulateCI ? Map.of("CI", "yes") : Map.of();

        List<String> args = new ArrayList<>();
        Collections.addAll(args, "build", "--info", "--stacktrace", "--build-cache", "--configuration-cache",
                "-Dquarkus.package.type=" + packageType);
        if (outputDir != null) {
            args.add("-Dquarkus.package.outputDirectory=" + outputDir);
        }
        String[] arguments = args.toArray(new String[0]);
        args.add("--rerun-tasks");
        String[] initialArguments = args.toArray(new String[0]);

        BuildResult result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(initialArguments)
                .withEnvironment(env)
                .build();
        Map<String, TaskOutcome> taskResults = taskResults(result);

        soft.assertThat(taskResults)
                .describedAs("output: %s", result.getOutput())
                .containsEntry(":quarkusGenerateCode", TaskOutcome.SUCCESS)
                .containsEntry(":quarkusGenerateCodeDev", TaskOutcome.SUCCESS)
                .containsEntry(":quarkusGenerateCodeTests", TaskOutcome.SUCCESS)
                .containsEntry(":quarkusAppPartsBuild", TaskOutcome.SUCCESS)
                .containsEntry(":quarkusDependenciesBuild", TaskOutcome.SUCCESS)
                .containsEntry(":quarkusBuild", TaskOutcome.SUCCESS)
                .containsEntry(":build", TaskOutcome.SUCCESS);

        // A follow-up 'build' does nothing, everything's up-to-date

        result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(arguments)
                .withEnvironment(env)
                .build();
        taskResults = taskResults(result);

        soft.assertThat(taskResults)
                .describedAs("output: %s", result.getOutput())
                .containsEntry(":quarkusGenerateCode", TaskOutcome.UP_TO_DATE)
                .containsEntry(":quarkusGenerateCodeDev", TaskOutcome.UP_TO_DATE)
                .containsEntry(":quarkusGenerateCodeTests", TaskOutcome.UP_TO_DATE)
                .containsEntry(":quarkusAppPartsBuild", TaskOutcome.UP_TO_DATE)
                .containsEntry(":quarkusDependenciesBuild", TaskOutcome.UP_TO_DATE)
                .containsEntry(":quarkusBuild", TaskOutcome.UP_TO_DATE)
                .containsEntry(":build", TaskOutcome.UP_TO_DATE);

        // Purge the whole build/ directory

        Path buildDir = testProjectDir.resolve("build");

        Path saveBuildDir = saveDir.resolve("build");
        FileUtils.moveDirectory(buildDir.toFile(), saveBuildDir.toFile());

        soft.assertThat(buildDir).doesNotExist();

        // A follow-up 'build', without a build/ directory should fetch everything from the cache / pull the dependencies

        result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(arguments)
                .withEnvironment(env)
                .build();
        taskResults = taskResults(result);

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

        result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(arguments)
                .withEnvironment(env)
                .build();
        taskResults = taskResults(result);

        soft.assertThat(taskResults)
                .describedAs("output: %s", result.getOutput())
                .containsEntry(":quarkusGenerateCode", TaskOutcome.UP_TO_DATE)
                .containsEntry(":quarkusGenerateCodeDev", TaskOutcome.UP_TO_DATE)
                .containsEntry(":quarkusGenerateCodeTests", TaskOutcome.UP_TO_DATE)
                .containsEntry(":quarkusAppPartsBuild", TaskOutcome.UP_TO_DATE)
                .containsEntry(":quarkusDependenciesBuild", TaskOutcome.UP_TO_DATE)
                .containsEntry(":quarkusBuild", TaskOutcome.UP_TO_DATE)
                .containsEntry(":build", TaskOutcome.UP_TO_DATE);
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
