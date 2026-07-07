package io.quarkus.gradle.application.internal.plugin;

import static io.quarkus.gradle.testing.BaseGradleTest.BUILD_CACHE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gradle.model.pom.PomClosureResult;
import io.quarkus.gradle.model.pom.PomClosureResultCodec;
import io.quarkus.gradle.testing.BaseGradleTest;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

class QuarkusApplicationOfflinePreparationFunctionalTest extends BaseGradleTest {

    private static final String GRADLE_38920_FAILURE = "Could not find org.junit.platform:junit-platform-launcher:.";

    @Test
    void unrelatedTaskDoesNotResolvePreparationConfigurations() throws IOException {
        String version = uniqueVersion();
        writeApplication(URI.create("http://127.0.0.1:1/unavailable/"), version, """
                dependencies {
                    implementation 'org.acme:unavailable-library:%s'
                }

                quarkusApplication {
                    builds {
                        fastJar('selected') {
                            prepareForOffline = true
                        }
                    }
                }
                """.formatted(version));

        BuildResult result = buildResultWithIsolatedProjects("help", "--offline", BUILD_CACHE);

        assertTaskOutcomes(result, SUCCESS, ":help");
        assertThat(result.task(":quarkusApplicationPrepareOffline")).isNull();
        assertThat(result.task(":quarkusApplicationSelectedPrepareOffline")).isNull();
        assertThat(result.task(":quarkusApplicationModelPomClosure")).isNull();
    }

    @Test
    void kotlinDslCanSelectNamedBuildForOfflinePreparation() throws IOException {
        writeFile("settings.gradle.kts", "rootProject.name = \"offline-preparation-kotlin\"\n");
        writeFile("build.gradle.kts", """
                plugins {
                    id("io.quarkus.application")
                }

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                quarkusApplication {
                    builds {
                        fastJar("kotlin") {
                            prepareForOffline.set(true)
                        }
                    }
                }

                tasks.register("quarkusGoOffline")
                """);

        BuildResult result = buildResultWithIsolatedProjects(
                "quarkusGoOffline", "quarkusApplicationPrepareOffline", "--dry-run", BUILD_CACHE);

        assertThat(result.getOutput())
                .contains(":quarkusGoOffline SKIPPED")
                .contains(":quarkusApplicationKotlinPrepareOffline SKIPPED")
                .contains(":quarkusApplicationPrepareOffline SKIPPED");
        assertNoBuildOperationTasks(result);
    }

    @Test
    void preparesDefaultAndSelectedBuildClosuresWithoutExecutingBuildOperations() throws IOException {
        String version = uniqueVersion();
        try (SyntheticMavenRepository repository = new SyntheticMavenRepository(testProjectDir.resolve("repo"))) {
            repository.writeSimpleArtifact("org.acme", "annotation-processor", version);
            writeApplication(repository.fileUri(), version, """
                    quarkusApplication {
                        builds {
                            fastJar('selected') {
                                prepareForOffline = true
                            }
                            nativeExecutable('notSelected')
                        }
                    }
                    """);

            BuildResult first = buildResultWithIsolatedProjects("quarkusApplicationPrepareOffline", BUILD_CACHE);

            assertTaskOutcomes(first, SUCCESS,
                    ":quarkusApplicationModelPomClosure",
                    ":quarkusApplicationSelectedPrepareOffline",
                    ":quarkusApplicationPrepareOffline");
            assertThat(first.task(":quarkusApplicationNotSelectedPrepareOffline")).isNull();
            assertNoBuildOperationTasks(first);
            assertThat(first.getOutput())
                    .contains("Prepared ")
                    .contains("named build 'selected'")
                    .contains("normal, development, test, code generation, dev launcher");

            BuildResult second = buildResultWithIsolatedProjects("quarkusApplicationPrepareOffline", BUILD_CACHE);

            assertConfigurationCacheReused(second);
            assertTaskOutcomes(second, SUCCESS,
                    ":quarkusApplicationSelectedPrepareOffline",
                    ":quarkusApplicationPrepareOffline");
            assertThat(second.task(":quarkusApplicationModelPomClosure")).isNotNull();
            assertNoBuildOperationTasks(second);
        }
    }

