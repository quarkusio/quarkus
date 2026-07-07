package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testing.base.TestingExtension;

import io.quarkus.gradle.application.QuarkusApplicationPlugin;
import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.dsl.QuarkusApplicationJvmTestSuite;
import io.quarkus.gradle.application.internal.nativeimage.NativeResult;
import io.quarkus.gradle.application.internal.nativeimage.NativeResultCodec;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationStartupArchiveTrainingExecutionTarget;

@SuppressWarnings("UnstableApiUsage")
class QuarkusApplicationNamedNativeTestRegistrationTest {

    @org.junit.jupiter.api.Test
    void nativeExecutablesOwnIndependentGradleTestSuitesOutsideLifecycleTasks() {
        Project project = applicationProject();
        QuarkusApplicationExtension extension = extension(project);

        extension.builds(builds -> {
            builds.nativeExecutable("first");
            builds.nativeExecutable("second");
        });

        TestingExtension testing = testing(project);
        assertThat(testing.getSuites().getNames())
                .contains("quarkusFirstNativeTest", "quarkusSecondNativeTest");
        Test first = testTask(project, "quarkusFirstNativeTest");
        Test second = testTask(project, "quarkusSecondNativeTest");
        seedNativeReceipt(project, "first");
        seedNativeReceipt(project, "second");
        assertThat(first.getGroup()).isEqualTo("verification");
        assertThat(first.getDescription()).isEqualTo("Runs tests against the 'first' native executable.");
        assertThat(taskDependencyNames(first)).contains("quarkusFirstBuild").doesNotContain("quarkusSecondBuild");
        assertThat(taskDependencyNames(second)).contains("quarkusSecondBuild").doesNotContain("quarkusFirstBuild");

        for (String lifecycleTask : Set.of(
                JavaBasePlugin.CHECK_TASK_NAME,
                "build",
                BasePlugin.ASSEMBLE_TASK_NAME)) {
            assertThat(taskDependencyNames(project.getTasks().getByName(lifecycleTask)))
                    .doesNotContain("quarkusFirstNativeTest", "quarkusSecondNativeTest");
        }
    }

    @org.junit.jupiter.api.Test
    void nativeSourcesAndJvmBuildsDoNotOwnNativeTestSuitesOrLegacySurface() {
        Project project = applicationProject();
        extension(project).builds(builds -> {
            builds.fastJar("fast");
            builds.mutableJar("mutable");
            builds.legacyJar("legacy");
            builds.uberJar("uber");
            builds.nativeSources("sources");
        });

        TestingExtension testing = testing(project);
        assertThat(testing.getSuites().getNames())
                .doesNotContain(
                        "quarkusFastNativeTest",
                        "quarkusMutableNativeTest",
                        "quarkusLegacyNativeTest",
                        "quarkusUberNativeTest",
                        "quarkusSourcesNativeTest");
        assertThat(project.getTasks().findByName("testNative")).isNull();
        assertThat(project.getConfigurations().findByName("nativeTestImplementation")).isNull();
        assertThat(project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets()
                .findByName("native-test")).isNull();
    }

    @org.junit.jupiter.api.Test
    void generatedSuiteInheritsStandardTestDependenciesAndSharedGeneratedSources() {
        Project project = applicationProject();
        QuarkusApplicationExtension extension = extension(project);
        extension.getCodegen().getProviders().set(List.of("generated-provider"));
        extension.builds(builds -> builds.nativeExecutable("native"));

        JvmTestSuite nativeSuite = testing(project).getSuites()
                .withType(JvmTestSuite.class).getByName("quarkusNativeNativeTest");
        SourceSet nativeSources = nativeSuite.getSources();
        SourceSet standardTestSources = project.getExtensions().getByType(JavaPluginExtension.class)
                .getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);

