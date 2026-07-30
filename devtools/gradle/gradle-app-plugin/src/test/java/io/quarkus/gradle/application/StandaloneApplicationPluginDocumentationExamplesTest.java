package io.quarkus.gradle.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.gradle.testing.BaseGradleTest;

class StandaloneApplicationPluginDocumentationExamplesTest extends BaseGradleTest {

    private static final String RESOURCE_ROOT = "documentation/standalone-application-plugin";
    private static final Pattern START_TAG = Pattern.compile("^\\s*// tag::([a-z0-9-]+)\\[\\]\\s*$");
    private static final Pattern END_TAG = Pattern.compile("^\\s*// end::([a-z0-9-]+)\\[\\]\\s*$");

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void documentationFixtureConfiguresExpectedGraph(Fixture fixture) throws Exception {
        Path projectDirectory = materialize(fixture);

        Instant started = Instant.now();
        BuildResult result = runner(projectDirectory, fixture.arguments()).build();
        System.out.printf("Documentation fixture %s completed in %s%n",
                fixture.name(), Duration.between(started, Instant.now()));

        assertThat(fixture.arguments()).contains("--dry-run");
        assertOnlySkippedTasks(result);
        assertThat(result.getOutput()).contains(fixture.expectedOutput().toArray(String[]::new));
        if (!fixture.unexpectedOutput().isEmpty()) {
            assertThat(result.getOutput()).doesNotContain(fixture.unexpectedOutput().toArray(String[]::new));
        }

        if (fixture.verifyConfigurationCacheReuse()) {
            BuildResult reused = runner(projectDirectory, fixture.arguments()).build();
            assertConfigurationCacheReused(reused);
            assertOnlySkippedTasks(reused);
            assertThat(reused.getOutput()).contains(fixture.expectedOutput().toArray(String[]::new));
            if (!fixture.unexpectedOutput().isEmpty()) {
                assertThat(reused.getOutput()).doesNotContain(fixture.unexpectedOutput().toArray(String[]::new));
            }
        }
    }

    @Test
    void displayedTagsAreUniqueAndHaveGroovyKotlinParity() throws Exception {
        for (FixturePair pair : fixturePairs()) {
            Set<String> groovyTags = tags(pair.groovy());
            Set<String> kotlinTags = tags(pair.kotlin());

            assertThat(groovyTags)
                    .as("Groovy/Kotlin public tags for %s", pair.name())
                    .containsExactlyElementsOf(kotlinTags);
        }
    }

    private static Stream<Fixture> fixtures() {
        return Stream.of(
                new Fixture("basic-groovy", "basic/groovy",
                        List.of("assemble", "--dry-run"),
                        List.of(":quarkusAssembledBuild SKIPPED", ":assemble SKIPPED"),
                        List.of(":quarkusProductionBuild SKIPPED", ":quarkusCustomizedBuild SKIPPED"), false),
                new Fixture("basic-kotlin", "basic/kotlin",
                        List.of("assemble", "--dry-run"),
                        List.of(":quarkusAssembledBuild SKIPPED", ":assemble SKIPPED"),
                        List.of(":quarkusProductionBuild SKIPPED", ":quarkusCustomizedBuild SKIPPED"), false),
                new Fixture("testing-groovy", "testing/groovy",
                        List.of("tasks", "--all", "--dry-run"),
                        List.of(":tasks SKIPPED"),
                        false),
                new Fixture("testing-kotlin", "testing/kotlin",
                        List.of("tasks", "--all", "--dry-run"),
                        List.of(":tasks SKIPPED"),
                        false),
                new Fixture("package-consumer-groovy", "package-consumer/groovy",
                        List.of(":distribution:copySelectedServerPackage", ":distribution:packageDistribution",
                                ":distribution:packageDistributionTar", "--dry-run"),
                        List.of(":app:quarkusServerBuild SKIPPED", ":distribution:packageDistribution SKIPPED",
                                ":distribution:packageDistributionTar SKIPPED",
                                ":distribution:copySelectedServerPackage SKIPPED"),
                        true),
                new Fixture("package-consumer-kotlin", "package-consumer/kotlin",
                        List.of(":distribution:copySelectedServerPackage", ":distribution:packageDistribution",
                                ":distribution:packageDistributionTar", "--dry-run"),
                        List.of(":app:quarkusServerBuild SKIPPED", ":distribution:packageDistribution SKIPPED",
                                ":distribution:packageDistributionTar SKIPPED",
                                ":distribution:copySelectedServerPackage SKIPPED"),
                        true),
                new Fixture("advanced-groovy", "advanced/groovy",
                        List.of("quarkusDiagnostics", "quarkusTrainedStartupArchiveValidation", "--dry-run"),
                        List.of(":quarkusApplicationShowModel SKIPPED",
                                ":quarkusDiagnosticsShowEffectiveConfig SKIPPED", ":quarkusDiagnostics SKIPPED",
                                ":quarkusTrainedTrainingStartupArchiveTrainingMetadata SKIPPED",
                                ":training SKIPPED", ":quarkusTrainedStartupArchiveValidation SKIPPED"),
                        false),
                new Fixture("advanced-kotlin", "advanced/kotlin",
                        List.of("quarkusDiagnostics", "quarkusTrainedStartupArchiveValidation", "--dry-run"),
                        List.of(":quarkusApplicationShowModel SKIPPED",
                                ":quarkusDiagnosticsShowEffectiveConfig SKIPPED", ":quarkusDiagnostics SKIPPED",
                                ":quarkusTrainedTrainingStartupArchiveTrainingMetadata SKIPPED",
                                ":training SKIPPED", ":quarkusTrainedStartupArchiveValidation SKIPPED"),
                        false));
    }

