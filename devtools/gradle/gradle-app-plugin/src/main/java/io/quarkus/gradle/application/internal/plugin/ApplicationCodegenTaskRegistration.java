package io.quarkus.gradle.application.internal.plugin;

import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.QUARKUS_APPLICATION_GROUP;
import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.configureConfigInputs;
import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.configureForkOptions;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;

import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.internal.modelgen.ClasspathBuilder;
import io.quarkus.gradle.application.internal.modelgen.GenerateModelTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationGenerateCodeTask;
import io.quarkus.runtime.LaunchMode;

final class ApplicationCodegenTaskRegistration {

    private static final String GENERATED_MAIN_SOURCES = "generated/sources/quarkus-application/main";
    private static final String GENERATED_TEST_SOURCES = "generated/sources/quarkus-application/test";

    private final TaskNameRegistry taskNames;

    ApplicationCodegenTaskRegistration(TaskNameRegistry taskNames) {
        this.taskNames = taskNames;
    }

    CodegenTasks register(Project project, QuarkusApplicationExtension extension, ClasspathBuilder classpath,
            TaskProvider<GenerateModelTask> codegenApplicationModel,
            TaskProvider<GenerateModelTask> devCodegenApplicationModel,
            TaskProvider<GenerateModelTask> testCodegenApplicationModel) {
        TaskProvider<QuarkusApplicationGenerateCodeTask> generateCode = registerGenerateCodeTask(project, extension,
                classpath, "quarkusApplicationGenerateCode", LaunchMode.NORMAL, SourceSet.MAIN_SOURCE_SET_NAME,
                codegenApplicationModel, GENERATED_MAIN_SOURCES,
                "Runs Quarkus code generators for main sources.");
        TaskProvider<QuarkusApplicationGenerateCodeTask> generateDevCode = registerGenerateCodeTask(project, extension,
                classpath, "quarkusApplicationGenerateDevCode", LaunchMode.DEVELOPMENT, SourceSet.MAIN_SOURCE_SET_NAME,
                devCodegenApplicationModel, GENERATED_MAIN_SOURCES,
                "Runs Quarkus code generators for main sources in development mode.");
        // Normal and dev generation share the main output directory. A dependency
        // prevents overlapping writes and also satisfies normal compile generation.
        generateDevCode.configure(task -> task.dependsOn(generateCode));
        TaskProvider<QuarkusApplicationGenerateCodeTask> generateTestCode = registerGenerateCodeTask(project, extension,
                classpath, "quarkusApplicationGenerateTestCode", LaunchMode.TEST, SourceSet.TEST_SOURCE_SET_NAME,
                testCodegenApplicationModel, GENERATED_TEST_SOURCES,
                "Runs Quarkus code generators for test sources.");
        // Test generation has a distinct output but must not race either producer
        // when a build requests main and test generation together.
        generateTestCode.configure(task -> task.mustRunAfter(generateCode, generateDevCode));
        Provider<Directory> generatedMainSources = project.getLayout().getBuildDirectory().dir(GENERATED_MAIN_SOURCES);
        Provider<Directory> generatedTestSources = project.getLayout().getBuildDirectory().dir(GENERATED_TEST_SOURCES);
        wireGeneratedSourcesIntoJavaCompilation(project, extension, generateCode, generateDevCode, generatedMainSources,
                generateTestCode, generatedTestSources);
        wireGeneratedSourcesIntoKotlinCompilation(project, generateCode, generateDevCode, generateTestCode);
        IdeGeneratedSourceWiring.wire(project, generatedMainSources, generatedTestSources);
        return new CodegenTasks(generateCode, generateDevCode, generateTestCode, generatedTestSources);
    }

