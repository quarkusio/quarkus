package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

class QuarkusApplicationModelResolutionTest extends QuarkusApplicationModelResolutionTestSupport {

    @Test
    void testApplicationModelMatchesLegacyTestApplicationModelForTestBootstrapFacts() throws IOException {
        Path repository = testProjectDir.resolve("repo");
        writeMavenArtifact(repository, "org.acme", "sample-extension", "1.0",
                "deployment-artifact=org.acme\\:sample-extension-deployment\\:1.0\n");
        writeMavenArtifact(repository, "org.acme", "sample-extension-deployment", "1.0", null);
        writeMavenArtifact(repository, "org.acme", "main-compile-only", "1.0", null);
        writeMavenArtifact(repository, "org.acme", "test-helper", "1.0", null);
        writeMavenArtifact(repository, "org.acme", "test-runtime", "1.0", null);
        writeMavenArtifact(repository, "org.acme", "test-compile-only", "1.0", null);
        writeFile(testProjectDir.resolve("settings.gradle"), "");
        writeFile(testProjectDir.resolve("src/main/java/org/acme/App.java"), """
                package org.acme;

                public final class App {
                }
                """);
        writeFile(testProjectDir.resolve("src/test/java/org/acme/AppTest.java"), """
                package org.acme;

                public final class AppTest {
                }
                """);
        writeFile(testProjectDir.resolve("build.gradle"), testModelBuildFile("io.quarkus", repository));

        buildResultWithIsolatedProjects("quarkusGenerateTestAppModel");
        Path legacyModelPath = testProjectDir.resolve("build/quarkus/application-model/quarkus-app-test-model.dat");
        byte[] legacyModelBytes = Files.readAllBytes(legacyModelPath);

        writeFile(testProjectDir.resolve("build.gradle"), testModelBuildFile("io.quarkus.application", repository));

        BuildResult result = buildResultWithIsolatedProjects("quarkusApplicationModel", "quarkusApplicationTestModel");
        assertTaskOutcomes(result, SUCCESS, ":quarkusApplicationModel", ":quarkusApplicationTestModel");
        Path newNormalModelPath = testProjectDir.resolve("build/quarkus/application-model/quarkus-application-model.dat");
        Path newTestModelPath = testProjectDir
                .resolve("build/quarkus/application-model/quarkus-application-test-model.dat");

        ApplicationModel legacyModel = io.quarkus.gradle.tooling.ToolingUtils
                .deserializeAppModel(Files.write(testProjectDir.resolve("legacy-test-model.dat"), legacyModelBytes));
        ApplicationModel newNormalModel = io.quarkus.gradle.tooling.ToolingUtils.deserializeAppModel(newNormalModelPath);
        ApplicationModel newTestModel = io.quarkus.gradle.tooling.ToolingUtils.deserializeAppModel(newTestModelPath);
        assertThat(newTestModel.getAppArtifact().getKey()).isEqualTo(legacyModel.getAppArtifact().getKey());
        assertThat(newTestModel.getAppArtifact().getWorkspaceModule().getId())
                .isEqualTo(legacyModel.getAppArtifact().getWorkspaceModule().getId());
        assertThat(newTestModel.getReloadableWorkspaceDependencies())
                .isEqualTo(legacyModel.getReloadableWorkspaceDependencies());
        assertThat(dependencyCoordinates(newTestModel.getDependencies()))
                .isEqualTo(dependencyCoordinates(legacyModel.getDependencies()));
        assertThat(dependencyCoordinates(newTestModel.getRuntimeDependencies()))
                .isEqualTo(dependencyCoordinates(legacyModel.getRuntimeDependencies()));
        assertThat(dependencyCoordinates(newTestModel.getDependencies(DependencyFlags.DEPLOYMENT_CP)))
                .isEqualTo(dependencyCoordinates(legacyModel.getDependencies(DependencyFlags.DEPLOYMENT_CP)));
        Set<String> legacyCompileOnlyDependencies = dependencyCoordinates(
                legacyModel.getDependencies(DependencyFlags.COMPILE_ONLY));
        Set<String> newCompileOnlyDependencies = dependencyCoordinates(
                newTestModel.getDependencies(DependencyFlags.COMPILE_ONLY));
        assertThat(legacyCompileOnlyDependencies).contains("org.acme:main-compile-only:1.0");
        assertThat(newCompileOnlyDependencies)
                .contains("org.acme:main-compile-only:1.0", "org.acme:test-compile-only:1.0");
        assertThat(without(newCompileOnlyDependencies, "org.acme:test-compile-only:1.0"))
                .isEqualTo(legacyCompileOnlyDependencies);
        assertThat(directDependencyCoordinates(newTestModel))
                .isEqualTo(directDependencyCoordinates(legacyModel));
        assertThat(dependencyCoordinates(newTestModel.getDependencies()))
                .contains(
                        "org.acme:sample-extension:1.0",
                        "org.acme:sample-extension-deployment:1.0",
                        "org.acme:test-helper:1.0",
                        "org.acme:test-runtime:1.0");
        assertThat(dependencyCoordinates(newNormalModel.getDependencies()))
                .contains("org.acme:sample-extension:1.0", "org.acme:sample-extension-deployment:1.0")
                .doesNotContain("org.acme:test-helper:1.0", "org.acme:test-runtime:1.0",
                        "org.acme:test-compile-only:1.0");
    }

