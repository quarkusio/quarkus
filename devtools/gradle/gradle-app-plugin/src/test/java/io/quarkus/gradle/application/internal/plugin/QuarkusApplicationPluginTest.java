package io.quarkus.gradle.application.internal.plugin;

import static io.quarkus.gradle.testing.BaseGradleTest.canonicalPath;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.attributes.java.TargetJvmEnvironment;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testfixtures.internal.ProjectBuilderImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.application.QuarkusApplicationPlugin;
import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.internal.modelgen.GenerateModelTask;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.model.QuarkusApplicationDevDebugMode;
import io.quarkus.gradle.application.model.QuarkusApplicationImageBuilder;
import io.quarkus.gradle.application.model.QuarkusApplicationLaunchKind;
import io.quarkus.gradle.application.model.QuarkusApplicationVariantAttributes;
import io.quarkus.gradle.application.tasks.QuarkusApplicationContinuousTestTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationDeployTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationDevTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationGenerateCodeTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationImageBuildTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationImagePushTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationPackageTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationRemoteDevTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationRunTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationShowModelTask;
import io.quarkus.gradle.model.pom.DeclaredDependencyEnrichmentMode;
import io.quarkus.gradle.model.tasks.GeneratePomClosureTask;
import io.quarkus.runtime.LaunchMode;

class QuarkusApplicationPluginTest {

    @TempDir
    Path testProjectDir;

    private Project project;

    /**
     * Cleanup functionality for Windows to let the automatic deletion of {@link #testProjectDir} succeed
     * and not fail with {@code org.junit.jupiter.api.io.TempDirDeletionStrategy$DeletionException}.
     * {@link ProjectBuilder} created {@link Project}s have a
     * <a href=
     * "https://github.com/gradle/gradle/blob/3f750f03d77e42327c5f9fcb9992110088330a32/platforms/extensibility/unit-test-fixtures/src/main/java/org/gradle/testfixtures/internal/ProjectBuilderImpl.java#L356-L358">simplified
     * lifecycle</a>
     * without an automatic <a href=
     * "https://github.com/gradle/gradle/blob/3f750f03d77e42327c5f9fcb9992110088330a32/platforms/extensibility/unit-test-fixtures/src/main/java/org/gradle/testfixtures/internal/ProjectBuilderImpl.java#L211-L218">tear-down</a>.
     */
    @AfterEach
    void cleanupForWindows() {
        if (project != null) {
            ProjectBuilderImpl.stop(project);
        }
    }

    @Test
    void mirrorsLatePlatformDependenciesThroughDeclarableInternalScope() {
        project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("java");
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);

        var platformDependency = project.getDependencies().platform("org.acme:application-platform:1.0");
        project.getDependencies().add("implementation", platformDependency);

        Configuration platformDeclarations = project.getConfigurations()
                .getByName("quarkusApplicationPlatformDeclarations");
        assertThat(platformDeclarations.isCanBeDeclared()).isTrue();
        assertThat(platformDeclarations.isCanBeResolved()).isFalse();
        assertThat(platformDeclarations.isCanBeConsumed()).isFalse();
        assertThat(platformDeclarations.getDependencies()).containsExactly(platformDependency);
        Configuration platformClasspath = project.getConfigurations()
                .getByName("quarkusApplicationPlatformConfiguration");
        assertThat(platformClasspath.isCanBeDeclared()).isFalse();
        assertThat(platformClasspath.isCanBeResolved()).isTrue();
        assertThat(platformClasspath.isCanBeConsumed()).isFalse();
        assertThat(platformClasspath.getExtendsFrom()).containsExactly(platformDeclarations);

