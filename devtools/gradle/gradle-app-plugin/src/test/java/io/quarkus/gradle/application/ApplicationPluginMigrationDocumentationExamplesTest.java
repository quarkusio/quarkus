package io.quarkus.gradle.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

class ApplicationPluginMigrationDocumentationExamplesTest extends BaseGradleTest {

    private static final String RESOURCE_ROOT = "documentation/application-plugin-migration";
    private static final Pattern START_TAG = Pattern.compile("^\\s*// tag::([a-z0-9-]+)\\[\\]\\s*$");
    private static final Pattern END_TAG = Pattern.compile("^\\s*// end::([a-z0-9-]+)\\[\\]\\s*$");

    @ParameterizedTest(name = "{0}")
    @MethodSource("directFixtures")
    void directMigrationConfiguresDocumentedDslAndTaskRelationships(DirectFixture fixture) throws Exception {
        Path projectDirectory = materialize("direct", fixture.resourceDirectory(), "direct-" + fixture.dsl());

        BuildResult legacy = runner(projectDirectory, "tasks", "--all", NO_CONFIGURATION_CACHE).build();
        assertThat(legacy.getOutput()).contains("quarkusBuild", "quarkusDev", "quarkusIntTest");

        overlay(projectDirectory, fixture.migratedBuild(), fixture.buildFile());
        overlay(projectDirectory, fixture.migratedSettings(), fixture.settingsFile());

        BuildResult configured = runner(projectDirectory,
                "help",
                "--dry-run",
                NO_CONFIGURATION_CACHE,
                "-PverifyMigrationDocumentation").build();
        assertDryRunSelects(configured, ":help");

        List<String> cacheProbe = List.of(
                "help",
                "--dry-run",
                CONFIGURATION_CACHE,
                ISOLATED_PROJECTS);
        BuildResult first = runner(projectDirectory, cacheProbe).build();
        assertDryRunSelects(first, ":help");

        // Focused plugin tests own package, test and run-task execution. This
        // executable-documentation test owns the displayed DSL, its task
        // relationships and the configuration-cache transition for both DSLs.
        BuildResult reused = runner(projectDirectory, cacheProbe).build();
        assertConfigurationCacheReused(reused);
        assertDryRunSelects(reused, ":help");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("coexistenceFixtures")
    void legacyFirstCoexistenceHasTheDocumentedOwnershipBoundaries(CoexistenceFixture fixture) throws Exception {
        Path projectDirectory = materialize("coexistence", fixture.resourceDirectory(),
                "coexistence-" + fixture.dsl());

        BuildResult ownership = runner(projectDirectory, "verifyMigrationOwnership", NO_CONFIGURATION_CACHE,
                "-PstandaloneParticipatesInAssemble").build();
        assertThat(ownership.getOutput())
                .contains("Both 'io.quarkus.application' and legacy 'io.quarkus' are applied")
                .contains("migration mode")
                .contains("Legacy owns Gradle Test task instrumentation");
        assertThat(ownership.task(":verifyMigrationOwnership").getOutcome()).isEqualTo(SUCCESS);

        if (fixture.verifyNativeIntegrationRestriction()) {
            BuildResult nativeRestriction = runner(projectDirectory, "tasks", "--all", NO_CONFIGURATION_CACHE,
                    "-PverifyNativeIntegrationRestriction").buildAndFail();
            assertThat(nativeRestriction.getOutput())
                    .contains("Quarkus integration-test suite 'nativeIntegrationTest'")
                    .contains("named native build 'native'")
                    .contains("legacy 'io.quarkus' owns Gradle Test task instrumentation")
                    .contains("apply only 'io.quarkus.application'");
        }

        overlay(projectDirectory, fixture.standaloneFirstBuild(), fixture.buildFile());
        BuildResult reversed = runner(projectDirectory, "tasks", "--all", NO_CONFIGURATION_CACHE).buildAndFail();
        assertThat(reversed.getOutput())
                .contains("Legacy plugin 'io.quarkus' must be applied before 'io.quarkus.application'")
                .contains("Apply 'io.quarkus' first");

        overlay(projectDirectory, fixture.standaloneOnlyBuild(), fixture.buildFile());
        BuildResult finalOwnership = runner(projectDirectory, "verifyStandaloneOwnership", NO_CONFIGURATION_CACHE).build();
        assertThat(finalOwnership.task(":verifyStandaloneOwnership").getOutcome()).isEqualTo(SUCCESS);
    }

    @Test
    void displayedTagsAreBalancedUniqueLeakFreeAndHaveGroovyKotlinParity() throws Exception {
        Set<String> allTags = new LinkedHashSet<>();
        for (FixturePair pair : fixturePairs()) {
            TaggedSource groovy = taggedSource(pair.groovy());
            TaggedSource kotlin = taggedSource(pair.kotlin());

            assertThat(groovy.tags())
                    .as("Groovy/Kotlin public tags for %s", pair.name())
                    .isNotEmpty()
                    .containsExactlyElementsOf(kotlin.tags());
            for (String tag : groovy.tags()) {
                assertThat(allTags.add(tag))
                        .as("globally unique public tag %s", tag)
                        .isTrue();
            }
            assertDisplayedSourceHasNoTestMechanics(pair.groovy(), groovy.displayedSource());
            assertDisplayedSourceHasNoTestMechanics(pair.kotlin(), kotlin.displayedSource());
            if (pair.name().equals("direct standalone build")) {
                assertIgnoredEntriesExample(groovy.displayedSource());
                assertIgnoredEntriesExample(kotlin.displayedSource());
            }
        }
    }

    private static Stream<DirectFixture> directFixtures() {
        return Stream.of(
                new DirectFixture("groovy", "groovy", "build.gradle", "build.gradle.quarkus4",
                        "settings.gradle", "settings.gradle.quarkus4"),
                new DirectFixture("kotlin", "kotlin", "build.gradle.kts", "build.gradle.kts.quarkus4",
                        "settings.gradle.kts", "settings.gradle.kts.quarkus4"));
    }

    private static Stream<CoexistenceFixture> coexistenceFixtures() {
        return Stream.of(
                new CoexistenceFixture("groovy", "groovy", "build.gradle", "build.gradle.standalone-first",
                        "build.gradle.standalone-only", true),
                new CoexistenceFixture("kotlin", "kotlin", "build.gradle.kts", "build.gradle.kts.standalone-first",
                        "build.gradle.kts.standalone-only", false));
    }

    private Path materialize(String migrationShape, String resourceDirectory, String destinationName)
            throws IOException, URISyntaxException {
        var resource = Objects.requireNonNull(
                getClass().getClassLoader().getResource(RESOURCE_ROOT + "/" + migrationShape + "/" + resourceDirectory),
                "Missing application migration fixture " + migrationShape + "/" + resourceDirectory);
        Path source = Path.of(resource.toURI());
        Path destination = testProjectDir.resolve(destinationName);
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

    private static void overlay(Path projectDirectory, String sourceFile, String targetFile) throws IOException {
        Path source = projectDirectory.resolve(sourceFile);
        Path target = projectDirectory.resolve(targetFile);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        assertThat(target).hasSameTextualContentAs(source);
    }

    private TaggedSource taggedSource(String relativePath) throws Exception {
        var resource = Objects.requireNonNull(
                getClass().getClassLoader().getResource(RESOURCE_ROOT + "/" + relativePath),
                "Missing tagged application migration source " + relativePath);
        List<String> lines = Files.readAllLines(Path.of(resource.toURI()));
        Set<String> starts = new LinkedHashSet<>();
        Set<String> ends = new LinkedHashSet<>();
        Deque<String> openTags = new ArrayDeque<>();
        StringBuilder displayedSource = new StringBuilder();
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
                continue;
            }
            if (!openTags.isEmpty()) {
                displayedSource.append(line).append('\n');
            }
        }
        assertThat(ends).as("matching end tags in %s", relativePath).containsExactlyInAnyOrderElementsOf(starts);
        assertThat(openTags).as("no unclosed tags in %s", relativePath).isEmpty();
        return new TaggedSource(starts, displayedSource.toString());
    }