    private TaskProvider<QuarkusApplicationGenerateCodeTask> registerGenerateCodeTask(Project project,
            QuarkusApplicationExtension extension, ClasspathBuilder classpath, String taskName,
            LaunchMode launchMode, String sourceSetName, TaskProvider<GenerateModelTask> modelTask,
            String generatedSourcesPath, String description) {
        taskNames.register(project, taskName);
        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet sourceSet = java.getSourceSets().getByName(sourceSetName);
        SourceSet mainSourceSet = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        Set<File> sourceParentDirectories = sourceParents(sourceSet);
        return project.getTasks().register(taskName, QuarkusApplicationGenerateCodeTask.class, task -> {
            task.getApplicationModel().set(modelTask.flatMap(GenerateModelTask::getApplicationModel));
            task.getLaunchMode().set(launchMode);
            task.getApplicationName().set(project.getName());
            task.getApplicationVersion().set(project.getVersion().toString());
            task.getBuildDirectory().set(project.getLayout().getBuildDirectory());
            task.getGeneratedOutputDirectory().set(project.getLayout().getBuildDirectory().dir(generatedSourcesPath));
            task.getQuarkusBuildProperties().set(extension.getQuarkusBuildProperties());
            task.getCodegenProviders().set(extension.getCodegen().getProviders());
            task.getCodegenInputNames().set(extension.getCodegen().getInputNames());
            configureForkOptions(task.getCodegenForkOptions(), extension.getCodeGenForkOptions());
            task.getClasspath().from(originalClasspath(classpath, launchMode),
                    deploymentConfiguration(classpath, launchMode).getIncoming().getArtifacts().getArtifactFiles());
            task.getCodegenDependencyArtifacts()
                    .from(runtimeConfiguration(classpath, launchMode).getIncoming().getArtifacts().getArtifactFiles());
            task.getSourceParentDirectories().from(sourceParentDirectories);
            task.getConfigurationSourceDirectories().from(mainSourceSet.getResources().getSourceDirectories());
            configureConfigInputs(task, extension.getConfigInputs());
            task.setGroup(QUARKUS_APPLICATION_GROUP);
            task.setDescription(description);
        });
    }

    private static Set<File> sourceParents(SourceSet sourceSet) {
        return sourceSet.getJava().getSrcDirs().stream()
                .map(File::getParentFile)
                .collect(Collectors.toSet());
    }

    private void wireGeneratedSourcesIntoJavaCompilation(Project project,
            QuarkusApplicationExtension extension,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateCode,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateDevCode,
            Provider<Directory> generatedMainSources,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateTestCode,
            Provider<Directory> generatedTestSources) {
        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet mainSourceSet = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        SourceSet testSourceSet = java.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);

        project.getTasks().named(mainSourceSet.getCompileJavaTaskName(), JavaCompile.class, task -> {
            task.dependsOn(generateCode);
            // Dev generation is requested by dev tasks, not by every Java compile.
            // Ordering is sufficient because both generators share the main output.
            task.mustRunAfter(generateDevCode);
            task.source(GeneratedSourceDirectories.fromConfiguredProviders(
                    generatedMainSources, extension.getCodegen().getProviders()));
        });
        project.getTasks().named(testSourceSet.getCompileJavaTaskName(), JavaCompile.class, task -> {
            task.dependsOn(generateCode, generateTestCode);
            task.source(GeneratedSourceDirectories.fromConfiguredProviders(
                    generatedTestSources, extension.getCodegen().getProviders()));
        });
    }

    private void wireGeneratedSourcesIntoKotlinCompilation(Project project,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateCode,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateDevCode,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateTestCode) {
        // Kotlin and Kapt may be applied after this plugin. Wire their tasks only
        // when the owning plugin is present, preserving Gradle's lazy plugin order.
        project.getPlugins().withId("org.jetbrains.kotlin.jvm",
                plugin -> KotlinGeneratedSourceWiring.wireKotlinCompileTasks(project, generateCode, generateDevCode,
                        generateTestCode));
        project.getPlugins().withId("org.jetbrains.kotlin.kapt",
                plugin -> KotlinGeneratedSourceWiring.wireKaptStubTasks(project, generateCode, generateDevCode,
                        generateTestCode));
    }

    private static FileCollection originalClasspath(ClasspathBuilder classpath, LaunchMode launchMode) {
        if (launchMode == LaunchMode.TEST) {
            return classpath.getOriginalTestRuntimeClasspathAsInput();
        }
        if (launchMode == LaunchMode.DEVELOPMENT) {
            return classpath.getOriginalDevRuntimeClasspathAsInput();
        }
        return classpath.getOriginalRuntimeClasspathAsInput();
    }

    private static Configuration runtimeConfiguration(ClasspathBuilder classpath,
            LaunchMode launchMode) {
        if (launchMode == LaunchMode.TEST) {
            return classpath.getTestRuntimeConfiguration();
        }
        if (launchMode == LaunchMode.DEVELOPMENT) {
            return classpath.getDevRuntimeConfiguration();
        }
        return classpath.getRuntimeConfiguration();
    }

    private static Configuration deploymentConfiguration(ClasspathBuilder classpath,
            LaunchMode launchMode) {
        if (launchMode == LaunchMode.TEST) {
            return classpath.getTestDeploymentConfiguration();
        }
        if (launchMode == LaunchMode.DEVELOPMENT) {
            return classpath.getDevDeploymentConfiguration();
        }
        return classpath.getDeploymentConfiguration();
    }

    record CodegenTasks(
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateCode,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateDevCode,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateTestCode,
            Provider<Directory> generatedTestSources) {
    }
}
