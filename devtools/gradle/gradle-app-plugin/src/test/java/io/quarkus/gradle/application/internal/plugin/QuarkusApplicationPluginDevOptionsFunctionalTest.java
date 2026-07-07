package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.junit.jupiter.api.Test;

class QuarkusApplicationPluginDevOptionsFunctionalTest
        extends QuarkusApplicationPluginFunctionalTestSupport {

    private static void assertDevLaunchOptions(BuildResult result) {
        assertThat(result.getOutput())
                .contains("--jvm-args")
                .contains("--quarkus-args")
                .contains("--continuous-testing")
                .contains("--no-continuous-testing")
                .contains("--modules")
                .contains("--open-lang-package")
                .contains("--compiler-args")
                .contains("--tests")
                .contains("--working-directory")
                .contains("--environment")
                .contains("--quarkus-debug")
                .contains("--no-quarkus-debug")
                .contains("--debug-mode")
                .contains("--debug-host")
                .contains("--debug-port")
                .contains("--suspend")
                .contains("--no-suspend")
                .contains("--force-c2")
                .contains("--no-force-c2")
                .contains("--disable-all-extension-jvm-options")
                .contains("--no-disable-all-extension-jvm-options")
                .contains("--disable-extension-jvm-options-for");
        assertThat(result.getOutput().lines().map(String::trim).toList())
                .noneMatch(line -> line.equals("--debug") || line.startsWith("--debug "));
    }

    @Test
    void launchTaskOptionsAreAvailableForRunAndDevTasks() throws IOException {
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
                """);

        BuildResult runHelp = buildResultWithIsolatedProjects("help", "--task", "quarkusAppRun");
        assertThat(runHelp.getOutput())
                .contains("--jvm-args")
                .contains("--quarkus-args");

        BuildResult devHelp = buildResultWithIsolatedProjects("help", "--task", "quarkusApplicationDev");
        assertDevLaunchOptions(devHelp);
        BuildResult reusedDevHelp = buildResultWithIsolatedProjects("help", "--task", "quarkusApplicationDev");
        assertConfigurationCacheReused(reusedDevHelp);
        assertDevLaunchOptions(reusedDevHelp);

        BuildResult continuousTestHelp = buildResultWithIsolatedProjects(
                "help", "--task", "quarkusApplicationContinuousTest");
        assertDevLaunchOptions(continuousTestHelp);
        BuildResult reusedContinuousTestHelp = buildResultWithIsolatedProjects(
                "help", "--task", "quarkusApplicationContinuousTest");
        assertConfigurationCacheReused(reusedContinuousTestHelp);
        assertDevLaunchOptions(reusedContinuousTestHelp);
    }

    @Test
    void devLaunchDslConfiguresInKotlinWithConfigurationCacheAndIsolatedProjects() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle.kts"), "");
        writeFile(testProjectDir.resolve("build.gradle.kts"), """
                import io.quarkus.gradle.application.model.QuarkusApplicationDevDebugMode

                plugins {
                    id("io.quarkus.application")
                }

                quarkusApplication {
                    dev {
                        workingDirectory.set(layout.projectDirectory)
                        environmentVariables.put("APP_MODE", "kotlin")
                        debug = true
                        debugMode = QuarkusApplicationDevDebugMode.CONNECT
                        debugHost = "localhost"
                        debugPort = 5005
                        suspend = false
                        forceC2 = false
                        extensionJvmOptions {
                            disableAll = false
                            disableFor.add("org.acme:acme-extension")
                        }
                    }
                }
                """);

        BuildResult first = buildResultWithIsolatedProjects("help", "--task", "quarkusApplicationDev");
        assertDevLaunchOptions(first);
        BuildResult second = buildResultWithIsolatedProjects("help", "--task", "quarkusApplicationDev");
        assertConfigurationCacheReused(second);
    }

    @Test
    void devLaunchDslConfiguresInGroovyWithConfigurationCacheAndIsolatedProjects() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), "");
        writeFile(testProjectDir.resolve("build.gradle"), """
                import io.quarkus.gradle.application.model.QuarkusApplicationDevDebugMode

                plugins {
                    id 'io.quarkus.application'
                }

                quarkusApplication {
                    dev {
                        workingDirectory.set(layout.projectDirectory)
                        environmentVariables.put("APP_MODE", "groovy")
                        debug = true
                        debugMode = QuarkusApplicationDevDebugMode.LISTEN
                        debugHost = "localhost"
                        debugPort = 5005
                        suspend = false
                        forceC2 = false
                        extensionJvmOptions {
                            disableAll = false
                            disableFor.add("org.acme:acme-extension")
                        }
                    }
                }
                """);

        BuildResult first = buildResultWithIsolatedProjects("help", "--task", "quarkusApplicationDev");
        assertDevLaunchOptions(first);
        BuildResult second = buildResultWithIsolatedProjects("help", "--task", "quarkusApplicationDev");
        assertConfigurationCacheReused(second);
    }

    @Test
    void devTaskHelpDoesNotResolveAnUnavailableJavaToolchain() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle.kts"), "");
        writeFile(testProjectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("io.quarkus.application")
                }

                java {
                    toolchain.languageVersion = JavaLanguageVersion.of(99)
                }
                """);

        BuildResult result = buildResultWithIsolatedProjects("help", "--task", "quarkusApplicationDev");

        assertDevLaunchOptions(result);
        assertThat(result.getOutput()).doesNotContain("No matching toolchains found");
    }

    @Test
    void gradleParsesTypedDevTaskOptions() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle.kts"), "");
        writeFile(testProjectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("io.quarkus.application")
                }
                """);

        BuildResult valid = buildResultWithIsolatedProjects(
                "quarkusApplicationDev",
                "--debug-mode", "CONNECT",
                "--debug-port", "0",
                "--no-quarkus-debug",
                "--disable-extension-jvm-options-for", "org.acme:one",
                "--disable-extension-jvm-options-for", "org.acme:two",
                "--dry-run");
        assertThat(valid.getOutput()).contains(":quarkusApplicationDev SKIPPED");

        BuildResult invalid = prepareBuildWithIsolatedProjects(
                "quarkusApplicationDev", "--debug-mode", "SIDEWAYS", "--dry-run")
                .buildAndFail();
        assertThat(invalid.getOutput())
                .contains("SIDEWAYS")
                .contains("--debug-mode");
    }

    @Test
    void invalidEnvironmentDoesNotExposeItsValueInGradleOutput() throws IOException {
        String sensitiveValue = "DO_NOT_DISCLOSE_DEV_ENV_VALUE";
        writeMultiProjectApplication(false);
        Files.writeString(testProjectDir.resolve("app/build.gradle"), """

                quarkusApplication {
                    dev.environmentVariables.put("", "%s")
                }
                """.formatted(sensitiveValue), StandardOpenOption.APPEND);

        BuildResult result = prepareBuildWithIsolatedProjects(
                ":app:quarkusApplicationDev",
                "--no-quarkus-debug")
                .buildAndFail();

        assertThat(result.getOutput())
                .contains("quarkusApplication.dev.environmentVariables entry has an empty or blank name")
                .doesNotContain(sensitiveValue);
    }

    @Test
    void devContinuousTestingTaskOptionOverridesTheDslDefault() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle.kts"), "");
        writeFile(testProjectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("io.quarkus.application")
                }
                """);

        BuildResult enabled = buildResultWithIsolatedProjects(
                "quarkusApplicationDev", "--continuous-testing", "--dry-run");

        assertThat(enabled.getOutput())
                .contains(":quarkusApplicationDevCodegenModel SKIPPED")
                .contains(":quarkusApplicationGenerateDevCode SKIPPED")
                .contains(":quarkusApplicationContinuousTestModel SKIPPED")
                .contains(":quarkusApplicationGenerateTestCode SKIPPED")
                .contains(":testClasses SKIPPED");

        writeFile(testProjectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("io.quarkus.application")
                }

                quarkusApplication {
                    dev.continuousTesting = true
                }
                """);

        BuildResult disabled = buildResultWithIsolatedProjects(
                "quarkusApplicationDev", "--no-continuous-testing", "--dry-run");

        assertThat(disabled.getOutput())
                .doesNotContain(":quarkusApplicationContinuousTestModel SKIPPED")
                .doesNotContain(":quarkusApplicationGenerateTestCode SKIPPED")
                .doesNotContain(":testClasses SKIPPED");
    }

    @Test
    void devTaskFailsEarlyWithoutContinuousBuildInTestKitBuild() throws IOException {
        writeMultiProjectApplication(false);

        assertThatThrownBy(() -> buildResultWithIsolatedProjects(":app:quarkusApplicationDev"))
                .isInstanceOf(UnexpectedBuildFailure.class)
                .hasMessageContaining("requires Gradle continuous build")
                .hasMessageContaining("--continuous");
    }

    @Test
    void continuousBuildTriggerInitializersPreserveDeploymentValuesWithConfigurationCacheAndIsolatedProjects()
            throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), "");
        writeFile(testProjectDir.resolve("build.gradle"), """
                plugins {
                    id 'io.quarkus.application'
                }
                """);
        assertTriggerInitializer(
                "quarkusApplicationDevInitializeReplayTrigger",
                testProjectDir.resolve("build/quarkus-dev/live-reload-replay.trigger"));
        assertTriggerInitializer(
                "quarkusApplicationRemoteDevInitializeReconnectTrigger",
                testProjectDir.resolve("build/quarkus-remote-dev/reconnect/reconnect.trigger"));
    }

    private void assertTriggerInitializer(String taskName, Path trigger) throws IOException {
        BuildResult initialized = buildResultWithIsolatedProjects(taskName, BUILD_CACHE);
        assertTaskOutcomes(initialized, SUCCESS, ":" + taskName);
        assertThat(trigger).hasContent("epoch=initializer\ngeneration=0\n");

        Files.writeString(trigger, "epoch=deployment\ngeneration=2\n");
        BuildResult preserved = buildResultWithIsolatedProjects(taskName, BUILD_CACHE);
        assertTaskOutcomes(preserved, SUCCESS, ":" + taskName);
        assertThat(trigger).hasContent("epoch=deployment\ngeneration=2\n");

        BuildResult stable = buildResultWithIsolatedProjects(taskName, BUILD_CACHE);
        assertTaskOutcomes(stable, UP_TO_DATE, ":" + taskName);
    }

}