    @Test
    void resolvesSelectedParentAndImportedBomPomClosureWithConfigurationCacheAndOfflineReuse() throws IOException {
        String version = uniqueVersion();
        String groupId = "org.acme.pomclosure";
        try (SyntheticMavenRepository repository = new SyntheticMavenRepository(testProjectDir.resolve("repo"))) {
            repository.writeRecursivePomClosureArtifacts(groupId, version);
            writeApplication(repository.startServer(), version, """
                    dependencies {
                        implementation platform('%s:gradle-platform:%s')
                        implementation '%s:application:%s'
                    }
                    """.formatted(groupId, version, groupId, version));

            BuildResult first = buildResultWithIsolatedProjects("quarkusApplicationPrepareOffline", BUILD_CACHE);

            assertTaskOutcomes(first, SUCCESS,
                    ":quarkusApplicationModelPomClosure",
                    ":quarkusApplicationPrepareOffline");
            assertCompletePomClosure(groupId, version);

            BuildResult reused = buildResultWithIsolatedProjects("quarkusApplicationPrepareOffline", BUILD_CACHE);

            assertConfigurationCacheReused(reused);
            assertCompletePomClosure(groupId, version);
        }

        BuildResult offline = buildResultWithIsolatedProjects(
                "quarkusApplicationModel", "--offline", BUILD_CACHE);

        assertThat(offline.task(":quarkusApplicationModelPomClosure")).isNotNull();
        assertTaskOutcomes(offline, SUCCESS, ":quarkusApplicationModel");
        assertCompletePomClosure(groupId, version);
        assertEnrichedDeclaredDependencies(groupId);
    }

    @Test
    void tracksOnlyPomReferencedSystemPropertiesForProfileActivatedImports() throws IOException {
        String version = uniqueVersion();
        String groupId = "org.acme.profileclosure";
        try (SyntheticMavenRepository repository = new SyntheticMavenRepository(testProjectDir.resolve("repo"))) {
            repository.writeProfileActivatedPomArtifacts(groupId, version);
            writeApplication(repository.startServer(), version, """
                    dependencies {
                        implementation '%s:application:%s'
                    }
                    """.formatted(groupId, version));

            BuildResult first = buildResultWithIsolatedProjects(
                    "quarkusApplicationModelPomClosure",
                    "-Dclosure.bom=one",
                    "-Dclosure.unrelated=first",
                    BUILD_CACHE);

            assertTaskOutcomes(first, SUCCESS, ":quarkusApplicationModelPomClosure");
            assertProfilePomClosure(groupId, version, "bom-one", "bom-two");

            BuildResult reused = buildResultWithIsolatedProjects(
                    "quarkusApplicationModelPomClosure",
                    "-Dclosure.bom=one",
                    "-Dclosure.unrelated=first",
                    BUILD_CACHE);

            assertConfigurationCacheReused(reused);
            assertProfilePomClosure(groupId, version, "bom-one", "bom-two");

            BuildResult unrelatedChanged = buildResultWithIsolatedProjects(
                    "quarkusApplicationModelPomClosure",
                    "-Dclosure.bom=one",
                    "-Dclosure.unrelated=second",
                    BUILD_CACHE);

            assertConfigurationCacheReused(unrelatedChanged);
            assertProfilePomClosure(groupId, version, "bom-one", "bom-two");

            BuildResult changed = buildResultWithIsolatedProjects(
                    "quarkusApplicationModelPomClosure",
                    "-Dclosure.bom=two",
                    "-Dclosure.unrelated=second",
                    BUILD_CACHE);

            assertThat(changed.task(":quarkusApplicationModelPomClosure")).isNotNull();
            assertProfilePomClosure(groupId, version, "bom-two", "bom-one");
        }
    }