    private static void assertDisplayedSourceHasNoTestMechanics(String relativePath, String displayedSource) {
        assertThat(displayedSource)
                .as("public source in %s", relativePath)
                .doesNotContain(
                        "999-SNAPSHOT",
                        "withPluginClasspath",
                        "mavenLocal()",
                        "/home/",
                        "credentials {",
                        "credentials(",
                        "test-only",
                        "verifyMigrationDocumentation",
                        "verifyMigrationOwnership",
                        "verifyNativeIntegrationRestriction",
                        "verifyStandaloneOwnership",
                        "standaloneParticipatesInAssemble",
                        "assert ",
                        "check(");
    }

    private static void assertIgnoredEntriesExample(String displayedSource) {
        assertThat(displayedSource)
                .contains("quarkus.package.jar.user-configured-ignored-entries", "META-INF/migration-ignored.txt");
    }

    private static void assertDryRunSelects(BuildResult result, String... taskPaths) {
        assertThat(result.getOutput())
                .contains(Stream.of(taskPaths).map(path -> path + " SKIPPED").toArray(String[]::new));
        assertThat(result.getOutput()).doesNotContain("> Task ");
    }

    private static GradleRunner runner(Path projectDirectory, String... arguments) {
        return runner(projectDirectory, List.of(arguments));
    }