        project.getConfigurations().getByName("implementation").getDependencies().remove(platformDependency);
        assertThat(platformDeclarations.getDependencies()).isEmpty();
    }

    @Test
    void registersRootDevelopmentConfigurationAndTasks() throws Exception {
        PluginFixture fixture = createProject("root-registration");
        Project project = fixture.project();
        Path effectiveProjectDir = fixture.effectiveProjectDir();
        int currentJavaVersion = fixture.currentJavaVersion();
        QuarkusApplicationExtension extension = fixture.extension();
        Configuration developmentDependencies = project.getConfigurations().getByName("quarkusDev");
        assertThat(developmentDependencies.isCanBeDeclared()).isTrue();
        assertThat(developmentDependencies.isCanBeResolved()).isFalse();
        assertThat(developmentDependencies.isCanBeConsumed()).isFalse();
        assertThat(developmentDependencies.getDescription())
                .isEqualTo("Declares dependencies used only for Quarkus development.");
        Configuration developmentClasspath = project.getConfigurations()
                .getByName("quarkusApplicationDevBaseRuntimeClasspathConfiguration");
        assertThat(developmentClasspath.isCanBeDeclared()).isFalse();
        assertThat(developmentClasspath.isCanBeResolved()).isTrue();
        assertThat(developmentClasspath.isCanBeConsumed()).isFalse();
        assertThat(developmentClasspath.getExtendsFrom()).contains(developmentDependencies);
        assertThat(project.getConfigurations()
                .getByName("quarkusApplicationDevRuntimeClasspathConfiguration")
                .getExtendsFrom())
                .contains(developmentClasspath);
        assertThat(extension.getDev().getWorkingDirectory().get().getAsFile()).isEqualTo(effectiveProjectDir.toFile());
        assertThat(extension.getDev().getEnvironmentVariables().get()).isEmpty();
        assertThat(extension.getDev().getDebug().isPresent()).isFalse();
        assertThat(extension.getDev().getDebugMode().isPresent()).isFalse();
        assertThat(extension.getDev().getDebugHost().isPresent()).isFalse();
        assertThat(extension.getDev().getDebugPort().isPresent()).isFalse();
        assertThat(extension.getDev().getSuspend().isPresent()).isFalse();
        assertThat(extension.getDev().getForceC2().isPresent()).isFalse();
        assertThat(extension.getDev().getExtensionJvmOptions().getDisableAll().get()).isFalse();
        assertThat(extension.getDev().getExtensionJvmOptions().getDisableFor().get()).isEmpty();
        QuarkusApplicationDevTask dev = (QuarkusApplicationDevTask) project.getTasks().getByName("quarkusApplicationDev");
        QuarkusApplicationContinuousTestTask continuousTest = (QuarkusApplicationContinuousTestTask) project.getTasks()
                .getByName("quarkusApplicationContinuousTest");
        Task devReplayTrigger = project.getTasks().getByName("quarkusApplicationDevInitializeReplayTrigger");
        Task continuousTestReplayTrigger = project.getTasks()
                .getByName("quarkusApplicationContinuousTestInitializeReplayTrigger");
        assertDefaultDevLaunchConfiguration(dev, effectiveProjectDir);
        assertDefaultDevLaunchConfiguration(continuousTest, effectiveProjectDir);
        assertThat(dev.getReplayTriggerFile().get().getAsFile().toPath())
                .isEqualTo(effectiveProjectDir.resolve("build/quarkus-dev/live-reload-replay.trigger"));
        assertThat(continuousTest.getReplayTriggerFile().get().getAsFile().toPath())
                .isEqualTo(effectiveProjectDir.resolve("build/quarkus-continuous-test/live-reload-replay.trigger"));
        assertThat(dev.getTaskDependencies().getDependencies(dev))
                .extracting(Task::getName)
                .contains(devReplayTrigger.getName());
        assertThat(continuousTest.getTaskDependencies().getDependencies(continuousTest))
                .extracting(Task::getName)
                .contains(continuousTestReplayTrigger.getName());
        assertThat(dev.getJavaLauncher().get().getMetadata().getLanguageVersion().asInt()).isEqualTo(currentJavaVersion);
        assertThat(continuousTest.getJavaLauncher().get().getMetadata().getLanguageVersion().asInt())
                .isEqualTo(currentJavaVersion);
    }

    @Test
    void registersApplicationModelAndCodegenTasks() throws Exception {
        PluginFixture fixture = createProject("model-codegen-registration");
        Project project = fixture.project();
        Path projectDir = fixture.projectDir();
        Path effectiveProjectDir = fixture.effectiveProjectDir();
        Path mainSources = fixture.mainSources();
        Path testSources = fixture.testSources();
        Path normalRuntime = fixture.normalRuntime();
        Path testRuntime = fixture.testRuntime();
        Path devRuntime = fixture.devRuntime();
        QuarkusApplicationExtension extension = fixture.extension();
        assertThat(extension.getCodegen().getProviders().get()).containsExactly("grpc", "avdl", "avpr", "avsc");
        assertThat(extension.getCodegen().getInputNames().get()).containsExactly("proto", "avro");
        configureProject(fixture);
        QuarkusApplicationDevTask dev = (QuarkusApplicationDevTask) project.getTasks()
                .getByName("quarkusApplicationDev");
        QuarkusApplicationContinuousTestTask continuousTest = (QuarkusApplicationContinuousTestTask) project.getTasks()
                .getByName("quarkusApplicationContinuousTest");
        GenerateModelTask applicationModel = (GenerateModelTask) project.getTasks()
                .getByName("quarkusApplicationModel");
        GenerateModelTask devApplicationModel = (GenerateModelTask) project.getTasks()
                .getByName("quarkusApplicationDevModel");
        GenerateModelTask codegenModel = (GenerateModelTask) project.getTasks()
                .getByName("quarkusApplicationCodegenModel");
        GenerateModelTask devCodegenModel = (GenerateModelTask) project.getTasks()
                .getByName("quarkusApplicationDevCodegenModel");
        GenerateModelTask testCodegenModel = (GenerateModelTask) project.getTasks()
                .getByName("quarkusApplicationTestCodegenModel");
        GenerateModelTask testApplicationModel = (GenerateModelTask) project.getTasks()
                .getByName("quarkusApplicationTestModel");
        GenerateModelTask continuousTestApplicationModel = (GenerateModelTask) project.getTasks()
                .getByName("quarkusApplicationContinuousTestModel");
        QuarkusApplicationShowModelTask showApplicationModel = (QuarkusApplicationShowModelTask) project.getTasks()
                .getByName("quarkusApplicationShowModel");
        QuarkusApplicationShowModelTask showDevApplicationModel = (QuarkusApplicationShowModelTask) project.getTasks()
                .getByName("quarkusApplicationShowDevModel");
        QuarkusApplicationShowModelTask showTestApplicationModel = (QuarkusApplicationShowModelTask) project.getTasks()
                .getByName("quarkusApplicationShowTestModel");
        QuarkusApplicationGenerateCodeTask generateCode = (QuarkusApplicationGenerateCodeTask) project.getTasks()
                .getByName("quarkusApplicationGenerateCode");
        QuarkusApplicationGenerateCodeTask generateDevCode = (QuarkusApplicationGenerateCodeTask) project.getTasks()
                .getByName("quarkusApplicationGenerateDevCode");
        QuarkusApplicationGenerateCodeTask generateTestCode = (QuarkusApplicationGenerateCodeTask) project.getTasks()
                .getByName("quarkusApplicationGenerateTestCode");
        Task compileJava = project.getTasks().getByName("compileJava");
        Task compileTestJava = project.getTasks().getByName("compileTestJava");
        Task classes = project.getTasks().getByName("classes");
        Task testClasses = project.getTasks().getByName("testClasses");
        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet mainSourceSet = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        SourceSet testSourceSet = java.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
        assertThat(applicationModel.getTaskDependencies().getDependencies(applicationModel))
                .extracting(Task::getName)
                .contains("classes");
        assertThat(testApplicationModel.getTaskDependencies().getDependencies(testApplicationModel))
                .extracting(Task::getName)
                .contains("classes");
        assertThat(continuousTestApplicationModel.getTaskDependencies().getDependencies(continuousTestApplicationModel))
                .extracting(Task::getName)
                .contains("classes");
        assertThat(codegenModel.getTaskDependencies().getDependencies(codegenModel)).isEmpty();
        assertThat(testCodegenModel.getTaskDependencies().getDependencies(testCodegenModel)).isEmpty();
        assertThat(applicationModel.getLaunchMode().get()).isEqualTo(LaunchMode.NORMAL);
        assertThat(devApplicationModel.getLaunchMode().get()).isEqualTo(LaunchMode.DEVELOPMENT);
        assertThat(codegenModel.getLaunchMode().get()).isEqualTo(LaunchMode.NORMAL);
        assertThat(devCodegenModel.getLaunchMode().get()).isEqualTo(LaunchMode.DEVELOPMENT);
        assertThat(testCodegenModel.getLaunchMode().get()).isEqualTo(LaunchMode.TEST);
        assertThat(testApplicationModel.getLaunchMode().get()).isEqualTo(LaunchMode.TEST);
        assertThat(continuousTestApplicationModel.getLaunchMode().get()).isEqualTo(LaunchMode.TEST);
        assertThat(applicationModel.getDeclaredDependencyEnrichmentMode().get())
                .isEqualTo(DeclaredDependencyEnrichmentMode.SELECTED_MODULE_POMS);
        assertThat(devApplicationModel.getDeclaredDependencyEnrichmentMode().get())
                .isEqualTo(DeclaredDependencyEnrichmentMode.NONE);
        assertThat(codegenModel.getDeclaredDependencyEnrichmentMode().get())
                .isEqualTo(DeclaredDependencyEnrichmentMode.NONE);
        assertThat(devCodegenModel.getDeclaredDependencyEnrichmentMode().get())
                .isEqualTo(DeclaredDependencyEnrichmentMode.NONE);
        assertThat(testCodegenModel.getDeclaredDependencyEnrichmentMode().get())
                .isEqualTo(DeclaredDependencyEnrichmentMode.NONE);
        assertThat(testApplicationModel.getDeclaredDependencyEnrichmentMode().get())
                .isEqualTo(DeclaredDependencyEnrichmentMode.NONE);
        assertThat(continuousTestApplicationModel.getDeclaredDependencyEnrichmentMode().get())
                .isEqualTo(DeclaredDependencyEnrichmentMode.NONE);
        assertThat(applicationModel.getApplicationModel().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus/application-model/quarkus-application-model.dat").get().getAsFile());
        GeneratePomClosureTask pomClosure = (GeneratePomClosureTask) project.getTasks()
                .getByName("quarkusApplicationModelPomClosure");
        assertThat(applicationModel.getPomClosureFile().get().getAsFile())
                .isEqualTo(pomClosure.getPomClosureFile().get().getAsFile());
        assertThat(pomClosure.getPomClosureFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus/application-model/pom-closure/quarkusApplicationModel.properties").get().getAsFile());
        Configuration selectedPoms = project.getConfigurations()
                .getByName("quarkusApplicationModelPomArtifacts");
        assertThat(selectedPoms.isCanBeResolved()).isTrue();
        assertThat(selectedPoms.isCanBeConsumed()).isFalse();
        assertThat(selectedPoms.isTransitive()).isFalse();
        assertThat(devApplicationModel.getPomClosureFile().isPresent()).isFalse();
        assertThat(codegenModel.getPomClosureFile().isPresent()).isFalse();
        assertThat(devCodegenModel.getPomClosureFile().isPresent()).isFalse();
        assertThat(testCodegenModel.getPomClosureFile().isPresent()).isFalse();
        assertThat(testApplicationModel.getPomClosureFile().isPresent()).isFalse();
        assertThat(continuousTestApplicationModel.getPomClosureFile().isPresent()).isFalse();
        assertThat(codegenModel.getApplicationModel().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus/application-model/quarkus-application-codegen-model.dat").get().getAsFile());
        assertThat(devCodegenModel.getApplicationModel().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus/application-model/quarkus-application-dev-codegen-model.dat").get().getAsFile());
        assertThat(testCodegenModel.getApplicationModel().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus/application-model/quarkus-application-test-codegen-model.dat").get().getAsFile());
        assertThat(testApplicationModel.getApplicationModel().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus/application-model/quarkus-application-test-model.dat").get().getAsFile());
        assertThat(continuousTestApplicationModel.getApplicationModel().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus/application-model/quarkus-application-continuous-test-model.dat")
                        .get().getAsFile());
        assertModelDiagnosticsTask(project, showApplicationModel, applicationModel, "normal",
                "quarkus-application-model.txt");
        assertModelDiagnosticsTask(project, showDevApplicationModel, devApplicationModel, "development",
                "quarkus-application-dev-model.txt");
        assertModelDiagnosticsTask(project, showTestApplicationModel, testApplicationModel, "test",
                "quarkus-application-test-model.txt");
        assertThat(generateCode.getApplicationModel().get().getAsFile())
                .isEqualTo(codegenModel.getApplicationModel().get().getAsFile());
        assertThat(generateDevCode.getApplicationModel().get().getAsFile())
                .isEqualTo(devCodegenModel.getApplicationModel().get().getAsFile());
        assertThat(generateTestCode.getApplicationModel().get().getAsFile())
                .isEqualTo(testCodegenModel.getApplicationModel().get().getAsFile());
        assertThat(generateCode.getLaunchMode().get()).isEqualTo(LaunchMode.NORMAL);
        assertThat(generateDevCode.getLaunchMode().get()).isEqualTo(LaunchMode.DEVELOPMENT);
        assertThat(generateTestCode.getLaunchMode().get()).isEqualTo(LaunchMode.TEST);
        assertThat(generateCode.getGeneratedOutputDirectory().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory().dir("generated/sources/quarkus-application/main")
                        .get().getAsFile());
        assertThat(generateDevCode.getGeneratedOutputDirectory().get().getAsFile())
                .isEqualTo(generateCode.getGeneratedOutputDirectory().get().getAsFile());
        assertThat(generateTestCode.getGeneratedOutputDirectory().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory().dir("generated/sources/quarkus-application/test")
                        .get().getAsFile());
        assertThat(generateCode.getCodegenForkOptions().getJvmArgs().get()).containsExactly("-Dcodegen-option=true");
        assertThat(generateCode.getCodegenForkOptions().getSystemProperties().get())
                .containsEntry("codegen.system.property", "true");
        assertThat(generateCode.getCodegenForkOptions().getEnvironment().get()).containsEntry("CODEGEN_ENV", "true");
        assertThat(generateCode.getCodegenForkOptions().getMinHeapSize().get()).isEqualTo("128m");
        assertThat(generateCode.getCodegenForkOptions().getDefaultCharacterEncoding().get()).isEqualTo("UTF-8");
        assertThat(mainSourceSet.getJava().getSrcDirs())
                .doesNotContain(generateCode.getGeneratedOutputDirectory().get().getAsFile());
        assertThat(testSourceSet.getJava().getSrcDirs())
                .doesNotContain(generateTestCode.getGeneratedOutputDirectory().get().getAsFile());
        assertThat(compileJava.getTaskDependencies().getDependencies(compileJava))
                .extracting(Task::getName)
                .contains("quarkusApplicationGenerateCode");
        assertThat(compileJava.getMustRunAfter().getDependencies(compileJava))
                .extracting(Task::getName)
                .contains("quarkusApplicationGenerateDevCode");
        assertThat(compileTestJava.getTaskDependencies().getDependencies(compileTestJava))
                .extracting(Task::getName)
                .contains("quarkusApplicationGenerateCode", "quarkusApplicationGenerateTestCode");
        assertThat(generateTestCode.getMustRunAfter().getDependencies(generateTestCode))
                .extracting(Task::getName)
                .contains("quarkusApplicationGenerateCode", "quarkusApplicationGenerateDevCode");
        Path generatedMainSource = projectDir.resolve(
                "build/generated/sources/quarkus-application/main/custom-provider/org/acme/GeneratedMain.java");
        Path generatedTestSource = projectDir.resolve(
                "build/generated/sources/quarkus-application/test/custom-provider/org/acme/GeneratedTest.java");
        Files.createDirectories(generatedMainSource.getParent());
        Files.createDirectories(generatedTestSource.getParent());
        Files.writeString(generatedMainSource, "package org.acme; class GeneratedMain {}");
        Files.writeString(generatedTestSource, "package org.acme; class GeneratedTest {}");
        assertThat(((JavaCompile) compileJava).getSource().getFiles().stream()
                .map(File::toPath)
                .map(path -> canonicalPath(path)))
                .contains(canonicalPath(generatedMainSource));
        assertThat(((JavaCompile) compileTestJava).getSource().getFiles().stream()
                .map(File::toPath)
                .map(path -> canonicalPath(path)))
                .contains(canonicalPath(generatedTestSource));
        assertThat(classes.getTaskDependencies().getDependencies(classes))
                .extracting(Task::getName)
                .contains("compileJava");
        assertThat(testClasses.getTaskDependencies().getDependencies(testClasses))
                .extracting(Task::getName)
                .contains("compileTestJava");
        assertThat(applicationModel.getOriginalClasspath().getFiles())
                .contains(normalRuntime.toFile())
                .doesNotContain(testRuntime.toFile(), devRuntime.toFile());
        assertThat(devApplicationModel.getOriginalClasspath().getFiles())
                .contains(normalRuntime.toFile(), devRuntime.toFile())
                .doesNotContain(testRuntime.toFile());
        assertThat(codegenModel.getOriginalClasspath().getFiles())
                .contains(normalRuntime.toFile())
                .doesNotContain(testRuntime.toFile(), devRuntime.toFile());
        assertThat(devCodegenModel.getOriginalClasspath().getFiles())
                .contains(normalRuntime.toFile(), devRuntime.toFile())
                .doesNotContain(testRuntime.toFile());
        assertThat(testCodegenModel.getOriginalClasspath().getFiles())
                .contains(testRuntime.toFile())
                .doesNotContain(devRuntime.toFile());
        assertThat(testApplicationModel.getOriginalClasspath().getFiles())
                .contains(testRuntime.toFile())
                .doesNotContain(devRuntime.toFile());
        assertThat(continuousTestApplicationModel.getOriginalClasspath().getFiles())
                .contains(normalRuntime.toFile(), testRuntime.toFile(), devRuntime.toFile());
        assertThat(testApplicationModel.getApplicationClassesDirectories().getFiles())
                .containsAll(mainSourceSet.getOutput().getClassesDirs().getFiles());
        assertThat(testApplicationModel.getApplicationResourceSourceDirectoryPaths().get())
                .containsExactlyInAnyOrderElementsOf(
                        mainSourceSet.getResources().getSrcDirs().stream().map(File::getAbsolutePath).toList());
        assertThat(generateCode.getClasspath().getFiles())
                .contains(normalRuntime.toFile())
                .doesNotContain(testRuntime.toFile(), devRuntime.toFile());
        assertThat(generateDevCode.getClasspath().getFiles())
                .contains(normalRuntime.toFile(), devRuntime.toFile())
                .doesNotContain(testRuntime.toFile());
        assertThat(generateTestCode.getClasspath().getFiles())
                .contains(testRuntime.toFile())
                .doesNotContain(devRuntime.toFile());
        assertThat(generateCode.getCodegenDependencyArtifacts().getFiles())
                .contains(normalRuntime.toFile())
                .doesNotContain(testRuntime.toFile());
        assertThat(generateTestCode.getCodegenDependencyArtifacts().getFiles()).contains(testRuntime.toFile());
        assertThat(dev.getTaskDependencies().getDependencies(dev))
                .extracting(Task::getName)
                .contains("quarkusApplicationDevModel", "quarkusApplicationGenerateDevCode");
        assertThat(dev.getTestApplicationModel().get().getAsFile())
                .isEqualTo(continuousTestApplicationModel.getApplicationModel().get().getAsFile());
        assertThat(continuousTest.getTaskDependencies().getDependencies(continuousTest))
                .extracting(Task::getName)
                .contains("quarkusApplicationContinuousTestModel", "quarkusApplicationGenerateDevCode");
        assertThat(continuousTest.getApplicationModel().get().getAsFile())
                .isEqualTo(continuousTestApplicationModel.getApplicationModel().get().getAsFile());
        assertThat(continuousTest.getTestApplicationModel().get().getAsFile())
                .isEqualTo(continuousTestApplicationModel.getApplicationModel().get().getAsFile());
        assertThat(normalizedPaths(generateCode.getSourceParentDirectories().getFiles()))
                .contains(normalizedPath(mainSources));
        assertThat(normalizedPaths(generateTestCode.getSourceParentDirectories().getFiles()))
                .contains(normalizedPath(testSources));
        assertThat(normalizedPaths(generateCode.getConfigurationSourceDirectories().getFiles()))
                .contains(normalizedPath(effectiveProjectDir.resolve("src/main/resources")));
        assertThat(normalizedPaths(generateTestCode.getConfigurationSourceDirectories().getFiles()))
                .contains(normalizedPath(effectiveProjectDir.resolve("src/main/resources")));
        assertThat(extension.getCodegen().getProviders().get()).containsExactly("custom-provider", "other-provider");
        assertThat(extension.getCodegen().getInputNames().get()).containsExactly("custom-input", "other-input");
        assertThat(generateCode.getCodegenProviders().get()).containsExactly("custom-provider", "other-provider");
        assertThat(generateCode.getCodegenInputNames().get()).containsExactly("custom-input", "other-input");
        assertThat(generateCode.getGradlePropertyNames().get()).containsExactly("quarkus.explicit.project");
        assertThat(generateTestCode.getCodegenProviders().get()).containsExactly("custom-provider", "other-provider");
        assertThat(generateTestCode.getCodegenInputNames().get()).containsExactly("custom-input", "other-input");
        assertThat(generateTestCode.getGradlePropertyNames().get()).containsExactly("quarkus.explicit.project");
        assertThat(generateTestCode.getCodegenForkOptions().getJvmArgs().get()).containsExactly("-Dcodegen-option=true");
        assertThat(generateTestCode.getCodegenForkOptions().getSystemProperties().get())
                .containsEntry("codegen.system.property", "true");
        assertThat(project.getTasks().findByName("quarkusGenerateCode")).isNull();
        assertThat(project.getTasks().findByName("quarkusGenerateCodeDev")).isNull();
        assertThat(project.getTasks().findByName("quarkusGenerateCodeTests")).isNull();
    }

    @Test
    void registersNamedPackageImageDeploymentAndVariants() throws Exception {
        PluginFixture fixture = createConfiguredProject("named-build-registration");
        Project project = fixture.project();
        QuarkusApplicationPackageTask build = (QuarkusApplicationPackageTask) project.getTasks()
                .getByName("quarkusAppBuild");
        assertThat(build.getApplicationModel().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus/application-model/quarkus-application-model.dat").get().getAsFile());
        assertThat(build.getBuildType().get()).isEqualTo(QuarkusApplicationBuildType.FAST_JAR);
        assertThat(build.getOutputDirectory().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory().dir("quarkus-builds/app/package").get().getAsFile());
        assertThat(build.getOutputName().get()).isEqualTo("test-1.2.3");
        assertThat(build.getAdditionalDescriptorShapeProperties().get()).isEmpty();
        assertThat(build.getBuildForkOptions().getJvmArgs().get()).containsExactly("-Dbuild-option=true");
        assertThat(build.getBuildForkOptions().getSystemProperties().get())
                .containsEntry("build.system.property", "true");
        assertThat(build.getBuildForkOptions().getEnvironment().get()).containsEntry("BUILD_ENV", "true");
        assertThat(build.getBuildForkOptions().getMaxHeapSize().get()).isEqualTo("768m");
        assertThat(build.getBuildForkOptions().getEnableAssertions().get()).isTrue();
        assertThat(build.getQuarkusBuildProperties().get())
                .containsEntry("common", "value")
                .containsEntry("build", "value")
                .doesNotContainKey("image");
        assertThat(build.getGradlePropertyNames().get()).containsExactly("quarkus.explicit.project");
        QuarkusApplicationImageBuildTask imageBuild = (QuarkusApplicationImageBuildTask) project.getTasks()
                .getByName("quarkusAppImageBuild");
        assertThat(imageBuild.getImageTag().isPresent()).isFalse();
        assertThat(imageBuild.getImageBuilder().get()).isEqualTo(QuarkusApplicationImageBuilder.JIB);
        assertThat(imageBuild.getQuarkusBuildProperties().get())
                .containsEntry("common", "value")
                .containsEntry("build", "value")
                .containsEntry("image", "value");
        assertThat(imageBuild.getOutputDirectory().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory().dir("quarkus-builds/app/image-build").get().getAsFile());
        assertThat(imageBuild.getTaskDependencies().getDependencies(imageBuild))
                .extracting(Task::getName)
                .doesNotContain("quarkusAppBuild");

        QuarkusApplicationImagePushTask imagePush = (QuarkusApplicationImagePushTask) project.getTasks()
                .getByName("quarkusAppImagePush");
        assertThat(imagePush)
                .isInstanceOf(QuarkusApplicationImagePushTask.class);
        assertThat(imagePush.getOutputDirectory().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory().dir("quarkus-builds/app/image-push").get().getAsFile());
        assertThat(imagePush.getTaskDependencies().getDependencies(imagePush))
                .extracting(Task::getName)
                .doesNotContain("quarkusAppBuild");

        assertJavaRuntimeAttributes(project, "quarkusApplicationRuntimeClasspathConfiguration");
        assertJavaRuntimeAttributes(project, "quarkusApplicationTestRuntimeClasspathConfiguration");
        assertJavaRuntimeAttributes(project, "quarkusApplicationConditionalRuntimeClasspathConfiguration");
        assertJavaRuntimeAttributes(project, "quarkusApplicationTestConditionalRuntimeClasspathConfiguration");
        assertJavaRuntimeAttributes(project, "quarkusApplicationDeploymentClasspathConfiguration");
        assertJavaRuntimeAttributes(project, "quarkusApplicationTestDeploymentClasspathConfiguration");
        assertJavaRuntimeAttributes(project, "quarkusApplicationCompileOnlyConfiguration");
        assertJavaRuntimeAttributes(project, "quarkusApplicationTestCompileOnlyConfiguration");
        assertPackageElementsVariant(project, "quarkusAppPackageElements", "app", "fast-jar", "quarkusAppBuild",
                QuarkusApplicationVariantAttributes.PACKAGE_CATEGORY,
                QuarkusApplicationVariantAttributes.PACKAGE_LIBRARY_ELEMENTS,
                ArtifactTypeDefinition.DIRECTORY_TYPE);
        assertPackageElementsVariant(project, "quarkusAppLauncherJarElements", "app", "fast-jar", "quarkusAppBuild",
                QuarkusApplicationVariantAttributes.LAUNCHER_CATEGORY,
                QuarkusApplicationVariantAttributes.LAUNCHER_LIBRARY_ELEMENTS,
                ArtifactTypeDefinition.JAR_TYPE);

        assertThat(project.getTasks().getByName("quarkusAppDeployToDev"))
                .isInstanceOf(QuarkusApplicationDeployTask.class);
        QuarkusApplicationRunTask run = (QuarkusApplicationRunTask) project.getTasks().getByName("quarkusAppRun");
        assertThat(run.getTaskDependencies().getDependencies(run))
                .extracting(Task::getName)
                .contains("quarkusAppBuild");
        assertThat(run.getPackageResultFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus-build-results/app/package/package-result.properties").get().getAsFile());
    }

    @Test
    void wiresDevRemoteDevAndContinuousTestTasks() throws Exception {
        PluginFixture fixture = createConfiguredProject("dev-registration");
        Project project = fixture.project();
        Path effectiveProjectDir = fixture.effectiveProjectDir();
        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet mainSourceSet = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        QuarkusApplicationDevTask dev = (QuarkusApplicationDevTask) project.getTasks()
                .getByName("quarkusApplicationDev");
        QuarkusApplicationContinuousTestTask continuousTest = (QuarkusApplicationContinuousTestTask) project.getTasks()
                .getByName("quarkusApplicationContinuousTest");
        assertThat(dev.getContinuousBuild().get()).isFalse();
        assertThat(dev.getTaskDependencies().getDependencies(dev))
                .extracting(Task::getName)
                .contains("classes");
        assertThat(dev.getQuarkusBuildProperties().get())
                .containsEntry("common", "value")
                .containsEntry("dev", "value")
                .doesNotContainKey("build")
                .doesNotContainKey("image");
        assertThat(dev.getDevJvmArgs().get()).containsExactly("-Ddev-jvm-arg=true");
        assertThat(dev.getJvmArguments().get()).isEmpty();
        assertThat(dev.getApplicationArguments().get()).isEmpty();
        assertThat(dev.getModules().get()).isEmpty();
        assertThat(dev.getOpenJavaLang().get()).isFalse();
        assertThat(dev.getCompilerArguments().get()).isEmpty();
        assertThat(dev.getTests().get()).isEmpty();
        assertThat(dev.getDevSystemProperties().get()).containsEntry("dev.system.property", "true");
        assertDevLaunchConfiguration(dev, effectiveProjectDir.resolve("dev-work"));
        assertThat(dev.getApplicationClasses().getFiles()).containsAll(mainSourceSet.getOutput().getClassesDirs().getFiles());
        assertThat(dev.getApplicationResources().getFiles()).contains(mainSourceSet.getOutput().getResourcesDir());
        assertThat(dev.getReceiptFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus-dev/dev-iteration.properties").get().getAsFile());
        QuarkusApplicationPackageTask remoteDevBuild = (QuarkusApplicationPackageTask) project.getTasks()
                .getByName("quarkusApplicationRemoteDevBuild");
        assertThat(remoteDevBuild.getBuildName().get()).isEqualTo("remoteDev");
        assertThat(remoteDevBuild.getBuildType().get()).isEqualTo(QuarkusApplicationBuildType.MUTABLE_JAR);
        assertThat(remoteDevBuild.getOutputName().get()).isEqualTo(project.getName() + "-1.2.3");
        assertThat(remoteDevBuild.getOutputDirectory().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory().dir("quarkus-remote-dev/build").get().getAsFile());
        assertThat(remoteDevBuild.getPackageResultFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus-remote-dev/build-result/package-result.properties").get().getAsFile());
        assertThat(remoteDevBuild.getQuarkusBuildProperties().get())
                .containsEntry("common", "value")
                .containsEntry("remote-dev", "value")
                .doesNotContainKey("dev")
                .doesNotContainKey("build")
                .doesNotContainKey("image");
        assertThat(remoteDevBuild.getBuildForkOptions().getJvmArgs().get())
                .containsExactly("-Dbuild-option=true", "-Dremote-dev-jvm-arg=true");
        assertThat(remoteDevBuild.getBuildForkOptions().getSystemProperties().get())
                .containsEntry("build.system.property", "true")
                .containsEntry("remote.dev.system.property", "true");
        assertThat(project.getConfigurations().findByName("quarkusRemoteDevPackageElements")).isNull();
        assertThat(project.getConfigurations().findByName("quarkusRemoteDevLauncherJarElements")).isNull();
        QuarkusApplicationRemoteDevTask remoteDev = (QuarkusApplicationRemoteDevTask) project.getTasks()
                .getByName("quarkusApplicationRemoteDev");
        Task remoteDevReconnectTrigger = project.getTasks()
                .getByName("quarkusApplicationRemoteDevInitializeReconnectTrigger");
        assertThat(remoteDev.getTaskDependencies().getDependencies(remoteDev))
                .extracting(Task::getName)
                .contains("quarkusApplicationRemoteDevBuild", remoteDevReconnectTrigger.getName());
        assertThat(remoteDev.getBuildName().get()).isEqualTo("remoteDev");
        assertThat(remoteDev.getBuildType().get()).isEqualTo(QuarkusApplicationBuildType.MUTABLE_JAR);
        assertThat(remoteDev.getOutputName().get()).isEqualTo(project.getName() + "-1.2.3");
        assertThat(remoteDev.getPackageResultFile().get().getAsFile())
                .isEqualTo(remoteDevBuild.getPackageResultFile().get().getAsFile());
        assertThat(remoteDev.getPackageOutputDirectory().get().getAsFile())
                .isEqualTo(remoteDevBuild.getOutputDirectory().get().getAsFile());
        assertThat(remoteDev.getReceiptFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus-remote-dev/build-result/remote-dev-result.properties").get().getAsFile());
        assertThat(remoteDev.getPackageSnapshotFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus-remote-dev/snapshot/package-snapshot.tsv").get().getAsFile());
        assertThat(remoteDev.getCloseReceiptFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus-remote-dev/snapshot/session-closed.txt").get().getAsFile());
        assertThat(remoteDev.getReconnectTriggerFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("quarkus-remote-dev/reconnect/reconnect.trigger").get().getAsFile());
        assertThat(QuarkusApplicationRemoteDevTask.class.getMethod("getReconnectTriggerFile")
                .getAnnotation(org.gradle.api.tasks.PathSensitive.class).value())
                .isEqualTo(org.gradle.api.tasks.PathSensitivity.NONE);
        assertThat(continuousTest.getContinuousTesting().get()).isTrue();
        assertThat(continuousTest.getLaunchKind().get()).isEqualTo(QuarkusApplicationLaunchKind.CONTINUOUS_TEST);
        assertDevLaunchConfiguration(continuousTest, effectiveProjectDir.resolve("dev-work"));
        assertThat(project.getTasks().findByName("quarkusAppContinuousTest")).isNull();
        assertQuarkusTestTaskConfigured((org.gradle.api.tasks.testing.Test) project.getTasks().getByName("test"));
    }

    private PluginFixture createConfiguredProject(String projectDirectoryName) throws IOException {
        PluginFixture fixture = createProject(projectDirectoryName);
        configureProject(fixture);
        return fixture;
    }

    private PluginFixture createProject(String projectDirectoryName) throws IOException {
        Path projectDir = Files.createDirectory(testProjectDir.resolve(projectDirectoryName));
        Files.createDirectories(projectDir.resolve("src/main"));
        Files.createDirectories(projectDir.resolve("src/test"));
        Files.createDirectories(projectDir.resolve("src/main/java"));
        Files.createDirectories(projectDir.resolve("src/test/java"));
        Path normalRuntime = createJar(projectDir.resolve("normal-runtime.jar"));
        Path testRuntime = createJar(projectDir.resolve("test-runtime.jar"));
        project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();
        Path effectiveProjectDir = project.getLayout().getProjectDirectory().getAsFile().toPath();
        Path mainSources = effectiveProjectDir.resolve("src/main");
        Path testSources = effectiveProjectDir.resolve("src/test");
        project.setVersion("1.2.3");
        project.getPluginManager().apply("java");
        int currentJavaVersion = Runtime.version().feature();
        project.getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion()
                .set(JavaLanguageVersion.of(currentJavaVersion));
        project.getDependencies().add("runtimeOnly", project.files(normalRuntime));
        project.getDependencies().add("testRuntimeOnly", project.files(testRuntime));
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        Path devRuntime = createJar(projectDir.resolve("dev-runtime.jar"));
        project.getDependencies().add("quarkusDev", project.files(devRuntime));

        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        return new PluginFixture(project, projectDir, effectiveProjectDir, mainSources, testSources,
                normalRuntime, testRuntime, devRuntime, currentJavaVersion, extension);
    }

    private static void configureProject(PluginFixture fixture) {
        Project project = fixture.project();
        QuarkusApplicationExtension extension = fixture.extension();
        extension.getQuarkusBuildProperties().put("common", "value");
        extension.codegen(codegen -> {
            codegen.getProviders().set(Arrays.asList("custom-provider", "other-provider"));
            codegen.getInputNames().set(Arrays.asList("custom-input", "other-input"));
        });
        extension.buildForkOptions(options -> {
            options.jvmArgs("-Dbuild-option=true");
            options.systemProperty("build.system.property", "true");
            options.environment("BUILD_ENV", "true");
            options.getMaxHeapSize().set("768m");
            options.getEnableAssertions().set(true);
        });
        extension.codeGenForkOptions(options -> {
            options.jvmArgs("-Dcodegen-option=true");
            options.systemProperty("codegen.system.property", "true");
            options.environment("CODEGEN_ENV", "true");
            options.getMinHeapSize().set("128m");
            options.getDefaultCharacterEncoding().set("UTF-8");
        });
        extension.dev(devDsl -> {
            devDsl.getQuarkusBuildProperties().put("dev", "value");
            devDsl.getWorkingDirectory().set(project.getLayout().getProjectDirectory().dir("dev-work"));
            devDsl.getEnvironmentVariables().put("APP_MODE", "local");
            devDsl.getDebug().set(true);
            devDsl.getDebugMode().set(QuarkusApplicationDevDebugMode.CONNECT);
            devDsl.getDebugHost().set("127.0.0.1");
            devDsl.getDebugPort().set(5006);
            devDsl.getSuspend().set(false);
            devDsl.getForceC2().set(false);
            devDsl.extensionJvmOptions(options -> {
                options.getDisableAll().set(true);
                options.getDisableFor().add("org.acme:acme-extension");
            });
            devDsl.forkOptions(forkOptions -> {
                forkOptions.jvmArgs("-Ddev-jvm-arg=true");
                forkOptions.systemProperty("dev.system.property", "true");
            });
        });
        extension.remoteDev(remoteDev -> {
            remoteDev.getQuarkusBuildProperties().put("remote-dev", "value");
            remoteDev.forkOptions(forkOptions -> {
                forkOptions.jvmArgs("-Dremote-dev-jvm-arg=true");
                forkOptions.systemProperty("remote.dev.system.property", "true");
            });
        });
        extension.configInputs(configInputs -> configInputs.projectProperties(
                projectProperties -> projectProperties.getNames().add("quarkus.explicit.project")));
        extension.builds(builds -> builds.fastJar("app", app -> {
            app.getQuarkusBuildProperties().put("build", "value");
            app.image(image -> {
                image.getRepository().set("example/app");
                image.getBuilder().set(QuarkusApplicationImageBuilder.JIB);
                image.getQuarkusBuildProperties().put("image", "value");
            });
            app.deployments(deployments -> deployments.kubernetes("dev",
                    deployment -> deployment.getImageSource().set(QuarkusApplicationDeploymentImageSource.NORMAL_IMAGE_PUSH)));
        }));
    }

    private record PluginFixture(Project project, Path projectDir, Path effectiveProjectDir, Path mainSources,
            Path testSources, Path normalRuntime, Path testRuntime, Path devRuntime, int currentJavaVersion,
            QuarkusApplicationExtension extension) {
    }

    private static void assertDefaultDevLaunchConfiguration(QuarkusApplicationDevTask task, Path projectDirectory) {
        assertThat(task.getWorkingDirectory().get().getAsFile().toPath()).isEqualTo(projectDirectory);
        assertThat(task.getEnvironmentVariables().get()).isEmpty();
        assertThat(task.getDebug().isPresent()).isFalse();
        assertThat(task.getDebugMode().isPresent()).isFalse();
        assertThat(task.getDebugHost().isPresent()).isFalse();
        assertThat(task.getDebugPort().isPresent()).isFalse();
        assertThat(task.getSuspend().isPresent()).isFalse();
        assertThat(task.getForceC2().isPresent()).isFalse();
        assertThat(task.getExtensionJvmOptions().getDisableAll().get()).isFalse();
        assertThat(task.getExtensionJvmOptions().getDisableFor().get()).isEmpty();
        assertThat(task.getJavaLauncher().isPresent()).isTrue();
        assertThat(task.getJavaLauncher().get().getExecutablePath().getAsFile()).isFile();
    }

    private static void assertDevLaunchConfiguration(QuarkusApplicationDevTask task, Path workingDirectory) {
        assertThat(task.getWorkingDirectory().get().getAsFile().toPath()).isEqualTo(workingDirectory);
        assertThat(task.getEnvironmentVariables().get()).containsExactlyEntriesOf(java.util.Map.of("APP_MODE", "local"));
        assertThat(task.getDebug().get()).isTrue();
        assertThat(task.getDebugMode().get()).isEqualTo(QuarkusApplicationDevDebugMode.CONNECT);
        assertThat(task.getDebugHost().get()).isEqualTo("127.0.0.1");
        assertThat(task.getDebugPort().get()).isEqualTo(5006);
        assertThat(task.getSuspend().get()).isFalse();
        assertThat(task.getForceC2().get()).isFalse();
        assertThat(task.getExtensionJvmOptions().getDisableAll().get()).isTrue();
        assertThat(task.getExtensionJvmOptions().getDisableFor().get())
                .containsExactly("org.acme:acme-extension");
        assertThat(task.getJavaLauncher().isPresent()).isTrue();
        assertThat(task.getJavaLauncher().get().getExecutablePath().getAsFile()).isFile();
    }

    private static void assertModelDiagnosticsTask(Project project, QuarkusApplicationShowModelTask task,
            GenerateModelTask modelTask, String modelName, String reportFileName) {
        assertThat(task.getModelName().get()).isEqualTo(modelName);
        assertThat(task.getApplicationModel().get().getAsFile())
                .isEqualTo(modelTask.getApplicationModel().get().getAsFile());
        assertThat(task.getTaskDependencies().getDependencies(task))
                .extracting(Task::getName)
                .contains(modelTask.getName());
        assertThat(task.getReportFile().get().getAsFile())
                .isEqualTo(project.getLayout().getBuildDirectory()
                        .file("reports/quarkus/application-model/" + reportFileName).get().getAsFile());
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

    private static void assertJavaRuntimeAttributes(Project project, String configurationName) {
        var attributes = project.getConfigurations().getByName(configurationName).getAttributes();
        assertThat(attributes.getAttribute(Category.CATEGORY_ATTRIBUTE).getName()).isEqualTo(Category.LIBRARY);
        assertThat(attributes.getAttribute(Usage.USAGE_ATTRIBUTE).getName()).isEqualTo(Usage.JAVA_RUNTIME);
        assertThat(attributes.getAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE).getName())
                .isEqualTo(LibraryElements.JAR);
        assertThat(attributes.getAttribute(Bundling.BUNDLING_ATTRIBUTE).getName()).isEqualTo(Bundling.EXTERNAL);
        assertThat(attributes.getAttribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE).getName())
                .isEqualTo(TargetJvmEnvironment.STANDARD_JVM);
    }

    private static void assertPackageElementsVariant(Project project, String configurationName, String buildName,
            String buildType, String builtByTaskName, String category, String libraryElements, String artifactType) {
        Configuration configuration = project.getConfigurations().getByName(configurationName);
        assertThat(configuration.isCanBeConsumed()).isTrue();
        assertThat(configuration.isCanBeResolved()).isFalse();
        assertThat(configuration.isCanBeDeclared()).isFalse();

        var attributes = configuration.getAttributes();
        assertThat(attributes.getAttribute(Category.CATEGORY_ATTRIBUTE).getName())
                .isEqualTo(category);
        assertThat(attributes.getAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE).getName())
                .isEqualTo(libraryElements);
        assertThat(attributes.getAttribute(Usage.USAGE_ATTRIBUTE)).isNull();
        assertThat(attributes.getAttribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE)).isNull();
        assertThat(attributes.getAttribute(Bundling.BUNDLING_ATTRIBUTE)).isNull();
        assertThat(attributes.getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE))
                .isEqualTo(artifactType);
        assertThat(attributes.getAttribute(QuarkusApplicationVariantAttributes.BUILD_NAME_ATTRIBUTE)).isEqualTo(buildName);
        assertThat(attributes.getAttribute(QuarkusApplicationVariantAttributes.BUILD_TYPE_ATTRIBUTE)).isEqualTo(buildType);
        assertThat(configuration.getOutgoing().getArtifacts()).hasSize(1);
        var artifact = configuration.getOutgoing().getArtifacts().iterator().next();
        assertThat(artifact.getType()).isEqualTo(artifactType);
        assertThat(artifact.getBuildDependencies()
                .getDependencies(null))
                .extracting(Task::getName)
                .containsExactly(builtByTaskName);
    }

    private static Path createJar(Path file) throws IOException {
        try (JarOutputStream ignored = new JarOutputStream(Files.newOutputStream(file))) {
            return file;
        }
    }

    private static Set<Path> normalizedPaths(Set<File> files) {
        return files.stream()
                .map(File::toPath)
                .map(QuarkusApplicationPluginTest::normalizedPath)
                .collect(Collectors.toSet());
    }

    private static Path normalizedPath(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