    @Test
    void recordsKnownMissingPomFromArtifactOnlyRepository() throws IOException {
        String version = uniqueVersion();
        String groupId = "org.acme.missingpom";
        try (SyntheticMavenRepository repository = new SyntheticMavenRepository(testProjectDir.resolve("repo"))) {
            repository.writeJarArtifact(groupId, "artifact-only-library", version);
            repository.writeJarArtifact("org.acme", "annotation-processor", version);
            writeApplication(repository.startServer(), version, """
                    metadataSources {
                        artifact()
                    }
                    """, """
                    dependencies {
                        implementation '%s:artifact-only-library:%s'
                    }
                    """.formatted(groupId, version));

            BuildResult preparation = buildResultWithIsolatedProjects(
                    "quarkusApplicationPrepareOffline", BUILD_CACHE);

            assertTaskOutcomes(preparation, SUCCESS,
                    ":quarkusApplicationModelPomClosure",
                    ":quarkusApplicationPrepareOffline");
            assertThat(readPomClosure().missingPoms())
                    .extracting(Object::toString)
                    .contains(groupId + ":artifact-only-library:" + version);
            assertThat(preparation.getOutput())
                    .contains("Offline preparation is incomplete:")
                    .contains("unresolved POM entries");

            BuildResult reused = buildResultWithIsolatedProjects(
                    "quarkusApplicationPrepareOffline", BUILD_CACHE);

            assertConfigurationCacheReused(reused);
            assertThat(readPomClosure().missingPoms())
                    .extracting(Object::toString)
                    .contains(groupId + ":artifact-only-library:" + version);
        }
    }

    @Test
    void recordsMissingImportedBomAfterResolvingSelectedPom() throws IOException {
        String version = uniqueVersion();
        String groupId = "org.acme.missingimport";
        try (SyntheticMavenRepository repository = new SyntheticMavenRepository(testProjectDir.resolve("repo"))) {
            repository.writeSimpleArtifact("org.acme", "annotation-processor", version);
            repository.writeArtifact(groupId, "application", version, """
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>%s</groupId>
                      <artifactId>application</artifactId>
                      <version>%s</version>
                      <dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>%s</groupId>
                            <artifactId>missing-bom</artifactId>
                            <version>%s</version>
                            <type>pom</type>
                            <scope>import</scope>
                          </dependency>
                        </dependencies>
                      </dependencyManagement>
                    </project>
                    """.formatted(groupId, version, groupId, version));
            writeApplication(repository.startServer(), version, """
                    metadataSources {
                        artifact()
                    }
                    """, """
                    dependencies {
                        implementation '%s:application:%s'
                    }
                    """.formatted(groupId, version));

            BuildResult result = buildResultWithIsolatedProjects(
                    "quarkusApplicationModelPomClosure", BUILD_CACHE);

            assertTaskOutcomes(result, SUCCESS, ":quarkusApplicationModelPomClosure");
            PomClosureResult closure = readPomClosure();
            assertThat(closure.resolvedPoms().keySet())
                    .extracting(Object::toString)
                    .contains(groupId + ":application:" + version);
            assertThat(closure.missingPoms())
                    .extracting(Object::toString)
                    .contains(groupId + ":missing-bom:" + version);
        }
    }