    private Path materialize(Fixture fixture) throws IOException, URISyntaxException {
        var resource = Objects.requireNonNull(
                getClass().getClassLoader().getResource(RESOURCE_ROOT + "/" + fixture.resourceDirectory()),
                "Missing documentation fixture " + fixture.resourceDirectory());
        Path source = Path.of(resource.toURI());
        Path destination = testProjectDir.resolve(fixture.name());
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path target = destination.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        return destination;
    }

    private Set<String> tags(String relativePath) throws Exception {
        var resource = Objects.requireNonNull(
                getClass().getClassLoader().getResource(RESOURCE_ROOT + "/" + relativePath),
                "Missing tagged documentation source " + relativePath);
        List<String> lines = Files.readAllLines(Path.of(resource.toURI()));
        Set<String> starts = new LinkedHashSet<>();
        Set<String> ends = new LinkedHashSet<>();
        Deque<String> openTags = new ArrayDeque<>();
        for (String line : lines) {
            var start = START_TAG.matcher(line);
            if (start.matches()) {
                assertThat(starts.add(start.group(1)))
                        .as("unique start tag %s in %s", start.group(1), relativePath)
                        .isTrue();
                openTags.push(start.group(1));
                continue;
            }
            var end = END_TAG.matcher(line);
            if (end.matches()) {
                assertThat(ends.add(end.group(1)))
                        .as("unique end tag %s in %s", end.group(1), relativePath)
                        .isTrue();
                assertThat(openTags)
                        .as("open tag for %s in %s", end.group(1), relativePath)
                        .isNotEmpty();
                assertThat(openTags.pop())
                        .as("properly nested tag ending with %s in %s", end.group(1), relativePath)
                        .isEqualTo(end.group(1));
            }
        }
        assertThat(ends).as("matching end tags in %s", relativePath).containsExactlyInAnyOrderElementsOf(starts);
        assertThat(openTags).as("no unclosed tags in %s", relativePath).isEmpty();

        String displayedSource = String.join("\n", lines);
        assertThat(displayedSource)
                .doesNotContain("999-SNAPSHOT", "test-only", "withPluginClasspath");
        return starts;
    }

    private static List<FixturePair> fixturePairs() {
        return List.of(
                new FixturePair("settings", "basic/groovy/settings.gradle", "basic/kotlin/settings.gradle.kts"),
                new FixturePair("basic", "basic/groovy/build.gradle", "basic/kotlin/build.gradle.kts"),
                new FixturePair("testing", "testing/groovy/build.gradle", "testing/kotlin/build.gradle.kts"),
                new FixturePair("package-consumer-settings",
                        "package-consumer/groovy/settings.gradle",
                        "package-consumer/kotlin/settings.gradle.kts"),
                new FixturePair("package-producer",
                        "package-consumer/groovy/app/build.gradle",
                        "package-consumer/kotlin/app/build.gradle.kts"),
                new FixturePair("package-consumer",
                        "package-consumer/groovy/distribution/build.gradle",
                        "package-consumer/kotlin/distribution/build.gradle.kts"),
                new FixturePair("advanced", "advanced/groovy/build.gradle", "advanced/kotlin/build.gradle.kts"));
    }

    private static GradleRunner runner(Path projectDirectory, List<String> arguments) {
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDirectory.toFile())
                .withArguments(Stream.concat(arguments.stream(), Stream.of(
                        CONFIGURATION_CACHE,
                        ISOLATED_PROJECTS,
                        STACKTRACE,
                        "-PquarkusPluginVersion=999-SNAPSHOT"))
                        .toList());
    }

    private static void assertOnlySkippedTasks(BuildResult result) {
        // TestKit does not expose BuildTask instances for a dry run, so verify
        // the dry-run graph text and reject the marker used for executed tasks.
        List<String> dryRunTasks = result.getOutput().lines()
                .map(String::trim)
                .filter(line -> line.startsWith(":") && line.endsWith(" SKIPPED"))
                .toList();
        assertThat(dryRunTasks).as("dry-run task graph").isNotEmpty();
        assertThat(result.getOutput()).doesNotContain("> Task ");
    }

    private record Fixture(String name, String resourceDirectory, List<String> arguments,
            List<String> expectedOutput, List<String> unexpectedOutput, boolean verifyConfigurationCacheReuse) {
        private Fixture(String name, String resourceDirectory, List<String> arguments,
                List<String> expectedOutput, boolean verifyConfigurationCacheReuse) {
            this(name, resourceDirectory, arguments, expectedOutput, List.of(), verifyConfigurationCacheReuse);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private record FixturePair(String name, String groovy, String kotlin) {
    }
}
