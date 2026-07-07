package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testing.base.TestingExtension;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.QuarkusApplicationPlugin;
import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.dsl.QuarkusApplicationJvmTestSuite;
import io.quarkus.gradle.application.dsl.QuarkusApplicationTests;
import io.quarkus.gradle.application.internal.packaging.PackageResult;
import io.quarkus.gradle.application.internal.packaging.PackageResultCodec;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;
import io.quarkus.gradle.application.tasks.QuarkusApplicationIntegrationTestMetadataTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupArchiveTrainingMetadataTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupArchiveValidationTask;
import io.quarkus.gradle.testing.BaseGradleTest;

@SuppressWarnings("UnstableApiUsage")
class QuarkusApplicationTestRegistrationTest extends BaseGradleTest {

    @Test
    void explicitTestDslConfiguresSelectedTasksOnly() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);

        TaskProvider<org.gradle.api.tasks.testing.Test> functionalTest = project.getTasks()
                .register("functionalTest", org.gradle.api.tasks.testing.Test.class);
        TaskProvider<org.gradle.api.tasks.testing.Test> unrelatedTest = project.getTasks()
                .register("unrelatedTest", org.gradle.api.tasks.testing.Test.class);
        extension.tests(tests -> tests.task(functionalTest));

        assertQuarkusTestTaskConfigured((org.gradle.api.tasks.testing.Test) project.getTasks().getByName("test"));
        assertQuarkusTestTaskConfigured(functionalTest.get());
        assertThat(unrelatedTest.get().getTaskDependencies().getDependencies(unrelatedTest.get()))
                .extracting(Task::getName)
                .doesNotContain("quarkusApplicationTestModel");
        assertThat(unrelatedTest.get().getSystemProperties()).doesNotContainKey("java.util.logging.manager");
    }

    @Test
    void allGradleTestTasksDslConfiguresAllTestTasks() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);

        TaskProvider<org.gradle.api.tasks.testing.Test> functionalTest = project.getTasks()
                .register("functionalTest", org.gradle.api.tasks.testing.Test.class);
        extension.tests(QuarkusApplicationTests::allGradleTestTasks);

        assertQuarkusTestTaskConfigured((org.gradle.api.tasks.testing.Test) project.getTasks().getByName("test"));
        assertQuarkusTestTaskConfigured(functionalTest.get());
    }

    @Test
    void jvmTestSuiteCanOptIntoQuarkusFiniteTestsFromSuiteSide() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);

        testing.getSuites().register("integrationTest", JvmTestSuite.class, suite -> {
            ExtensionAware extensionAwareSuite = (ExtensionAware) suite;
            extensionAwareSuite.getExtensions()
                    .getByType(QuarkusApplicationJvmTestSuite.class)
                    .forQuarkusTests();
        });

        assertQuarkusTestTaskConfigured(
                (org.gradle.api.tasks.testing.Test) project.getTasks().getByName("integrationTest"));
    }

    @Test
    void jvmTestSuiteCanOptIntoPackageBackedIntegrationTests() throws IOException {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.builds(builds -> builds.fastJar("fastJar"));
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);

        testing.getSuites().register("integrationTest", JvmTestSuite.class, suite -> {
            ExtensionAware extensionAwareSuite = (ExtensionAware) suite;
            extensionAwareSuite.getExtensions()
                    .getByType(QuarkusApplicationJvmTestSuite.class)
                    .forQuarkusIntegrationTests("fastJar");
        });

        org.gradle.api.tasks.testing.Test integrationTest = (org.gradle.api.tasks.testing.Test) project.getTasks()
                .getByName("integrationTest");
        assertQuarkusTestTaskConfigured(integrationTest);
        assertThat(integrationTest.getTaskDependencies().getDependencies(integrationTest))
                .extracting(Task::getName)
                .contains("quarkusFastJarBuild", "quarkusFastJarIntegrationTestMetadata");
        QuarkusApplicationIntegrationTestMetadataTask metadataTask = (QuarkusApplicationIntegrationTestMetadataTask) project
                .getTasks().getByName("quarkusFastJarIntegrationTestMetadata");
        Path packageOutput = project.getLayout().getBuildDirectory()
                .dir("quarkus-builds/fastJar/package").get().getAsFile().toPath();
        Path packageResultFile = project.getLayout().getBuildDirectory()
                .file("quarkus-build-results/fastJar/package/package-result.properties").get().getAsFile().toPath();
        Path relocatedArtifactProperties = project.getLayout().getBuildDirectory()
                .file("quarkus-build-results/fastJar/package/quarkus-artifact.properties").get().getAsFile().toPath();
        Files.createDirectories(packageOutput.resolve("lib"));
        Files.createFile(packageOutput.resolve("quarkus-run.jar"));
        new PackageResultCodec().write(packageResultFile, new PackageResult(
                "fastJar",
                QuarkusApplicationBuildType.FAST_JAR,
                packageOutput,
                "app",
                packageOutput.resolve("quarkus-run.jar"),
                Optional.empty(),
                Optional.of(packageOutput.resolve("lib")),
                false,
                false,
                Optional.empty(),
                List.of(new PackageResult.Artifact(
                        Optional.of(packageOutput.resolve("quarkus-run.jar")), "jar", Map.of()))));
        writeFile(relocatedArtifactProperties, """
                # Generated by Quarkus - Do not edit manually
                metadata.library-dir=lib
                path=quarkus-run.jar
                type=jar
                """);
        metadataTask.generateIntegrationTestMetadata();
        Properties launcherProperties = new Properties();
        try (var reader = Files.newBufferedReader(project.getLayout().getBuildDirectory()
                .file("quarkus-build-results/fastJar/integration-test/quarkus-artifact.properties")
                .get().getAsFile().toPath())) {
            launcherProperties.load(reader);
        }
        assertThat(launcherProperties)
                .containsEntry("type", "jar")
                .containsEntry("path", packageOutput.resolve("quarkus-run.jar").toAbsolutePath().toString())
                .containsEntry("metadata.library-dir", packageOutput.resolve("lib").toAbsolutePath().toString());
        new QuarkusApplicationIntegrationTestPackageAction(project.getLayout().getBuildDirectory()
                .dir("quarkus-build-results/fastJar/integration-test")).execute(integrationTest);
        assertThat(integrationTest.getSystemProperties())
                .containsEntry("build.output.directory", project.getLayout().getBuildDirectory()
                        .dir("quarkus-build-results/fastJar/integration-test").get().getAsFile().getAbsolutePath())
                .containsEntry("quarkus.management.port", "0");
    }

    @Test
    void groovyJvmTestSuiteDslCanUseConciseQuarkusApplicationBlock() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'groovy-suite-dsl'\n");
        writeFile(testProjectDir.resolve("build.gradle"), """
                            plugins {
                                id 'io.quarkus.application'
                            }

                            repositories {
                                mavenCentral()
                            }

                            version = '1.0.0'

                            quarkusApplication {
                                builds {
                            	fastJar('fastJar')
                                }
                            }

                            testing {
                                suites {
                            	intTest(JvmTestSuite) {
                forQuarkusIntegrationTests 'fastJar'
                            	}
                            	unitTest(JvmTestSuite) {
                forQuarkusTests()
                            	}
                                }
                            }
                """);

        BuildResult result = buildResultWithIsolatedProjects("intTest", "--dry-run");
        BuildResult unitResult = buildResultWithIsolatedProjects("unitTest", "--dry-run");

        assertThat(result.getOutput())
                .contains(":quarkusApplicationTestModel SKIPPED")
                .contains(":quarkusFastJarBuild SKIPPED")
                .contains(":quarkusFastJarIntegrationTestMetadata SKIPPED")
                .contains(":intTest SKIPPED");
        assertThat(unitResult.getOutput())
                .contains(":quarkusApplicationTestModel SKIPPED")
                .contains(":unitTest SKIPPED")
                .doesNotContain(":quarkusFastJarBuild SKIPPED")
                .doesNotContain(":quarkusFastJarIntegrationTestMetadata SKIPPED");
    }

    @Test
    void kotlinJvmTestSuiteDslCanUseConciseQuarkusApplicationBlock() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle.kts"), "rootProject.name = \"kotlin-suite-dsl\"\n");
        writeFile(testProjectDir.resolve("build.gradle.kts"),
                """
                        import org.gradle.api.plugins.jvm.JvmTestSuite

                        plugins {
                            id("io.quarkus.application")
                        }

                        repositories {
                            mavenCentral()
                        }

                        version = "1.0.0"

                        quarkusApplication {
                            builds {
                            fastJar("fastJar")
                            }
                        }

                        testing {
                            suites {
                                register<JvmTestSuite>("intTest") {
                                    forQuarkusIntegrationTests("fastJar")
                                }
                                register<JvmTestSuite>("unitTest") {
                                    forQuarkusTests()
                                }
                            }
                        }
                        """);

        BuildResult result = buildResultWithIsolatedProjects("intTest", "--dry-run");
        BuildResult unitResult = buildResultWithIsolatedProjects("unitTest", "--dry-run");

        assertThat(result.getOutput())
                .contains(":quarkusApplicationTestModel SKIPPED")
                .contains(":quarkusFastJarBuild SKIPPED")
                .contains(":quarkusFastJarIntegrationTestMetadata SKIPPED")
                .contains(":intTest SKIPPED");
        assertThat(unitResult.getOutput())
                .contains(":quarkusApplicationTestModel SKIPPED")
                .contains(":unitTest SKIPPED")
                .doesNotContain(":quarkusFastJarBuild SKIPPED")
                .doesNotContain(":quarkusFastJarIntegrationTestMetadata SKIPPED");
    }

    @Test
    void integrationTestSuiteCanReferenceBuildRegisteredLater() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);

        testing.getSuites().register("integrationTest", JvmTestSuite.class, suite -> {
            ExtensionAware extensionAwareSuite = (ExtensionAware) suite;
            extensionAwareSuite.getExtensions()
                    .getByType(QuarkusApplicationJvmTestSuite.class)
                    .forQuarkusIntegrationTests("late");
        });
        extension.builds(builds -> builds.fastJar("late"));

        org.gradle.api.tasks.testing.Test integrationTest = (org.gradle.api.tasks.testing.Test) project.getTasks()
                .getByName("integrationTest");
        assertThat(integrationTest.getTaskDependencies().getDependencies(integrationTest))
                .extracting(Task::getName)
                .contains("quarkusLateBuild");
    }

    @Test
    void integrationTestWithoutTrainingDoesNotRegisterTrainingTasks() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.getBuilds().aotJar("aot", QuarkusApplicationJvmStartupArchiveType.AOT);
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);
        testing.getSuites().register("integrationTest", JvmTestSuite.class,
                suite -> quarkusSuite(suite).forQuarkusIntegrationTests("aot"));

        assertThat(taskDependencyNames(testTask(project, "integrationTest")))
                .contains("quarkusAotIntegrationTestMetadata")
                .noneMatch(name -> name.contains("StartupArchiveTraining"));
        assertThat(project.getTasks().withType(QuarkusApplicationStartupArchiveTrainingMetadataTask.class))
                .isEmpty();
        assertThat(project.getTasks().withType(QuarkusApplicationStartupArchiveValidationTask.class))
                .isEmpty();
    }

    @Test
    void integrationTestSuiteAcceptsNamedBuildProviderNotation() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        var fastJar = extension.getBuilds().fastJar("fastJar");
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);

        testing.getSuites().register("integrationTest", JvmTestSuite.class, suite -> {
            ExtensionAware extensionAwareSuite = (ExtensionAware) suite;
            extensionAwareSuite.getExtensions()
                    .getByType(QuarkusApplicationJvmTestSuite.class)
                    .forQuarkusIntegrationTests(fastJar);
        });

        org.gradle.api.tasks.testing.Test integrationTest = (org.gradle.api.tasks.testing.Test) project.getTasks()
                .getByName("integrationTest");
        assertThat(integrationTest.getTaskDependencies().getDependencies(integrationTest))
                .extracting(Task::getName)
                .contains("quarkusFastJarBuild");
    }

    @Test
    void jvmTestSuiteRejectsMixedQuarkusTestModes() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);

        assertThatThrownBy(() -> testing.getSuites().register("integrationTest",
                JvmTestSuite.class, suite -> {
                    ExtensionAware extensionAwareSuite = (ExtensionAware) suite;
                    var quarkusSuite = extensionAwareSuite.getExtensions()
                            .getByType(QuarkusApplicationJvmTestSuite.class);
                    quarkusSuite.forQuarkusTests();
                    quarkusSuite.forQuarkusIntegrationTests("fastJar");
                }))
                .hasStackTraceContaining("cannot be configured for both Quarkus JVM and integration tests");
    }

    private static void assertQuarkusTestTaskConfigured(org.gradle.api.tasks.testing.Test task) {
        assertThat(task.getTaskDependencies().getDependencies(task))
                .extracting(Task::getName)
                .contains("quarkusApplicationTestModel");
        assertThat(task.getSystemProperties())
                .containsEntry("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        assertThat(task.getJvmArgs())
                .contains(
                        "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
                        "--add-opens=java.base/java.lang=ALL-UNNAMED",
                        "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED");
        assertThat(task.getOptions().getClass().getName()).contains("JUnitPlatform");
    }

    private static QuarkusApplicationJvmTestSuite quarkusSuite(JvmTestSuite suite) {
        return ((ExtensionAware) suite).getExtensions().getByType(QuarkusApplicationJvmTestSuite.class);
    }

    private static org.gradle.api.tasks.testing.Test testTask(Project project, String name) {
        return (org.gradle.api.tasks.testing.Test) project.getTasks().getByName(name);
    }

    private static List<String> taskDependencyNames(Task task) {
        return task.getTaskDependencies().getDependencies(task).stream()
                .map(Task::getName)
                .toList();
    }
}