    @Test
    void preparedArtifactsAndPomClosureSupportOfflineModelsAndCompilation() throws IOException {
        String version = uniqueVersion();
        BuildResult preparation;
        try (SyntheticMavenRepository repository = new SyntheticMavenRepository(testProjectDir.resolve("repo"))) {
            repository.writeSimpleArtifact("org.acme", "offline-library", version);
            repository.writeSimpleArtifact("org.acme", "runtime-only-library", version);
            repository.writeSimpleArtifact("org.acme", "compile-only-library", version);
            repository.writeSimpleArtifact("org.acme", "development-only-library", version);
            repository.writeSimpleArtifact("org.acme", "test-library", version);
            repository.writeSimpleArtifact("org.acme", "test-compile-only-library", version);
            repository.writeSimpleArtifact("org.acme", "annotation-processor", version);
            repository.writeSimpleArtifact("org.acme", "test-annotation-processor", version);
            writeApplication(repository.startServer(), version, """
                    dependencies {
                        implementation 'org.acme:offline-library:%s'
                        runtimeOnly 'org.acme:runtime-only-library:%s'
                        compileOnly 'org.acme:compile-only-library:%s'
                        quarkusDev 'org.acme:development-only-library:%s'
                        testImplementation 'org.acme:test-library:%s'
                        testCompileOnly 'org.acme:test-compile-only-library:%s'
                        testAnnotationProcessor 'org.acme:test-annotation-processor:%s'
                    }
                    """.formatted(version, version, version, version, version, version, version));
            preparation = buildResultWithIsolatedProjects("quarkusApplicationPrepareOffline", BUILD_CACHE);
        }

        assertTaskOutcomes(preparation, SUCCESS,
                ":quarkusApplicationModelPomClosure",
                ":quarkusApplicationPrepareOffline");
        assertThat(preparation.task(":quarkusApplicationSelectedPrepareOffline")).isNull();
        Path closure = testProjectDir.resolve(
                "build/quarkus/application-model/pom-closure/quarkusApplicationModel.properties");
        assertThat(closure)
                .as("POM closure after preparation output:%n%s", preparation.getOutput())
                .content(StandardCharsets.UTF_8)
                .contains("offline-library");
        assertThat(preparation.getOutput())
                .doesNotContain("Offline preparation is incomplete:");
        assertThat(readPomClosure().missingPoms())
                .extracting(Object::toString)
                .noneMatch(gav -> gav.startsWith("org.acme:"));

        BuildResult offline = buildResultWithIsolatedProjects(
                "quarkusApplicationDevModel",
                "quarkusApplicationDevCodegenModel",
                "quarkusApplicationContinuousTestModel",
                "quarkusApplicationCodegenModel",
                "quarkusApplicationTestCodegenModel",
                "quarkusApplicationModel",
                "compileJava",
                "compileTestJava",
                "--offline",
                BUILD_CACHE);

        assertTaskOutcomes(offline, SUCCESS,
                ":quarkusApplicationDevModel",
                ":quarkusApplicationDevCodegenModel",
                ":quarkusApplicationContinuousTestModel",
                ":quarkusApplicationCodegenModel",
                ":quarkusApplicationTestCodegenModel",
                ":quarkusApplicationModel");
        assertThat(offline.task(":compileJava").getOutcome()).isIn(SUCCESS, FROM_CACHE);
        assertThat(offline.task(":compileTestJava").getOutcome()).isIn(SUCCESS, FROM_CACHE);
        assertThat(offline.task(":quarkusApplicationModelPomClosure")).isNotNull();
        assertThat(closure)
                .content(StandardCharsets.UTF_8)
                .contains("offline-library");
    }

