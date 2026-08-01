package io.quarkus.extension.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
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

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.gradle.testing.BaseGradleTest;

class ExtensionPluginMigrationDocumentationExamplesTest extends BaseGradleTest {

    private static final String RESOURCE_ROOT = "documentation/extension-plugin-migration";
    private static final Pattern START_TAG = Pattern.compile("^\\s*// tag::([a-z0-9-]+)\\[\\]\\s*$");
    private static final Pattern END_TAG = Pattern.compile("^\\s*// end::([a-z0-9-]+)\\[\\]\\s*$");

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtures")
    void documentedMigrationReplacesTheSinglePluginLayout(Fixture fixture) throws Exception {
        Path projectDirectory = materialize(fixture);

        BuildResult legacyLayout = runner(projectDirectory, ":runtime:jar").buildAndFail();

        // Marker selection is a dependency of validation, so Gradle rejects the graph before executing the task.
        assertThat(legacyLayout.task(":runtime:validateExtension")).isNull();
        assertThat(legacyLayout.getOutput())
                .contains("Could not determine the dependencies of task ':runtime:validateExtension'",
                        "No matching variant of project ':deployment' was found",
                        "io.quarkus.extension.deployment");

        Files.copy(projectDirectory.resolve(fixture.migratedSettings()),
                projectDirectory.resolve(fixture.settings()), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(projectDirectory.resolve(fixture.migratedDeploymentBuild()),
                projectDirectory.resolve(fixture.deploymentBuild()), StandardCopyOption.REPLACE_EXISTING);

        Instant started = Instant.now();
        BuildResult migrated = runner(projectDirectory, ":runtime:jar", ":deployment:test").build();
        BuildResult reused = runner(projectDirectory, ":runtime:jar", ":deployment:test").build();
        System.out.printf("Extension migration fixture %s completed in %s%n",
                fixture.name(), Duration.between(started, Instant.now()));

        assertThat(migrated.task(":runtime:extensionDescriptor").getOutcome()).isEqualTo(SUCCESS);
        assertThat(migrated.task(":runtime:jar").getOutcome()).isEqualTo(SUCCESS);
        assertThat(migrated.task(":deployment:quarkusGenerateTestAppModel").getOutcome()).isEqualTo(SUCCESS);
        assertThat(migrated.task(":deployment:test").getOutcome()).isEqualTo(SUCCESS);

        assertConfigurationCacheReused(reused);
        assertThat(reused.task(":runtime:extensionDescriptor").getOutcome()).isIn(UP_TO_DATE, FROM_CACHE);
        assertThat(reused.task(":runtime:jar").getOutcome()).isIn(UP_TO_DATE, FROM_CACHE);
        assertThat(reused.task(":deployment:quarkusGenerateTestAppModel").getOutcome()).isIn(UP_TO_DATE, FROM_CACHE);
        assertThat(reused.task(":deployment:test").getOutcome()).isIn(UP_TO_DATE, FROM_CACHE);

        Path runtimeJar = projectDirectory.resolve("runtime/build/libs/runtime-1.0.0.jar");
        assertThat(runtimeJar).isRegularFile();
        try (FileSystem jar = ZipUtils.newFileSystem(runtimeJar)) {
            assertThat(jar.getPath(BootstrapConstants.DESCRIPTOR_PATH)).isRegularFile();
            assertThat(jar.getPath(BootstrapConstants.EXTENSION_METADATA_PATH)).isRegularFile();
        }
        assertThat(projectDirectory.resolve("deployment/build/migration-test-model.txt"))
                .isRegularFile()
                .isNotEmptyFile();
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
                new Fixture("groovy", "groovy", "settings.gradle", "settings.gradle.quarkus4",
                        "deployment/build.gradle", "deployment/build.gradle.quarkus4"),
                new Fixture("kotlin", "kotlin", "settings.gradle.kts", "settings.gradle.kts.quarkus4",
                        "deployment/build.gradle.kts", "deployment/build.gradle.kts.quarkus4"));
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
                .doesNotContain("999-SNAPSHOT", "withPluginClasspath", "some/repo");
        return starts;
    }

    private static List<FixturePair> fixturePairs() {
        return List.of(
                new FixturePair("3.x settings", "groovy/settings.gradle", "kotlin/settings.gradle.kts"),
                new FixturePair("Quarkus 4 settings", "groovy/settings.gradle.quarkus4",
                        "kotlin/settings.gradle.kts.quarkus4"),
                new FixturePair("runtime project", "groovy/runtime/build.gradle", "kotlin/runtime/build.gradle.kts"),
                new FixturePair("3.x deployment project", "groovy/deployment/build.gradle",
                        "kotlin/deployment/build.gradle.kts"),
                new FixturePair("Quarkus 4 deployment project", "groovy/deployment/build.gradle.quarkus4",
                        "kotlin/deployment/build.gradle.kts.quarkus4"));
    }

    private static GradleRunner runner(Path projectDirectory, String... tasks) throws IOException {
        List<String> arguments = Stream.concat(Stream.of(tasks), Stream.of(
                CONFIGURATION_CACHE,
                ISOLATED_PROJECTS,
                STACKTRACE,
                "-PquarkusPluginVersion=" + TestUtils.getCurrentQuarkusVersion(),
                "-PquarkusPlatformVersion=" + TestUtils.getCurrentQuarkusVersion()))
                .toList();
        return GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDirectory.toFile())
                .withArguments(arguments);
    }

    private record Fixture(String name, String resourceDirectory, String settings, String migratedSettings,
            String deploymentBuild, String migratedDeploymentBuild) {
        @Override
        public String toString() {
            return name;
        }
    }

    private record FixturePair(String name, String groovy, String kotlin) {
    }
}