    @Test
    void developmentOnlyDependenciesAppearOnlyInDevelopmentModel() throws IOException {
        Path repository = testProjectDir.resolve("repo");
        writeMavenArtifact(repository, "org.acme", "development-helper", "1.0",
                "deployment-artifact=org.acme\\:development-helper-deployment\\:1.0\n");
        writeMavenArtifact(repository, "org.acme", "development-helper-deployment", "1.0", null);
        writeFile(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'development-dependencies'\n");
        writeFile(testProjectDir.resolve("build.gradle"), """
                plugins {
                    id 'io.quarkus.application'
                }

                repositories {
                    maven {
                        url = uri('repo')
                    }
                }

                dependencies {
                    quarkusDev 'org.acme:development-helper:1.0'
                }

                def developmentDependencies = configurations.named('quarkusDev').get()
                assert developmentDependencies.canBeDeclared
                assert !developmentDependencies.canBeResolved
                assert !developmentDependencies.canBeConsumed
                """);

        BuildResult first = buildResultWithIsolatedProjects(
                "quarkusApplicationShowModel",
                "quarkusApplicationShowDevModel",
                "quarkusApplicationShowTestModel",
                "quarkusApplicationDevCodegenModel",
                "quarkusApplicationContinuousTestModel");

        assertTaskOutcomes(first, SUCCESS,
                ":quarkusApplicationModel",
                ":quarkusApplicationDevModel",
                ":quarkusApplicationTestModel",
                ":quarkusApplicationDevCodegenModel",
                ":quarkusApplicationContinuousTestModel",
                ":quarkusApplicationShowModel",
                ":quarkusApplicationShowDevModel",
                ":quarkusApplicationShowTestModel");
        assertThat(testProjectDir.resolve(
                "build/reports/quarkus/application-model/quarkus-application-dev-model.txt"))
                .content()
                .contains(
                        "org.acme:development-helper::jar:1.0",
                        "org.acme:development-helper-deployment::jar:1.0");
        assertThat(testProjectDir.resolve(
                "build/reports/quarkus/application-model/quarkus-application-model.txt"))
                .content()
                .doesNotContain("org.acme:development-helper");
        assertThat(testProjectDir.resolve(
                "build/reports/quarkus/application-model/quarkus-application-test-model.txt"))
                .content()
                .doesNotContain("org.acme:development-helper");
        ApplicationModel devCodegenModel = io.quarkus.gradle.tooling.ToolingUtils.deserializeAppModel(testProjectDir.resolve(
                "build/quarkus/application-model/quarkus-application-dev-codegen-model.dat"));
        ApplicationModel continuousTestModel = io.quarkus.gradle.tooling.ToolingUtils
                .deserializeAppModel(testProjectDir.resolve(
                        "build/quarkus/application-model/quarkus-application-continuous-test-model.dat"));
        assertThat(dependencyCoordinates(devCodegenModel.getDependencies()))
                .contains(
                        "org.acme:development-helper:1.0",
                        "org.acme:development-helper-deployment:1.0");
        assertThat(dependencyCoordinates(continuousTestModel.getDependencies()))
                .contains(
                        "org.acme:development-helper:1.0",
                        "org.acme:development-helper-deployment:1.0");

        BuildResult second = buildResultWithIsolatedProjects(
                "quarkusApplicationShowModel",
                "quarkusApplicationShowDevModel",
                "quarkusApplicationShowTestModel",
                "quarkusApplicationDevCodegenModel",
                "quarkusApplicationContinuousTestModel");
        assertConfigurationCacheReused(second);
        assertTaskOutcomes(second, SUCCESS,
                ":quarkusApplicationShowModel",
                ":quarkusApplicationShowDevModel",
                ":quarkusApplicationShowTestModel");
        assertTaskOutcomes(second, UP_TO_DATE,
                ":quarkusApplicationDevCodegenModel",
                ":quarkusApplicationContinuousTestModel");
    }

    @Test
    void realLegacyDevelopmentConfigurationRemainsOwnedAndFeedsStandaloneDevelopmentModel() throws IOException {
        Path repository = testProjectDir.resolve("repo");
        writeMavenArtifact(repository, "org.acme", "legacy-development-helper", "1.0",
                "deployment-artifact=org.acme\\:legacy-development-helper-deployment\\:1.0\n");
        writeMavenArtifact(repository, "org.acme", "legacy-development-helper-deployment", "1.0", null);
        writeFile(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'legacy-development-dependencies'\n");
        writeFile(testProjectDir.resolve("build.gradle"), """
                plugins {
                    id 'io.quarkus'
                }

                def legacyDevelopmentDependencies = configurations.named('quarkusDev').get()
                def legacyDescription = legacyDevelopmentDependencies.description
                def legacyCanBeDeclared = legacyDevelopmentDependencies.canBeDeclared
                def legacyCanBeResolved = legacyDevelopmentDependencies.canBeResolved
                def legacyCanBeConsumed = legacyDevelopmentDependencies.canBeConsumed
                def legacyParents = legacyDevelopmentDependencies.extendsFrom as Set
                def legacyAttributes = legacyDevelopmentDependencies.attributes.keySet().collectEntries {
                    [(it): legacyDevelopmentDependencies.attributes.getAttribute(it)]
                }

                apply plugin: io.quarkus.gradle.application.QuarkusApplicationPlugin

                assert legacyDevelopmentDependencies.description == legacyDescription
                assert legacyDevelopmentDependencies.canBeDeclared == legacyCanBeDeclared
                assert legacyDevelopmentDependencies.canBeResolved == legacyCanBeResolved
                assert legacyDevelopmentDependencies.canBeConsumed == legacyCanBeConsumed
                assert legacyDevelopmentDependencies.extendsFrom == legacyParents
                assert legacyDevelopmentDependencies.attributes.keySet().collectEntries {
                    [(it): legacyDevelopmentDependencies.attributes.getAttribute(it)]
                } == legacyAttributes
                assert configurations.quarkusApplicationDevBaseRuntimeClasspathConfiguration.extendsFrom
                        .contains(legacyDevelopmentDependencies)

                repositories {
                    maven {
                        url = uri('repo')
                    }
                }

                dependencies {
                    quarkusDev 'org.acme:legacy-development-helper:1.0'
                }
                """);

        BuildResult result = buildResultWithIsolatedProjects("quarkusApplicationDevModel");

        assertTaskOutcomes(result, SUCCESS, ":quarkusApplicationDevModel");
        ApplicationModel developmentModel = io.quarkus.gradle.tooling.ToolingUtils.deserializeAppModel(
                testProjectDir.resolve("build/quarkus/application-model/quarkus-application-dev-model.dat"));
        assertThat(dependencyCoordinates(developmentModel.getDependencies()))
                .contains(
                        "org.acme:legacy-development-helper:1.0",
                        "org.acme:legacy-development-helper-deployment:1.0");
    }

    private static Set<String> dependencyCoordinates(Iterable<? extends ResolvedDependency> dependencies) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(dependencies.iterator(), Spliterator.ORDERED), false)
                .map(ArtifactCoords::toCompactCoords)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<String> without(Set<String> values, String value) {
        Set<String> filtered = new TreeSet<>(values);
        filtered.remove(value);
        return filtered;
    }

    private static Map<String, Set<String>> directDependencyCoordinates(ApplicationModel model) {
        return model.getDependencies().stream()
                .collect(Collectors.toMap(
                        ArtifactCoords::toCompactCoords,
                        dependency -> dependency.getDirectDependencies().stream()
                                .map(ArtifactCoords::toCompactCoords)
                                .collect(Collectors.toCollection(TreeSet::new)),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private static String testModelBuildFile(String pluginId, Path repository) {
        return """
                plugins {
                    id 'java'
                    id '%s'
                }

                group = 'org.acme'
                version = '1.0'

                repositories {
                    maven {
                	url = uri('%s')
                    }
                }

                dependencies {
                    implementation 'org.acme:sample-extension:1.0'
                    compileOnly 'org.acme:main-compile-only:1.0'
                    testImplementation 'org.acme:test-helper:1.0'
                    testRuntimeOnly 'org.acme:test-runtime:1.0'
                    testCompileOnly 'org.acme:test-compile-only:1.0'
                }
                """.formatted(pluginId, repository.toUri());
    }
}
