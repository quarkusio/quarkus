package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.testing.BaseGradleTest;

class QuarkusApplicationPluginCoexistenceFunctionalTest extends BaseGradleTest {

    @Test
    void rejectsDevelopmentConfigurationOwnedByAnotherPlugin() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle.kts"), "");
        writeFile(testProjectDir.resolve("buildSrc/build.gradle.kts"), """
                plugins {
                    `java-gradle-plugin`
                }

                gradlePlugin {
                    plugins {
                        create("conflictingPlugin") {
                            id = "org.acme.conflicting-plugin"
                            implementationClass = "ConflictingPlugin"
                        }
                    }
                }
                """);
        writeFile(testProjectDir.resolve("buildSrc/src/main/java/ConflictingPlugin.java"), """
                import org.gradle.api.Plugin;
                import org.gradle.api.Project;

                public final class ConflictingPlugin implements Plugin<Project> {
                    @Override
                    public void apply(Project project) {
                        project.getConfigurations().create("quarkusDev");
                    }
                }
                """);
        writeFile(testProjectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("org.acme.conflicting-plugin")
                    id("io.quarkus.application")
                }
                """);

        assertThatThrownBy(() -> buildResultWithIsolatedProjects("help"))
                .isInstanceOf(UnexpectedBuildFailure.class)
                .hasMessageContaining("configuration 'quarkusDev'")
                .hasMessageContaining("legacy 'io.quarkus' plugin does not own it");
    }

    @Test
    void warnsWhenLegacyPluginIsAlsoApplied() throws IOException {
        writeLegacyPluginStub();
        writeFile(testProjectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("io.quarkus")
                    id("io.quarkus.application")
                }

                quarkusApplication {
                    builds {
                        nativeExecutable("native")
                    }
                }

                val java = extensions.getByType<org.gradle.api.plugins.JavaPluginExtension>()
                val mainSourceDirs = java.sourceSets.named("main").get().java.srcDirs
                val testSourceDirs = java.sourceSets.named("test").get().java.srcDirs
                val quarkusDev = configurations.named("quarkusDev").get()
                val devBaseRuntime =
                    configurations.named("quarkusApplicationDevBaseRuntimeClasspathConfiguration").get()

                check(quarkusDev.description == "legacy-owned development dependencies")
                check(quarkusDev.isCanBeResolved) {
                    "new plugin must not mutate the legacy-owned quarkusDev configuration"
                }
                check(devBaseRuntime.extendsFrom.contains(quarkusDev)) {
                    "new plugin development runtime must inherit the legacy-owned quarkusDev configuration"
                }

                check(mainSourceDirs.none {
                    it.invariantSeparatorsPath.contains("generated/sources/quarkus-application")
                }) {
                    "new plugin generated sources must not be added to the shared main source set"
                }
                check(testSourceDirs.none {
                    it.invariantSeparatorsPath.contains("generated/sources/quarkus-application")
                }) {
                    "new plugin generated sources must not be added to the shared test source set"
                }
                tasks.named("test", org.gradle.api.tasks.testing.Test::class).configure {
                    check(!systemProperties.containsKey("java.util.logging.manager")) {
                        "new plugin must not configure Test tasks when legacy io.quarkus owns test instrumentation"
                    }
                }
                check(testing.suites.findByName("quarkusNativeNativeTest") == null) {
                    "legacy-first coexistence must suppress generated named native-test suites"
                }
                check(tasks.findByName("quarkusNativeNativeTest") == null) {
                    "legacy-first coexistence must suppress generated named native-test tasks"
                }
                """);

        var result = buildResultWithIsolatedProjects("tasks");

        assertThat(result.getOutput())
                .contains("Both 'io.quarkus.application' and legacy 'io.quarkus' are applied")
                .contains("migration mode")
                .contains("legacy 'io.quarkus' is applied first")
                .contains("Legacy owns Gradle Test task instrumentation");
    }

    @Test
    void rejectsNamedNativeAttachmentWhenLegacyOwnsTestInstrumentation() throws IOException {
        writeLegacyPluginStub();
        writeFile(testProjectDir.resolve("build.gradle.kts"), """
                import org.gradle.api.plugins.jvm.JvmTestSuite

                plugins {
                    id("io.quarkus")
                    id("io.quarkus.application")
                }

                quarkusApplication {
                    builds {
                        nativeExecutable("native")
                    }
                }

                testing.suites.register<JvmTestSuite>("integrationTest") {
                    forQuarkusIntegrationTests("native")
                }
                """);

        assertThatThrownBy(() -> buildResultWithIsolatedProjects("tasks"))
                .isInstanceOf(UnexpectedBuildFailure.class)
                .hasMessageContaining("Quarkus integration-test suite 'integrationTest'")
                .hasMessageContaining("named native build 'native'")
                .hasMessageContaining("legacy 'io.quarkus' owns Gradle Test task instrumentation")
                .hasMessageContaining("apply only 'io.quarkus.application'");
    }

    @Test
    void rejectsLegacyPluginAppliedAfterStandalonePlugin() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle.kts"), "");
        writeFile(testProjectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("io.quarkus.application")
                    id("io.quarkus")
                }
                """);

        assertThatThrownBy(() -> buildResultWithIsolatedProjects("tasks"))
                .isInstanceOf(UnexpectedBuildFailure.class)
                .hasMessageContaining("Legacy plugin 'io.quarkus' must be applied before 'io.quarkus.application'")
                .hasMessageContaining("Apply 'io.quarkus' first");
    }

    private void writeLegacyPluginStub() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle.kts"), "");
        writeFile(testProjectDir.resolve("buildSrc/build.gradle.kts"), """
                plugins {
                    `java-gradle-plugin`
                }

                gradlePlugin {
                    plugins {
                        create("legacyQuarkus") {
                            id = "io.quarkus"
                            implementationClass = "LegacyQuarkusPlugin"
                        }
                    }
                }
                """);
        writeFile(testProjectDir.resolve("buildSrc/src/main/java/LegacyQuarkusPlugin.java"), """
                import org.gradle.api.Plugin;
                import org.gradle.api.Project;

                public final class LegacyQuarkusPlugin implements Plugin<Project> {
                    @Override
                    public void apply(Project project) {
                        var quarkusDev = project.getConfigurations().create("quarkusDev");
                        quarkusDev.setDescription("legacy-owned development dependencies");
                        quarkusDev.setCanBeConsumed(false);
                        quarkusDev.setCanBeResolved(true);
                    }
                }
                """);
    }

}
