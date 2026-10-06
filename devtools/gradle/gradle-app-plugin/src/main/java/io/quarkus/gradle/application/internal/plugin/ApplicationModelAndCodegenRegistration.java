package io.quarkus.gradle.application.internal.plugin;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelResolutionViews;
import io.quarkus.gradle.application.internal.modelgen.ClasspathBuilder;
import io.quarkus.gradle.application.internal.modelgen.GenerateModelTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationGenerateCodeTask;
import io.quarkus.gradle.model.tasks.GeneratePomClosureTask;

final class ApplicationModelAndCodegenRegistration {

    private final TaskNameRegistry taskNames;

    ApplicationModelAndCodegenRegistration(TaskNameRegistry taskNames) {
        this.taskNames = taskNames;
    }

    ApplicationTasks register(Project project, QuarkusApplicationExtension extension, ClasspathBuilder classpath,
            ApplicationModelResolutionViews resolutionViews) {
        ApplicationModelTaskRegistration.ModelTasks modelTasks = new ApplicationModelTaskRegistration(taskNames)
                .register(project, classpath, resolutionViews);
        ApplicationCodegenTaskRegistration.CodegenTasks codegenTasks = new ApplicationCodegenTaskRegistration(taskNames)
                .register(project, extension, classpath, modelTasks.codegenApplicationModel(),
                        modelTasks.devCodegenApplicationModel(), modelTasks.testCodegenApplicationModel());
        return new ApplicationTasks(modelTasks.applicationModel(), modelTasks.devApplicationModel(),
                modelTasks.testApplicationModel(), modelTasks.continuousTestApplicationModel(),
                modelTasks.applicationModelPomClosure(), codegenTasks.generateCode(), codegenTasks.generateDevCode(),
                codegenTasks.generateTestCode(), codegenTasks.generatedTestSources());
    }

    record ApplicationTasks(
            TaskProvider<GenerateModelTask> applicationModel,
            TaskProvider<GenerateModelTask> devApplicationModel,
            TaskProvider<GenerateModelTask> testApplicationModel,
            TaskProvider<GenerateModelTask> continuousTestApplicationModel,
            TaskProvider<GeneratePomClosureTask> applicationModelPomClosure,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateCode,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateDevCode,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateTestCode,
            Provider<Directory> generatedTestSources) {
    }
}