    private static GradleRunner runner(Path projectDirectory, List<String> arguments) {
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDirectory.toFile())
                .withArguments(Stream.concat(arguments.stream(), Stream.of(
                        STACKTRACE,
                        "-PquarkusPluginVersion=999-SNAPSHOT",
                        "-PquarkusPlatformVersion=999-SNAPSHOT",
                        "-PquarkusPlatformGroupId=io.quarkus"))
                        .distinct()
                        .toList());
    }

    private static List<FixturePair> fixturePairs() {
        return List.of(
                new FixturePair("direct legacy plugin management", "direct/groovy/settings.gradle",
                        "direct/kotlin/settings.gradle.kts"),
                new FixturePair("direct standalone plugin management", "direct/groovy/settings.gradle.quarkus4",
                        "direct/kotlin/settings.gradle.kts.quarkus4"),
                new FixturePair("direct legacy build", "direct/groovy/build.gradle",
                        "direct/kotlin/build.gradle.kts"),
                new FixturePair("direct standalone build", "direct/groovy/build.gradle.quarkus4",
                        "direct/kotlin/build.gradle.kts.quarkus4"),
                new FixturePair("legacy-first coexistence", "coexistence/groovy/build.gradle",
                        "coexistence/kotlin/build.gradle.kts"),
                new FixturePair("standalone-first rejection", "coexistence/groovy/build.gradle.standalone-first",
                        "coexistence/kotlin/build.gradle.kts.standalone-first"),
                new FixturePair("standalone-only final state", "coexistence/groovy/build.gradle.standalone-only",
                        "coexistence/kotlin/build.gradle.kts.standalone-only"),
                new FixturePair("legacy-first plugin management", "coexistence/groovy/settings.gradle",
                        "coexistence/kotlin/settings.gradle.kts"));
    }

    private record DirectFixture(String dsl, String resourceDirectory, String buildFile, String migratedBuild,
            String settingsFile, String migratedSettings) {
        @Override
        public String toString() {
            return dsl;
        }
    }

    private record CoexistenceFixture(String dsl, String resourceDirectory, String buildFile,
            String standaloneFirstBuild, String standaloneOnlyBuild, boolean verifyNativeIntegrationRestriction) {
        @Override
        public String toString() {
            return dsl;
        }
    }

    private record FixturePair(String name, String groovy, String kotlin) {
    }

    private record TaggedSource(Set<String> tags, String displayedSource) {
    }
}
