package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class QuarkusApplicationPluginFunctionalTest extends QuarkusApplicationPluginFunctionalTestSupport {

    @Test
    void pluginIdCreatesExtensionWithConfigurationCacheAndIsolatedProjects() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle.kts"), "");
        writeFile(testProjectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("io.quarkus.application")
                }

                quarkusApplication {
                    builds {
                        fastJar("app")
                    }
                }

                check(extensions.findByName("quarkusApplication") != null)
                check(tasks.findByName("quarkusAppBuild") != null)
                """);

        var result = buildResultWithIsolatedProjects("tasks");

        assertTaskOutcomes(result, SUCCESS, ":tasks");
    }

    @Test
    void namedBuildAssembleOptInIsLazyWithConfigurationCacheAndIsolatedProjects() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle.kts"), "");
        writeFile(testProjectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("io.quarkus.application")
                }

                version = "1.0"

                quarkusApplication {
                    builds {
                        fastJar("default")
                        fastJar("selected") {
                            participatesInAssemble.set(true)
                            deployments {
                                kubernetes("dev")
                            }
                        }
                        nativeSources("native") {
                            participatesInAssemble.set(true)
                        }
                    }
                }
                """);

        BuildResult first = buildResultWithIsolatedProjects("assemble", "--dry-run");
        assertAssembleTaskGraph(first);

        BuildResult second = buildResultWithIsolatedProjects("assemble", "--dry-run");
        assertConfigurationCacheReused(second);
        assertAssembleTaskGraph(second);
    }

    private static void assertAssembleTaskGraph(BuildResult result) {
        assertThat(result.getOutput())
                .contains(
                        ":quarkusSelectedBuild SKIPPED",
                        ":quarkusNativeBuild SKIPPED",
                        ":assemble SKIPPED")
                .doesNotContain(
                        ":quarkusDefaultBuild SKIPPED",
                        ":quarkusSelectedRun SKIPPED",
                        ":quarkusSelectedImageBuild SKIPPED",
                        ":quarkusSelectedImagePush SKIPPED",
                        ":quarkusSelectedDeployToDev SKIPPED",
                        ":quarkusNativeImageBuild SKIPPED",
                        ":quarkusNativeImagePush SKIPPED");
    }

    @Test
    void kotlinDslDeclaresDependenciesOnDevelopmentOnlyConfiguration() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle.kts"), "");
        writeFile(testProjectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("io.quarkus.application")
                }

                dependencies {
                    quarkusDev("org.acme:must-not-resolve-during-help:1.0")
                }

                val developmentDependencies = configurations.named("quarkusDev").get()
                check(developmentDependencies.isCanBeDeclared)
                check(!developmentDependencies.isCanBeResolved)
                check(!developmentDependencies.isCanBeConsumed)
                check(developmentDependencies.dependencies.size == 1)
                """);

        BuildResult first = buildResultWithIsolatedProjects("help");
        assertTaskOutcomes(first, SUCCESS, ":help");

        BuildResult second = buildResultWithIsolatedProjects("help");
        assertConfigurationCacheReused(second);
        assertTaskOutcomes(second, SUCCESS, ":help");
    }

    @Test
    void selectedAmbientConfigValuesInvalidateTaskOutputs() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), "");
        writeFile(testProjectDir.resolve("build.gradle"), """
                plugins {
                    id 'io.quarkus.application'
                }

                group = 'org.acme'
                version = '1.0'

                quarkusApplication {
                    configInputs {
                        projectProperties {
                            prefixes.set([])
                            names.set(['probe.gradle'])
                        }
                        systemProperties {
                            prefixes.set([])
                            names.set(['probe.system'])
                        }
                        environmentVariables {
                            prefixes.set([])
                            names.set(['PROBE_ENV'])
                        }
                    }
                    builds.fastJar('probe')
                }

                def effectiveConfig = tasks.named('quarkusProbeShowEffectiveConfig')
                tasks.register('selectedConfigProbe') {
                    def selectedGradle = effectiveConfig.map { it.getSelectedGradleProperties() }
                    def selectedSystem = effectiveConfig.map { it.getSelectedSystemProperties() }
                    def selectedEnvironment = effectiveConfig.map { it.getSelectedEnvironmentVariables() }
                    def result = layout.buildDirectory.file('selected-config.txt')
                    inputs.property('selectedGradleProperties', selectedGradle)
                    inputs.property('selectedSystemProperties', selectedSystem)
                    inputs.property('selectedEnvironmentVariables', selectedEnvironment)
                    outputs.file(result)
                    doLast {
                        result.get().asFile.text = [
                            selectedGradle.get()['probe.gradle'],
                            selectedSystem.get()['probe.system'],
                            selectedEnvironment.get()['PROBE_ENV']
                        ].join('|')
                    }
                }
                """);

        var firstArguments = isolatedProjectsGradleArguments(
                "selectedConfigProbe", "-Pprobe.gradle=one", "-Dprobe.system=one");
        BuildResult first = prepareBuild(Map.of("PROBE_ENV", "one"), firstArguments).build();
        assertTaskOutcomes(first, SUCCESS, ":selectedConfigProbe");

        BuildResult unchanged = prepareBuild(Map.of("PROBE_ENV", "one"), firstArguments).build();
        assertTaskOutcomes(unchanged, UP_TO_DATE, ":selectedConfigProbe");
        assertConfigurationCacheReused(unchanged);

        BuildResult gradlePropertyChanged = prepareBuild(
                Map.of("PROBE_ENV", "one"),
                isolatedProjectsGradleArguments(
                        "selectedConfigProbe", "-Pprobe.gradle=two", "-Dprobe.system=one"))
                .build();
        assertTaskOutcomes(gradlePropertyChanged, SUCCESS, ":selectedConfigProbe");
        assertThat(testProjectDir.resolve("build/selected-config.txt")).hasContent("two|one|one");

        BuildResult systemPropertyChanged = prepareBuild(
                Map.of("PROBE_ENV", "one"),
                isolatedProjectsGradleArguments(
                        "selectedConfigProbe", "-Pprobe.gradle=two", "-Dprobe.system=two"))
                .build();
        assertTaskOutcomes(systemPropertyChanged, SUCCESS, ":selectedConfigProbe");
        assertThat(testProjectDir.resolve("build/selected-config.txt")).hasContent("two|two|one");

        BuildResult environmentChanged = prepareBuild(
                Map.of("PROBE_ENV", "two"),
                isolatedProjectsGradleArguments(
                        "selectedConfigProbe", "-Pprobe.gradle=two", "-Dprobe.system=two"))
                .build();
        assertTaskOutcomes(environmentChanged, SUCCESS, ":selectedConfigProbe");
        assertThat(testProjectDir.resolve("build/selected-config.txt")).hasContent("two|two|two");

        BuildResult changedUnchanged = prepareBuild(
                Map.of("PROBE_ENV", "two"),
                isolatedProjectsGradleArguments(
                        "selectedConfigProbe", "-Pprobe.gradle=two", "-Dprobe.system=two"))
                .build();
        assertTaskOutcomes(changedUnchanged, UP_TO_DATE, ":selectedConfigProbe");
        assertConfigurationCacheReused(changedUnchanged);
    }

    @Test
    void effectiveConfigValuesRequireExplicitTaskOptionAndNeverIncludeAmbientWinners() throws IOException {
        String fileValue = "file-canary-7b18d3";
        String dslValue = "dsl-canary-69c2a1";
        String projectValue = "project-canary-41ef5d";
        String systemValue = "system-canary-c84b20";
        String environmentValue = "environment-canary-a307e9";
        writeFile(testProjectDir.resolve("settings.gradle"), "");
        writeFile(testProjectDir.resolve("src/main/resources/application.properties"),
                "quarkus.diagnostic.file=" + fileValue + "\n");
        writeFile(testProjectDir.resolve("build.gradle"), """
                plugins {
                    id 'io.quarkus.application'
                }

                group = 'org.acme'
                version = '1.0'

                quarkusApplication {
                    quarkusBuildProperties.put('quarkus.diagnostic.dsl', '%s')
                    builds.fastJar('probe')
                }
                """.formatted(dslValue));

        var defaultArguments = isolatedProjectsGradleArguments(
                "quarkusProbeShowEffectiveConfig",
                "-Pquarkus.diagnostic.project=" + projectValue,
                "-Dquarkus.diagnostic.system=" + systemValue);
        BuildResult defaultResult = prepareBuild(
                Map.of("QUARKUS_DIAGNOSTIC_ENVIRONMENT", environmentValue),
                defaultArguments)
                .build();

        assertTaskOutcomes(defaultResult, SUCCESS, ":quarkusProbeShowEffectiveConfig");
        assertThat(defaultResult.getOutput())
                .contains("quarkus.diagnostic.file    source=application.properties")
                .contains("quarkus.diagnostic.dsl    source=Quarkus build DSL")
                .contains("quarkus.diagnostic.project    source=Gradle project properties")
                .contains("system-property/environment")
                .contains("--show-values")
                .doesNotContain(fileValue, dslValue, projectValue, systemValue, environmentValue);

        var explicitArguments = isolatedProjectsGradleArguments(
                "quarkusProbeShowEffectiveConfig",
                "--show-values",
                "-Pquarkus.diagnostic.project=" + projectValue,
                "-Dquarkus.diagnostic.system=" + systemValue);
        BuildResult explicitResult = prepareBuild(
                Map.of("QUARKUS_DIAGNOSTIC_ENVIRONMENT", environmentValue),
                explicitArguments)
                .build();

        assertThat(explicitResult.getOutput())
                .contains("Showing captured effective configuration values")
                .contains(fileValue, dslValue, projectValue)
                .doesNotContain(systemValue, environmentValue);

        BuildResult explicitAgain = prepareBuild(
                Map.of("QUARKUS_DIAGNOSTIC_ENVIRONMENT", environmentValue),
                explicitArguments)
                .build();
        assertConfigurationCacheReused(explicitAgain);
        assertThat(explicitAgain.getOutput())
                .contains("Showing captured effective configuration values")
                .contains(fileValue, dslValue, projectValue)
                .doesNotContain(systemValue, environmentValue);

        BuildResult defaultAgain = prepareBuild(
                Map.of("QUARKUS_DIAGNOSTIC_ENVIRONMENT", environmentValue),
                defaultArguments)
                .build();

        assertConfigurationCacheReused(defaultAgain);
        assertThat(defaultAgain.getOutput())
                .doesNotContain(fileValue, dslValue, projectValue, systemValue, environmentValue);
    }

    @Test
    void buildsTinyFastJarInMultiProjectBuildWithIsolatedProjects() throws IOException {
        writeMultiProjectApplication(false);
        Files.writeString(testProjectDir.resolve("app/build.gradle"), """

                dependencies {
                    quarkusDev 'org.acme:must-not-resolve-during-packaging:1.0'
                }
                """, StandardOpenOption.APPEND);

        BuildResult result = buildResultWithIsolatedProjects(":app:quarkusAppBuild");

        assertTaskOutcomes(result, SUCCESS, ":app:quarkusApplicationModel", ":app:quarkusAppBuild");
        assertThat(testProjectDir.resolve(Path.of("app", "build", "quarkus-builds", "app", "package",
                "quarkus-run.jar"))).isRegularFile();
    }

    @Test
    void buildsTinyFastJarWithPlainProjectDependencyAndIsolatedProjects() throws IOException {
        writeMultiProjectApplication(true);

        BuildResult result = buildResultWithIsolatedProjects(":app:quarkusAppBuild");

        assertTaskOutcomes(result, SUCCESS,
                ":lib:jar",
                ":app:quarkusApplicationModel",
                ":app:quarkusAppBuild");
        assertThat(testProjectDir.resolve(Path.of("app", "build", "quarkus-builds", "app", "package",
                "quarkus-run.jar"))).isRegularFile();
    }

    @Test
    void finiteTestTaskExposesQuarkusBootstrapPropertiesAtExecutionTime() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), "");
        writeFile(testProjectDir.resolve("build.gradle"), """
                plugins {
                    id 'io.quarkus.application'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    quarkusDev 'org.acme:must-not-resolve-during-finite-tests:1.0'
                    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.10.3'
                    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.10.3'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher:1.10.3'
                    testRuntimeOnly 'org.jboss.logmanager:jboss-logmanager:3.2.2.Final'
                }
                """);
        writeFile(testProjectDir.resolve("src/main/java/org/acme/App.java"), """
                package org.acme;

                public final class App {
                }
                """);
        writeFile(testProjectDir.resolve("src/test/java/org/acme/AppTest.java"), """
                package org.acme;

                import static org.junit.jupiter.api.Assertions.assertFalse;
                import static org.junit.jupiter.api.Assertions.assertNotNull;
                import static org.junit.jupiter.api.Assertions.assertTrue;

                import org.junit.jupiter.api.Test;

                class AppTest {
                    @Test
                    void quarkusBootstrapInputsAreVisible() {
                        assertNotNull(System.getProperty("quarkus-internal-test.serialized-app-model.path"));
                        assertNotNull(System.getProperty("OUTPUT_SOURCES_DIR"));
                        String mappings = System.getenv("TEST_TO_MAIN_MAPPINGS");
                        assertNotNull(mappings);
                        assertFalse(mappings.isBlank());
                        String normalizedMappings = mappings.replace('\\\\', '/');
                        assertTrue(normalizedMappings.contains("build/classes/java/test:build/classes/java/main"),
                                normalizedMappings);
                    }
                }
                """);

        BuildResult result = buildResultWithIsolatedProjects("test", "-Dquarkus.test.profile=from-system-property");

        assertTaskOutcomes(result, SUCCESS, ":quarkusApplicationTestModel", ":test");
    }

}