    @ParameterizedTest(name = "mavenLocalFirst={0}")
    @ValueSource(booleans = { true, false })
    void selectedAotBuildPreparesCustomTrainingSuiteWithoutExecutingIt(boolean mavenLocalFirst) throws IOException {
        String version = uniqueVersion();
        BuildResult preparation;
        try (SyntheticMavenRepository repository = new SyntheticMavenRepository(testProjectDir.resolve("repo"))) {
            repository.writeSimpleArtifact("org.acme", "annotation-processor", version);
            repository.writeSimpleArtifact("org.acme", "training-library", version);
            repository.writeSimpleArtifact("org.acme", "training-annotation-processor", version);
            repository.writeSimpleArtifact("org.acme", "unselected-training-library", version);
            writeApplication(repository.startServer(), version, "", mavenLocalFirst, """
                    import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType
                    import io.quarkus.gradle.application.model.QuarkusApplicationStartupArchiveTrainingExecutionTarget
                    import org.gradle.api.plugins.jvm.JvmTestSuite

                    quarkusApplication {
                        builds {
                            aotJar('aot', QuarkusApplicationJvmStartupArchiveType.AOT) {
                                prepareForOffline = true
                            }
                            aotJar('notSelected', QuarkusApplicationJvmStartupArchiveType.AOT)
                        }
                    }

                    testing {
                        suites {
                            training(JvmTestSuite) {
                                dependencies {
                                    implementation 'org.acme:training-library:%s'
                                    annotationProcessor 'org.acme:training-annotation-processor:%s'
                                }
                                startupArchiveTraining {
                                    executionTarget.set(
                                        QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM)
                                }
                                forQuarkusIntegrationTests 'aot'
                            }
                            unselectedTraining(JvmTestSuite) {
                                dependencies {
                                    implementation 'org.acme:unselected-training-library:%s'
                                }
                                startupArchiveTraining {
                                    executionTarget.set(
                                        QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM)
                                }
                                forQuarkusIntegrationTests 'notSelected'
                            }
                        }
                    }
                    """.formatted(version, version, version));
            writeFile("src/training/java/org/acme/Training.java", """
                    package org.acme;

                    public final class Training {
                    }
                    """);
            writeFile("src/unselectedTraining/java/org/acme/UnselectedTraining.java", """
                    package org.acme;

                    public final class UnselectedTraining {
                    }
                    """);
            try {
                preparation = buildResultWithIsolatedProjects("quarkusApplicationPrepareOffline", BUILD_CACHE);
            } catch (UnexpectedBuildFailure failure) {
                if (mavenLocalFirst) {
                    assumeThat(failure.getMessage())
                            .as("Gradle issue https://github.com/gradle/gradle/issues/38920 prevents custom "
                                    + "JvmTestSuite resolution with mavenLocal() first")
                            .doesNotContain(GRADLE_38920_FAILURE);
                }
                throw failure;
            }
        }

        assertTaskOutcomes(preparation, SUCCESS,
                ":quarkusApplicationModelPomClosure",
                ":quarkusApplicationAotPrepareOffline",
                ":quarkusApplicationPrepareOffline");
        assertThat(preparation.task(":quarkusApplicationNotSelectedPrepareOffline")).isNull();
        assertThat(preparation.task(":training")).isNull();
        assertThat(preparation.task(":quarkusAotTrainingStartupArchiveTrainingMetadata")).isNull();
        assertThat(preparation.task(":quarkusAotStartupArchiveValidation")).isNull();
        assertNoBuildOperationTasks(preparation);

        BuildResult offline = buildResultWithIsolatedProjects("compileTrainingJava", "--offline", BUILD_CACHE);

        assertThat(offline.task(":compileTrainingJava").getOutcome()).isIn(SUCCESS, FROM_CACHE);
        assertThat(offline.task(":training")).isNull();
        assertNoBuildOperationTasks(offline);

        BuildResult unselected = prepareBuildWithIsolatedProjects(
                "compileUnselectedTrainingJava", "--offline", BUILD_CACHE).buildAndFail();

        assertThat(unselected.getOutput())
                .contains("unselected-training-library")
                .contains("No cached version");
    }

    private void writeApplication(URI repository, String version, String additionalBuildScript) throws IOException {
        writeApplication(repository, version, "", true, additionalBuildScript);
    }

    private void writeApplication(URI repository, String version, String repositoryMetadataSources,
            String additionalBuildScript) throws IOException {
        writeApplication(repository, version, repositoryMetadataSources, true, additionalBuildScript);
    }