        assertExtendsFrom(project, nativeSources.getImplementationConfigurationName(),
                standardTestSources.getImplementationConfigurationName());
        assertExtendsFrom(project, nativeSources.getCompileOnlyConfigurationName(),
                standardTestSources.getCompileOnlyConfigurationName());
        assertExtendsFrom(project, nativeSources.getRuntimeOnlyConfigurationName(),
                standardTestSources.getRuntimeOnlyConfigurationName());
        assertExtendsFrom(project, nativeSources.getAnnotationProcessorConfigurationName(),
                standardTestSources.getAnnotationProcessorConfigurationName());
        assertThat(project.getConfigurations().getByName(nativeSources.getImplementationConfigurationName())
                .getDependencies())
                .anyMatch(ProjectDependency.class::isInstance);
        createGeneratedSource(project, "generated-provider/Generated.java");
        JavaCompile compileJava = (JavaCompile) project.getTasks().getByName(nativeSources.getCompileJavaTaskName());
        assertThat(compileJava.getSource().getFiles())
                .extracting(File::toPath)
                .anyMatch(path -> path.endsWith(
                        "build/generated/sources/quarkus-application/test/generated-provider/Generated.java"));
        assertThat(taskDependencyNames(compileJava))
                .contains("quarkusApplicationGenerateTestCode");
    }

    @org.junit.jupiter.api.Test
    void includedSuiteOutputsFeedCompilationAndExecutionWithoutSelectingIncludedTestTask() {
        Project project = applicationProject();
        TestingExtension testing = testing(project);
        var included = testing.getSuites().register("integrationTest", JvmTestSuite.class);
        extension(project).builds(builds -> builds.nativeExecutable("native"));
        JvmTestSuite nativeSuite = testing.getSuites()
                .withType(JvmTestSuite.class).getByName("quarkusNativeNativeTest");

        quarkusSuite(nativeSuite).includeTestsFrom(included);
        quarkusSuite(nativeSuite).includeTestsFrom(included);
        seedNativeReceipt(project, "native");

        SourceSet includedSources = included.get().getSources();
        Test nativeTest = testTask(project, "quarkusNativeNativeTest");
        assertThat(nativeTest.getTestClassesDirs().getFiles())
                .containsAll(includedSources.getOutput().getClassesDirs().getFiles());
        assertThat(taskDependencyNames(project.getTasks()
                .getByName(nativeSuite.getSources().getCompileJavaTaskName())))
                .contains(includedSources.getCompileJavaTaskName(), includedSources.getClassesTaskName());
        assertThat(taskDependencyNames(nativeTest))
                .contains(includedSources.getCompileJavaTaskName(), includedSources.getClassesTaskName())
                .doesNotContain("integrationTest");
    }

    @org.junit.jupiter.api.Test
    void inclusionRejectsUnsupportedReceiverSelfGeneratedAndForeignSuites() {
        Project project = applicationProject();
        TestingExtension testing = testing(project);
        var userSuite = testing.getSuites().register("integrationTest", JvmTestSuite.class);
        extension(project).builds(builds -> {
            builds.nativeExecutable("first");
            builds.nativeExecutable("second");
        });
        JvmTestSuite first = testing.getSuites().withType(JvmTestSuite.class)
                .getByName("quarkusFirstNativeTest");
        var firstProvider = testing.getSuites().named("quarkusFirstNativeTest", JvmTestSuite.class);
        var secondProvider = testing.getSuites().named("quarkusSecondNativeTest", JvmTestSuite.class);

        assertThatThrownBy(() -> quarkusSuite(userSuite.get()).includeTestsFrom(userSuite))
                .hasMessageContaining("integrationTest")
                .hasMessageContaining("generated Quarkus named native-test suite");
        assertThatThrownBy(() -> quarkusSuite(first).includeTestsFrom(firstProvider))
                .hasMessageContaining("quarkusFirstNativeTest")
                .hasMessageContaining("cannot include itself");
        assertThatThrownBy(() -> quarkusSuite(first).includeTestsFrom(secondProvider))
                .hasMessageContaining("quarkusFirstNativeTest")
                .hasMessageContaining("quarkusSecondNativeTest")
                .hasMessageContaining("user-owned JVM test suite");

        Project foreignProject = ProjectBuilder.builder().withName("foreign").build();
        foreignProject.getPluginManager().apply("jvm-test-suite");
        var foreignSuite = testing(foreignProject).getSuites().register("foreignTest", JvmTestSuite.class);
        assertThatThrownBy(() -> quarkusSuite(first).includeTestsFrom(foreignSuite))
                .hasMessageContaining("quarkusFirstNativeTest")
                .hasMessageContaining("foreignTest")
                .hasMessageContaining("owned by project");
    }

    @org.junit.jupiter.api.Test
    void generatedSuiteRejectsPublicTestModeAndTrainingDsl() {
        Project project = applicationProject();
        extension(project).builds(builds -> builds.nativeExecutable("native"));
        QuarkusApplicationJvmTestSuite generated = quarkusSuite(testing(project).getSuites()
                .withType(JvmTestSuite.class).getByName("quarkusNativeNativeTest"));

        assertThatThrownBy(generated::forQuarkusTests)
                .hasMessageContaining("quarkusNativeNativeTest")
                .hasMessageContaining("forQuarkusTests()");
        assertThatThrownBy(() -> generated.forQuarkusIntegrationTests("native"))
                .hasMessageContaining("quarkusNativeNativeTest")
                .hasMessageContaining("forQuarkusIntegrationTests(...)");
        assertThatThrownBy(() -> generated.startupArchiveTraining(training -> training.getExecutionTarget()
                .set(QuarkusApplicationStartupArchiveTrainingExecutionTarget.HOST_JVM)))
                .hasMessageContaining("quarkusNativeNativeTest")
                .hasMessageContaining("startupArchiveTraining(...)");
    }

    private static Project applicationProject() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        return project;
    }

    private static QuarkusApplicationExtension extension(Project project) {
        return project.getExtensions().getByType(QuarkusApplicationExtension.class);
    }

    private static TestingExtension testing(Project project) {
        return project.getExtensions().getByType(TestingExtension.class);
    }

    private static QuarkusApplicationJvmTestSuite quarkusSuite(JvmTestSuite suite) {
        return ((ExtensionAware) suite).getExtensions().getByType(QuarkusApplicationJvmTestSuite.class);
    }

    private static Test testTask(Project project, String name) {
        return (Test) project.getTasks().getByName(name);
    }

    private static Set<String> taskDependencyNames(Task task) {
        return task.getTaskDependencies().getDependencies(task).stream()
                .map(Task::getName)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static void assertExtendsFrom(Project project, String configurationName, String parentName) {
        assertThat(project.getConfigurations().getByName(configurationName).getExtendsFrom())
                .extracting(configuration -> configuration.getName())
                .contains(parentName);
    }

    private static void seedNativeReceipt(Project project, String buildName) {
        try {
            Path outputRoot = project.getLayout().getBuildDirectory()
                    .dir("quarkus-builds/" + buildName + "/package").get().getAsFile().toPath();
            Files.createDirectories(outputRoot);
            Path executable = Files.createFile(outputRoot.resolve("application-runner"));
            Path receipt = project.getLayout().getBuildDirectory()
                    .file("quarkus-build-results/" + buildName + "/package/native-result.properties")
                    .get().getAsFile().toPath();
            new NativeResultCodec().write(receipt, new NativeResult(
                    buildName,
                    QuarkusApplicationBuildType.NATIVE_EXECUTABLE,
                    outputRoot,
                    "application",
                    Optional.of(executable),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Map.of(),
                    List.of()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void createGeneratedSource(Project project, String relativePath) {
        try {
            Path source = project.getLayout().getBuildDirectory()
                    .file("generated/sources/quarkus-application/test/" + relativePath)
                    .get().getAsFile().toPath();
            Files.createDirectories(source.getParent());
            Files.writeString(source, "class Generated {}");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
