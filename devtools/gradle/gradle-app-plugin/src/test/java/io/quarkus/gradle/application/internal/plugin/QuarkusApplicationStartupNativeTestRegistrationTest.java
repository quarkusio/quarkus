package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.TaskOutputsInternal;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testing.base.TestingExtension;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.QuarkusApplicationPlugin;
import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.dsl.QuarkusApplicationJvmTestSuite;
import io.quarkus.gradle.application.internal.nativeimage.NativeResult;
import io.quarkus.gradle.application.internal.nativeimage.NativeResultCodec;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;
import io.quarkus.gradle.application.model.QuarkusApplicationStartupArchiveTrainingExecutionTarget;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupArchiveTrainingMetadataTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupArchiveValidationTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupOptimizedImageBuildTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupOptimizedImagePushTask;
import io.quarkus.gradle.testing.BaseGradleTest;

@SuppressWarnings("UnstableApiUsage")
class QuarkusApplicationStartupNativeTestRegistrationTest extends BaseGradleTest {

    @Test
    void namedNativeExecutableRegistersGradleTestSuite() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);

        extension.builds(builds -> builds.nativeExecutable("native"));

        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);
        assertThat(testing.getSuites().findByName("quarkusNativeNativeTest"))
                .isInstanceOf(JvmTestSuite.class);
        Task nativeTest = project.getTasks().getByName("quarkusNativeNativeTest");
        assertThat(nativeTest).isInstanceOf(org.gradle.api.tasks.testing.Test.class);
        assertThat(nativeTest.getTaskDependencies().getDependencies(nativeTest))
                .extracting(Task::getName)
                .contains("quarkusNativeBuild");
        Task check = project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME);
        assertThat(check.getTaskDependencies().getDependencies(check))
                .extracting(Task::getName)
                .doesNotContain("quarkusNativeNativeTest");
    }

    @Test
    void groovyJvmTestSuiteDslExposesDirectStartupArchiveTrainingModifier() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'groovy-training-dsl'\n");
        writeFile(testProjectDir.resolve("build.gradle"), """
                import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType
                import io.quarkus.gradle.application.model.QuarkusApplicationStartupArchiveTrainingExecutionTarget
                import org.gradle.api.plugins.jvm.JvmTestSuite

                plugins {
                    id 'io.quarkus.application'
                }

                repositories {
                    mavenCentral()
                }

                version = '1.0.0'

                quarkusApplication {
                    builds {
                        aotJar('aot', QuarkusApplicationJvmStartupArchiveType.AOT)
                    }
                }

                testing {
                    suites {
                        training(JvmTestSuite) {
                            startupArchiveTraining {
                                executionTarget.set(
                                    QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM)
                            }
                            forQuarkusIntegrationTests 'aot'
                        }
                    }
                }
                """);

        BuildResult result = buildResultWithIsolatedProjects(
                "quarkusAotStartupArchiveValidation", "--dry-run", CONFIGURATION_CACHE);

        assertThat(result.getOutput())
                .contains(":quarkusAotTrainingStartupArchiveTrainingMetadata SKIPPED")
                .contains(":training SKIPPED")
                .contains(":quarkusAotStartupArchiveValidation SKIPPED");
    }

    @Test
    void startupArchiveTrainingWithoutIntegrationTestModeFailsDuringConfiguration() throws IOException {
        writeFile(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'invalid-training-suite'\n");
        writeFile(testProjectDir.resolve("build.gradle"), """
                import io.quarkus.gradle.application.model.QuarkusApplicationStartupArchiveTrainingExecutionTarget
                import org.gradle.api.plugins.jvm.JvmTestSuite

                plugins {
                    id 'io.quarkus.application'
                }

                testing {
                    suites {
                        training(JvmTestSuite) {
                            startupArchiveTraining {
                                executionTarget.set(
                                    QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM)
                            }
                        }
                    }
                }
                """);

        BuildResult result = buildAndFailResult(
                "help", CONFIGURATION_CACHE, "-Dorg.gradle.unsafe.isolated-projects=true");

        assertThat(result.getOutput())
                .contains("configures startup-archive training but is not configured for Quarkus integration tests");
    }

    @Test
    void hostJvmStartupArchiveTrainingWiresOnlyTheSelectedSuiteWhenBuildIsRegisteredFirst() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.getBuilds().aotJar("aot", QuarkusApplicationJvmStartupArchiveType.AOT,
                output -> output.startupOptimizedImage(ignored -> {
                }));
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);

        testing.getSuites().register("integrationTest", JvmTestSuite.class, suite -> {
            QuarkusApplicationJvmTestSuite quarkusSuite = quarkusSuite(suite);
            quarkusSuite.forQuarkusIntegrationTests("aot");
            quarkusSuite.startupArchiveTraining(training -> training.getExecutionTarget()
                    .set(QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM));
        });
        testing.getSuites().register("otherIntegrationTest", JvmTestSuite.class,
                suite -> quarkusSuite(suite).forQuarkusIntegrationTests("aot"));

        org.gradle.api.tasks.testing.Test integrationTest = testTask(project, "integrationTest");
        org.gradle.api.tasks.testing.Test otherIntegrationTest = testTask(project, "otherIntegrationTest");
        QuarkusApplicationStartupArchiveTrainingMetadataTask metadata = trainingMetadataTask(project,
                "quarkusAotIntegrationTestStartupArchiveTrainingMetadata");
        QuarkusApplicationStartupArchiveValidationTask validation = validationTask(project, "quarkusAot");
        QuarkusApplicationStartupOptimizedImageBuildTask startupOptimizedImageBuild = (QuarkusApplicationStartupOptimizedImageBuildTask) project
                .getTasks().getByName("quarkusAotStartupOptimizedImageBuild");
        QuarkusApplicationStartupOptimizedImagePushTask startupOptimizedImagePush = (QuarkusApplicationStartupOptimizedImagePushTask) project
                .getTasks().getByName("quarkusAotStartupOptimizedImagePush");
        Path expectedArchive = project.getLayout().getBuildDirectory()
                .file("quarkus-builds/aot/startup-archive-training/IntegrationTest/app.aot")
                .get().getAsFile().toPath();

        assertThat(metadata.getArchiveType().get()).isEqualTo(QuarkusApplicationJvmStartupArchiveType.AOT);
        assertThat(metadata.getExecutionTarget().get())
                .isEqualTo(QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM);
        assertThat(metadata.getArchiveDestination().get()).isEqualTo(expectedArchive.toAbsolutePath().toString());
        assertThat(metadata.getBaseImageReceiptFile().isPresent()).isFalse();
        assertThat(taskDependencyNames(metadata))
                .contains("quarkusAotIntegrationTestMetadata")
                .doesNotContain("quarkusAotImageBuild");
        assertThat(taskDependencyNames(integrationTest))
                .contains("quarkusAotBuild", metadata.getName());
        assertThat(taskDependencyNames(otherIntegrationTest))
                .contains("quarkusAotBuild", "quarkusAotIntegrationTestMetadata")
                .doesNotContain(metadata.getName());
        assertThat(validation.getArchiveFile().get().getAsFile().toPath()).isEqualTo(expectedArchive);
        assertThat(taskDependencyNames(validation)).contains(integrationTest.getName());
        assertThat(integrationTest.getOutputs().getFiles().getFiles()).contains(expectedArchive.toFile());
        assertThat(otherIntegrationTest.getOutputs().getFiles().getFiles()).doesNotContain(expectedArchive.toFile());
        assertThat(startupOptimizedImageBuild.getArchiveFile().get().getAsFile().toPath()).isEqualTo(expectedArchive);
        assertThat(taskDependencyNames(startupOptimizedImageBuild)).contains(validation.getName());
        assertThat(taskDependencyNames(startupOptimizedImagePush)).contains(validation.getName());

        TaskOutputsInternal outputs = (TaskOutputsInternal) integrationTest.getOutputs();
        assertThat(outputs.getDoNotCacheIfSpecs())
                .singleElement()
                .satisfies(spec -> {
                    assertThat(spec.getDisplayName())
                            .isEqualTo("Startup-archive training depends on runtime workload and platform state");
                    assertThat(spec.isSatisfiedBy((TaskInternal) integrationTest)).isTrue();
                });
        assertThat(outputs.getUpToDateSpec().isSatisfiedBy((TaskInternal) integrationTest)).isFalse();
        assertThat(project.getTasks().withType(QuarkusApplicationStartupArchiveTrainingMetadataTask.class))
                .hasSize(1);
    }

    @Test
    void baseImageStartupArchiveTrainingWiresImageMetadataWhenSuiteIsRegisteredFirst() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);

        testing.getSuites().register("integrationTest", JvmTestSuite.class, suite -> {
            QuarkusApplicationJvmTestSuite quarkusSuite = quarkusSuite(suite);
            quarkusSuite.startupArchiveTraining(training -> training.getExecutionTarget()
                    .set(QuarkusApplicationStartupArchiveTrainingExecutionTarget.BASE_IMAGE));
            quarkusSuite.forQuarkusIntegrationTests("scc");
        });
        var scc = extension.getBuilds()
                .aotJar("scc", QuarkusApplicationJvmStartupArchiveType.SCC,
                        output -> output.startupOptimizedImage(ignored -> {
                        }));

        org.gradle.api.tasks.testing.Test integrationTest = testTask(project, "integrationTest");
        QuarkusApplicationStartupArchiveTrainingMetadataTask metadata = trainingMetadataTask(project,
                "quarkusSccIntegrationTestStartupArchiveTrainingMetadata");
        QuarkusApplicationStartupArchiveValidationTask validation = validationTask(project, "quarkusScc");
        QuarkusApplicationStartupOptimizedImageBuildTask startupOptimizedImageBuild = (QuarkusApplicationStartupOptimizedImageBuildTask) project
                .getTasks().getByName("quarkusSccStartupOptimizedImageBuild");
        QuarkusApplicationStartupOptimizedImagePushTask startupOptimizedImagePush = (QuarkusApplicationStartupOptimizedImagePushTask) project
                .getTasks().getByName("quarkusSccStartupOptimizedImagePush");
        Path expectedArchive = project.getLayout().getBuildDirectory()
                .dir("quarkus-builds/scc/startup-archive-training/IntegrationTest/app-scc")
                .get().getAsFile().toPath();

        assertThat(metadata.getArchiveType().get()).isEqualTo(QuarkusApplicationJvmStartupArchiveType.SCC);
        assertThat(metadata.getExecutionTarget().get())
                .isEqualTo(QuarkusApplicationStartupArchiveTrainingExecutionTarget.BASE_IMAGE);
        assertThat(metadata.getArchiveDestination().get()).isEqualTo(expectedArchive.toAbsolutePath().toString());
        assertThat(metadata.getBaseImageReceiptFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus-build-results/scc/image-build/image-build-result.properties")
                        .get().getAsFile());
        assertThat(taskDependencyNames(metadata))
                .contains("quarkusSccIntegrationTestMetadata", "quarkusSccImageBuild");
        assertThat(validation.getArchiveDirectory().get().getAsFile().toPath()).isEqualTo(expectedArchive);
        assertThat(validation.getArchiveFile().isPresent()).isFalse();
        assertThat(integrationTest.getOutputs().getFiles().getFiles()).contains(expectedArchive.toFile());
        assertThat(scc.get().getStartupArchive().getDirectory().get().getAsFile().toPath()).isEqualTo(expectedArchive);
        assertThat(startupOptimizedImageBuild.getArchiveDirectory().get().getAsFile().toPath()).isEqualTo(expectedArchive);
        assertThat(taskDependencyNames(startupOptimizedImageBuild)).contains(validation.getName());
        assertThat(taskDependencyNames(startupOptimizedImagePush)).contains(validation.getName());
    }

    @Test
    void startupArchiveTrainingRejectsMultipleSuitesForOneOutput() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.getBuilds().aotJar("aot", QuarkusApplicationJvmStartupArchiveType.AOT);
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);
        registerTrainingSuite(testing, "firstIntegrationTest", "aot",
                QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM);

        assertThatThrownBy(() -> registerTrainingSuite(testing, "secondIntegrationTest", "aot",
                QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM))
                .hasStackTraceContaining("claimed by more than one integration-test target or suite");
    }

    @Test
    void startupArchiveTrainingRejectsAppCds() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.getBuilds().aotJar("appCds", QuarkusApplicationJvmStartupArchiveType.APP_CDS);
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);

        assertThatThrownBy(() -> registerTrainingSuite(testing, "integrationTest", "appCds",
                QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM))
                .hasStackTraceContaining("Integration-test startup-archive training does not support APP_CDS");
    }

    @Test
    void startupArchiveTrainingRejectsPackageBuildProducer() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.getBuilds().aotJar("aot", QuarkusApplicationJvmStartupArchiveType.AOT,
                output -> output.getStartupArchive().fromPackageBuild());
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);

        assertThatThrownBy(() -> registerTrainingSuite(testing, "integrationTest", "aot",
                QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM))
                .hasStackTraceContaining("cannot use package and integration-test startup archive producers together");
    }

    @Test
    void startupArchiveTrainingRejectsUserSuppliedFile() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.getBuilds().aotJar("aot", QuarkusApplicationJvmStartupArchiveType.AOT,
                output -> output.getStartupArchive().getFile()
                        .set(project.getLayout().getProjectDirectory().file("training/app.aot")));
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);

        assertThatThrownBy(() -> registerTrainingSuite(testing, "integrationTest", "aot",
                QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM))
                .hasStackTraceContaining("cannot combine integration-test training with a user-supplied startup archive");
    }

    @Test
    void startupArchiveTrainingRejectsUserSuppliedDirectory() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.getBuilds().aotJar("scc", QuarkusApplicationJvmStartupArchiveType.SCC,
                output -> output.getStartupArchive().getDirectory()
                        .set(project.getLayout().getProjectDirectory().dir("training/app-scc")));
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);

        assertThatThrownBy(() -> registerTrainingSuite(testing, "integrationTest", "scc",
                QuarkusApplicationStartupArchiveTrainingExecutionTarget.BASE_IMAGE))
                .hasStackTraceContaining("cannot combine integration-test training with a user-supplied startup archive");
    }

    @Test
    void startupArchiveTrainingRequiresOneExplicitTargetPerSuite() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);

        assertThatThrownBy(() -> testing.getSuites().register("missingTarget", JvmTestSuite.class, suite -> quarkusSuite(suite)
                .startupArchiveTraining(ignored -> {
                })))
                .hasStackTraceContaining("requires an explicit execution target");

        assertThatThrownBy(() -> testing.getSuites().register("duplicateTarget", JvmTestSuite.class, suite -> {
            QuarkusApplicationJvmTestSuite quarkusSuite = quarkusSuite(suite);
            quarkusSuite.startupArchiveTraining(training -> training.getExecutionTarget()
                    .set(QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM));
            quarkusSuite.startupArchiveTraining(training -> training.getExecutionTarget()
                    .set(QuarkusApplicationStartupArchiveTrainingExecutionTarget.BASE_IMAGE));
        }))
                .hasStackTraceContaining("cannot configure startup-archive training more than once");
    }

    @Test
    void startupArchiveTrainingRejectsQuarkusJvmTestModeInEitherDslOrder() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);

        assertThatThrownBy(() -> testing.getSuites().register("testsFirst", JvmTestSuite.class, suite -> {
            QuarkusApplicationJvmTestSuite quarkusSuite = quarkusSuite(suite);
            quarkusSuite.forQuarkusTests();
            quarkusSuite.startupArchiveTraining(training -> training.getExecutionTarget()
                    .set(QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM));
        })).hasStackTraceContaining("requires a Quarkus integration-test suite");

        assertThatThrownBy(() -> testing.getSuites().register("trainingFirst", JvmTestSuite.class, suite -> {
            QuarkusApplicationJvmTestSuite quarkusSuite = quarkusSuite(suite);
            quarkusSuite.startupArchiveTraining(training -> training.getExecutionTarget()
                    .set(QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM));
            quarkusSuite.forQuarkusTests();
        })).hasStackTraceContaining("requires a Quarkus integration-test suite");
    }

    @Test
    void startupArchiveTrainingRejectsParallelOrRecycledTestWorkers() {
        Project project = ProjectBuilder.builder().build();
        org.gradle.api.tasks.testing.Test test = project.getTasks().create("training", org.gradle.api.tasks.testing.Test.class);
        var validation = new QuarkusApplicationStartupArchiveTestWorkerValidationAction("training");

        test.setMaxParallelForks(2);
        assertThatThrownBy(() -> validation.execute(test))
                .hasMessageContaining("maxParallelForks=1")
                .hasMessageContaining("forkEvery=0");

        test.setMaxParallelForks(1);
        test.setForkEvery(1);
        assertThatThrownBy(() -> validation.execute(test))
                .hasMessageContaining("maxParallelForks=1")
                .hasMessageContaining("forkEvery=0");

        test.setForkEvery(0);
        validation.execute(test);
    }

    @Test
    void integrationTestSuiteCanUseNativeExecutableBuild() throws IOException {
        Path projectDir = Files.createDirectory(testProjectDir.resolve("native-integration-test"));
        Project project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        extension.builds(builds -> builds.nativeExecutable("native"));
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);

        testing.getSuites().register("integrationTest", JvmTestSuite.class, suite -> {
            ExtensionAware extensionAwareSuite = (ExtensionAware) suite;
            extensionAwareSuite.getExtensions()
                    .getByType(QuarkusApplicationJvmTestSuite.class)
                    .forQuarkusIntegrationTests("native");
        });

        org.gradle.api.tasks.testing.Test integrationTest = (org.gradle.api.tasks.testing.Test) project.getTasks()
                .getByName("integrationTest");
        assertThat(integrationTest.getTaskDependencies().getDependencies(integrationTest))
                .extracting(Task::getName)
                .contains("quarkusNativeBuild");
        Path executable = Files.createFile(projectDir.resolve("application-runner"));
        Path nativeResult = project.getLayout().getBuildDirectory()
                .file("quarkus-build-results/native/package/native-result.properties").get().getAsFile().toPath();
        new NativeResultCodec().write(nativeResult, new NativeResult(
                "native",
                QuarkusApplicationBuildType.NATIVE_EXECUTABLE,
                projectDir,
                "application",
                Optional.of(executable),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                List.of()));

        new QuarkusApplicationIntegrationTestNativeAction(
                project.getLayout().getBuildDirectory().dir("quarkus-build-results/native/package"),
                project.getLayout().getBuildDirectory()
                        .file("quarkus-build-results/native/package/native-result.properties"),
                "integrationTest",
                "native")
                .execute(integrationTest);
        assertThat(integrationTest.getSystemProperties())
                .containsEntry("native.image.path", executable.toAbsolutePath().toString())
                .containsEntry("build.output.directory", project.getLayout().getBuildDirectory()
                        .dir("quarkus-build-results/native/package").get().getAsFile().getAbsolutePath())
                .containsEntry("quarkus.management.port", "0");
    }

    @Test
    void integrationTestSuiteRejectsNativeSourcesBuild() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);
        testing.getSuites().register("integrationTest", JvmTestSuite.class, suite -> {
            ExtensionAware extensionAwareSuite = (ExtensionAware) suite;
            extensionAwareSuite.getExtensions()
                    .getByType(QuarkusApplicationJvmTestSuite.class)
                    .forQuarkusIntegrationTests("sources");
        });

        assertThatThrownBy(() -> extension.builds(builds -> builds.nativeSources("sources")))
                .hasStackTraceContaining("native-sources builds do not produce a runnable artifact");
    }

    private static QuarkusApplicationJvmTestSuite quarkusSuite(JvmTestSuite suite) {
        return ((ExtensionAware) suite).getExtensions().getByType(QuarkusApplicationJvmTestSuite.class);
    }

    private static void registerTrainingSuite(TestingExtension testing, String suiteName, String buildName,
            QuarkusApplicationStartupArchiveTrainingExecutionTarget executionTarget) {
        testing.getSuites().register(suiteName, JvmTestSuite.class, suite -> {
            QuarkusApplicationJvmTestSuite quarkusSuite = quarkusSuite(suite);
            quarkusSuite.forQuarkusIntegrationTests(buildName);
            quarkusSuite.startupArchiveTraining(training -> training.getExecutionTarget().set(executionTarget));
        });
    }

    private static org.gradle.api.tasks.testing.Test testTask(Project project, String name) {
        return (org.gradle.api.tasks.testing.Test) project.getTasks().getByName(name);
    }

    private static QuarkusApplicationStartupArchiveTrainingMetadataTask trainingMetadataTask(Project project,
            String name) {
        return (QuarkusApplicationStartupArchiveTrainingMetadataTask) project.getTasks().getByName(name);
    }

    private static QuarkusApplicationStartupArchiveValidationTask validationTask(Project project, String prefix) {
        return (QuarkusApplicationStartupArchiveValidationTask) project.getTasks()
                .getByName(prefix + "StartupArchiveValidation");
    }

    private static List<String> taskDependencyNames(Task task) {
        return task.getTaskDependencies().getDependencies(task).stream()
                .map(Task::getName)
                .toList();
    }
}
