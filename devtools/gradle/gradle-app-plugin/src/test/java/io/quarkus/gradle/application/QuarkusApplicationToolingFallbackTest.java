package io.quarkus.gradle.application;

import static io.quarkus.gradle.testing.BaseGradleTest.canonicalPath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.tooling.BuildActionExecuter;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.OperationType;
import org.gradle.tooling.events.task.TaskStartEvent;
import org.gradle.wrapper.GradleUserHomeLookup;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar;
import io.quarkus.bootstrap.model.gradle.GradleLogicalOutput;
import io.quarkus.bootstrap.model.gradle.GradleProjectComponent;
import io.quarkus.bootstrap.model.gradle.GradleSourceObservation;
import io.quarkus.gradle.testing.BaseGradleTest;
import io.quarkus.maven.dependency.ResolvedDependency;

class QuarkusApplicationToolingFallbackTest extends BaseGradleTest {

    @Test
    void externalJarRemainsExternalAndDoesNotCreateASidecarProject() throws Exception {
        writeExternalJarFixture();

        request("DEVELOPMENT");
        ModelRequest result = request("DEVELOPMENT");
        ResolvedDependency dependency = dependency(result.models().applicationModel(), "external-lib");

        Path externalJar = testProjectDir.resolve("repo/org/acme/external-lib/1.0/external-lib-1.0.jar");
        assertThat(dependency.getResolvedPaths().size()).isOne();
        assertThat(dependency.getResolvedPaths().contains(canonicalPath(externalJar))).isTrue();
        assertThat(dependency.getWorkspaceModule()).isNull();
        assertThat(result.models().sidecar().getProjectComponents())
                .extracting(component -> component.getProjectIdentity().getBuildTreePath())
                .containsExactly(":");
        assertThat(result.taskPaths()).isEmpty();
        assertConfigurationCacheReused(result.output());
    }

    @Test
    void kotlinProducerPublishesSourceAndClassOutputObservationsWithoutExecutingTasks() throws Exception {
        writeKotlinFixture();

        request("DEVELOPMENT", "--offline");
        ModelRequest result = request("DEVELOPMENT", "--offline");
        GradleProjectComponent producer = component(result.models().sidecar(), ":kotlin-producer");

        assertThat(producer.getSourceObservations())
                .extracting(GradleSourceObservation::getPath)
                .anyMatch(path -> Path.of(path).endsWith(Path.of("kotlin-producer", "src", "main", "kotlin")));
        assertThat(producer.getSourceObservations())
                .allSatisfy(source -> {
                    assertThat(source.getRole()).isEqualTo(GradleSourceObservation.Role.UNKNOWN);
                    assertThat(source.hasLogicalOutputAssociation()).isFalse();
                });
        assertThat(producer.getLogicalOutputs())
                .filteredOn(output -> output.getKind() == GradleLogicalOutput.Kind.CLASSES)
                .extracting(GradleLogicalOutput::getPath)
                .anyMatch(path -> Path.of(path)
                        .endsWith(Path.of("kotlin-producer", "build", "classes", "kotlin", "main")));
        assertThat(result.taskPaths()).isEmpty();
        assertConfigurationCacheReused(result.output());
    }

    @Test
    void duplicateArtifactsForTheExactOutputPathRemainUnassociated() throws Exception {
        writeDuplicateOutputFixture();

        request("DEVELOPMENT");
        ModelRequest result = request("DEVELOPMENT");
        GradleProjectComponent producer = component(result.models().sidecar(), ":multi-artifact");
        Path sharedOutput = canonicalPath(testProjectDir.resolve("multi-artifact/build/classes/java/main"));

        assertThat(producer.getLogicalOutputs())
                .filteredOn(output -> canonicalPath(Path.of(output.getPath())).equals(sharedOutput))
                .hasSize(2)
                .allSatisfy(output -> {
                    assertThat(output.getSelectedArtifactIdentity()).isNotBlank();
                    assertThat(output.getModelAssociation().isKnown()).isFalse();
                    assertThat(output.getModelAssociation().isEligibleForOverlayReplacement()).isFalse();
                });
        assertThat(result.taskPaths()).isEmpty();
        assertConfigurationCacheReused(result.output());
    }

    @Test
    void unrelatedTaskDoesNotResolveToolingModelConfigurations() throws Exception {
        writeFile("settings.gradle", "rootProject.name = 'tooling-laziness'\n");
        writeFile("build.gradle", """
                %s
                apply plugin: 'io.quarkus.application'

                dependencies {
                    implementation 'org.missing:must-not-be-resolved:1.0'
                }

                configurations.matching { it.name.startsWith('quarkusApplication') }.configureEach {
                    incoming.beforeResolve {
                        throw new GradleException("tooling-only configuration was resolved: ${name}")
                    }
                }

                tasks.register('ordinary') {
                    doLast {
                        println 'ordinary task executed'
                    }
                }
                """.formatted(buildscriptClasspath()));

        var result = buildResultWithIsolatedProjects("ordinary", BUILD_CACHE);

        assertTaskOutcomes(result, SUCCESS, ":ordinary");
        assertThat(result.getOutput())
                .contains("ordinary task executed")
                .doesNotContain("tooling-only configuration was resolved")
                .doesNotContain("Could not resolve org.missing:must-not-be-resolved:1.0");
    }