    private void writeApplication(URI repository, String version, String repositoryMetadataSources, boolean mavenLocalFirst,
            String additionalBuildScript) throws IOException {
        String standardRepositories = mavenLocalFirst
                ? "mavenLocal()\n                    mavenCentral()"
                : "mavenCentral()\n                    mavenLocal()";
        writeFile("settings.gradle", "rootProject.name = 'offline-preparation'\n");
        writeFile("gradle.properties", "version = 999-SNAPSHOT\n");
        writeFile("src/main/java/org/acme/Application.java", """
                package org.acme;

                public final class Application {
                }
                """);
        writeFile("src/test/java/org/acme/ApplicationTest.java", """
                package org.acme;

                public final class ApplicationTest {
                }
                """);
        writeFile("build.gradle", """
                plugins {
                    id 'io.quarkus.application'
                }

                repositories {
                    maven {
                        url = uri('%s')
                        %s
                    }
                    %s
                }

                dependencies {
                    annotationProcessor 'org.acme:annotation-processor:%s'
                }

                %s
                """.formatted(repository, repositoryMetadataSources, standardRepositories, version, additionalBuildScript));
    }

    private static void assertNoBuildOperationTasks(BuildResult result) {
        assertThat(result.getTasks())
                .extracting(task -> task.getPath())
                .noneMatch(path -> path.matches(
                        ".*(Build|ImageBuild|ImagePush|DeployTo.*|Run|StartupOptimizedImage.*|"
                                + "StartupArchiveTrainingMetadata|StartupArchiveValidation|NativeTest)$"));
    }

    private static String uniqueVersion() {
        return "1.0-" + UUID.randomUUID();
    }

    private void assertCompletePomClosure(String groupId, String version) throws IOException {
        PomClosureResult closure = readPomClosure();
        List<String> expected = List.of(
                "application",
                "parent",
                "grandparent",
                "bom",
                "nested-bom",
                "bom-parent",
                "gradle-platform",
                "library",
                "runtime-library").stream()
                .map(artifactId -> groupId + ":" + artifactId + ":" + version)
                .toList();
        assertThat(closure.resolvedPoms().keySet())
                .extracting(Object::toString)
                .containsAll(expected);
        assertThat(closure.missingPoms())
                .extracting(Object::toString)
                .noneMatch(gav -> gav.startsWith(groupId + ":"));
    }

    private void assertEnrichedDeclaredDependencies(String groupId) throws IOException {
        ApplicationModel model = io.quarkus.gradle.tooling.ToolingUtils.deserializeAppModel(testProjectDir.resolve(
                "build/quarkus/application-model/quarkus-application-model.dat"));
        ResolvedDependency application = model.getDependencies().stream()
                .filter(dependency -> groupId.equals(dependency.getGroupId()))
                .filter(dependency -> "application".equals(dependency.getArtifactId()))
                .findFirst()
                .orElseThrow();
        assertThat(application.getDirectDependencies())
                .filteredOn(dependency -> groupId.equals(dependency.getGroupId()))
                .satisfiesExactlyInAnyOrder(
                        dependency -> {
                            assertThat(dependency.getArtifactId()).isEqualTo("library");
                            assertThat(dependency.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();
                        },
                        dependency -> {
                            assertThat(dependency.getArtifactId()).isEqualTo("provided-library");
                            assertThat(dependency.getScope()).isEqualTo("provided");
                            assertThat(dependency.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isTrue();
                        },
                        dependency -> {
                            assertThat(dependency.getArtifactId()).isEqualTo("optional-library");
                            assertThat(dependency.isOptional()).isTrue();
                            assertThat(dependency.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isTrue();
                        },
                        dependency -> {
                            assertThat(dependency.getArtifactId()).isEqualTo("runtime-library");
                            assertThat(dependency.getScope()).isEqualTo("runtime");
                            assertThat(dependency.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();
                        });
    }

    private void assertProfilePomClosure(String groupId, String version, String includedArtifact,
            String excludedArtifact) throws IOException {
        PomClosureResult closure = readPomClosure();
        assertThat(closure.resolvedPoms().keySet())
                .extracting(Object::toString)
                .contains(groupId + ":application:" + version, groupId + ":" + includedArtifact + ":" + version)
                .doesNotContain(groupId + ":" + excludedArtifact + ":" + version);
    }

    private PomClosureResult readPomClosure() throws IOException {
        return PomClosureResultCodec.read(testProjectDir.resolve(
                "build/quarkus/application-model/pom-closure/quarkusApplicationModel.properties"));
    }
}