    private void writeExternalJarFixture() throws IOException {
        writeFile("settings.gradle", "rootProject.name = 'external-jar-fallback'\n");
        writeFile("build.gradle", """
                %s
                apply plugin: 'io.quarkus.application'

                group = 'org.acme'
                version = '1.0'

                repositories {
                    maven { url = uri('repo') }
                }

                dependencies {
                    implementation 'org.acme:external-lib:1.0'
                }
                """.formatted(buildscriptClasspath()));
        Path artifactDirectory = testProjectDir.resolve("repo/org/acme/external-lib/1.0");
        writeFile(artifactDirectory.resolve("external-lib-1.0.pom"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.acme</groupId>
                    <artifactId>external-lib</artifactId>
                    <version>1.0</version>
                </project>
                """);
        Path jar = artifactDirectory.resolve("external-lib-1.0.jar");
        Files.createDirectories(jar.getParent());
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("external-marker.txt"));
            output.write("external".getBytes(UTF_8));
            output.closeEntry();
        }
    }

    private void writeKotlinFixture() throws IOException {
        String kotlinVersion = System.getProperty("kotlin_version", "2.4.0");
        writeFile("settings.gradle", """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                        mavenCentral()
                    }
                }

                rootProject.name = 'kotlin-source-observation'
                include 'kotlin-producer'
                """);
        writeFile("gradle.properties", "kotlin.stdlib.default.dependency=false\n");
        writeFile("build.gradle", """
                %s
                apply plugin: 'io.quarkus.application'

                group = 'org.acme'
                version = '1.0'

                dependencies {
                    implementation project(':kotlin-producer')
                }
                """.formatted(buildscriptClasspath()));
        writeFile("kotlin-producer/build.gradle", """
                plugins {
                    id 'org.jetbrains.kotlin.jvm' version '%s'
                }

                group = 'org.acme'
                version = '1.0'
                """.formatted(kotlinVersion));
        writeFile("kotlin-producer/src/main/kotlin/org/acme/KotlinValue.kt", """
                package org.acme

                object KotlinValue
                """);
    }

    private void writeDuplicateOutputFixture() throws IOException {
        writeFile("settings.gradle", """
                rootProject.name = 'ambiguous-output-association'
                include 'multi-artifact'
                """);
        writeFile("build.gradle", """
                %s
                apply plugin: 'io.quarkus.application'

                group = 'org.acme'
                version = '1.0'

                dependencies {
                    implementation project(':multi-artifact')
                }
                """.formatted(buildscriptClasspath()));
        writeFile("multi-artifact/build.gradle", """
                plugins {
                    id 'java-library'
                }

                group = 'org.acme'
                version = '1.0'

                def mainClasses = layout.buildDirectory.dir('classes/java/main')
                configurations.named('runtimeElements') {
                    outgoing.variants.named('classes') {
                        artifact(mainClasses) {
                            type = 'java-classes-directory'
                            classifier = 'duplicate'
                        }
                    }
                }
                """);
        writeFile("multi-artifact/src/main/java/org/acme/MultiArtifactValue.java", """
                package org.acme;

                public final class MultiArtifactValue {
                }
                """);
    }

    private ModelRequest request(String mode, String... extraArguments) {
        ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        List<String> taskPaths = new CopyOnWriteArrayList<>();
        try (ProjectConnection connection = connection()) {
            BuildActionExecuter<ToolingPairedModels> request = connection
                    .action(new ToolingPairedModelsAction(mode))
                    .withArguments(arguments(extraArguments))
                    .setStandardOutput(standardOutput)
                    .setStandardError(errorOutput);
            request.addProgressListener(event -> {
                if (event instanceof TaskStartEvent taskStart) {
                    taskPaths.add(taskStart.getDescriptor().getTaskPath());
                }
            }, OperationType.TASK);
            ToolingPairedModels models = request.run();
            return new ModelRequest(models, List.copyOf(taskPaths),
                    standardOutput.toString(UTF_8)
                            + System.lineSeparator()
                            + errorOutput.toString(UTF_8));
        }
    }

    private ProjectConnection connection() {
        GradleConnector connector = GradleConnector.newConnector()
                .forProjectDirectory(testProjectDir.toFile())
                .useGradleUserHomeDir(GradleUserHomeLookup.gradleUserHome());
        String requestedVersion = System.getProperty("quarkus-test-gradle-wrapper-version");
        if (requestedVersion != null) {
            connector.useGradleVersion(requestedVersion);
        }
        return connector.connect();
    }

    private static String[] arguments(String... extraArguments) {
        return Stream.concat(
                Stream.of(
                        CONFIGURATION_CACHE,
                        ISOLATED_PROJECTS,
                        STACKTRACE,
                        "--info",
                        "-Dorg.gradle.console=plain",
                        "-Dquarkus.analytics.disabled=true"),
                Arrays.stream(extraArguments))
                .toArray(String[]::new);
    }

    private static ResolvedDependency dependency(ApplicationModel model, String artifactId) {
        return model.getDependencies().stream()
                .filter(dependency -> dependency.getArtifactId().equals(artifactId))
                .findFirst()
                .orElseThrow();
    }

    private static GradleProjectComponent component(GradleApplicationModelSidecar sidecar, String buildTreePath) {
        return sidecar.getProjectComponents().stream()
                .filter(component -> component.getProjectIdentity().getBuildTreePath().equals(buildTreePath))
                .findFirst()
                .orElseThrow();
    }

    private static String buildscriptClasspath() {
        return """
                buildscript {
                    dependencies {
                        classpath files(%s)
                    }
                }
                """.formatted(TestKitPluginClasspath.implementationClasspath().stream()
                .map(File::getAbsolutePath)
                .sorted(Comparator.naturalOrder())
                .map(QuarkusApplicationToolingFallbackTest::singleQuotedGroovyString)
                .collect(Collectors.joining(", ")));
    }

    private static String singleQuotedGroovyString(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private record ModelRequest(ToolingPairedModels models, List<String> taskPaths, String output) {
    }
}
