package io.quarkus.gradle.application.internal.plugin;

import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.QUARKUS_APPLICATION_GROUP;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

import io.quarkus.gradle.application.dsl.QuarkusApplicationBuild;
import io.quarkus.gradle.application.internal.modelgen.ClasspathBuilder;
import io.quarkus.gradle.application.internal.planning.TaskNameSegment;
import io.quarkus.gradle.application.internal.plugin.ApplicationModelAndCodegenRegistration.ApplicationTasks;
import io.quarkus.gradle.application.tasks.QuarkusApplicationPrepareOfflineTask;
import io.quarkus.gradle.model.tasks.GeneratePomClosureTask;

final class OfflinePreparationRegistration {

    static final String PREPARE_OFFLINE_TASK_NAME = "quarkusApplicationPrepareOffline";

    private final Project project;
    private final TaskNameRegistry taskNames;
    private final TaskProvider<QuarkusApplicationPrepareOfflineTask> aggregate;
    private final Map<String, TaskProvider<QuarkusApplicationPrepareOfflineTask>> namedBuildPreparations = new LinkedHashMap<>();

    private OfflinePreparationRegistration(Project project, TaskNameRegistry taskNames,
            TaskProvider<QuarkusApplicationPrepareOfflineTask> aggregate) {
        this.project = project;
        this.taskNames = taskNames;
        this.aggregate = aggregate;
    }

    static OfflinePreparationRegistration register(Project project, TaskNameRegistry taskNames,
            ClasspathBuilder classpath, Provider<Configuration> devModeClasspath, ApplicationTasks applicationTasks) {
        taskNames.register(project, PREPARE_OFFLINE_TASK_NAME);
        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet main = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        SourceSet test = java.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
        TaskProvider<QuarkusApplicationPrepareOfflineTask> aggregate = project.getTasks().register(
                PREPARE_OFFLINE_TASK_NAME, QuarkusApplicationPrepareOfflineTask.class, task -> {
                    for (Configuration configuration : classpath.getOfflinePreparationConfigurations()) {
                        task.getDependencyFiles().from(externalModuleArtifacts(configuration));
                    }
                    addExternalModuleArtifacts(task,
                            project.getConfigurations().getByName(main.getAnnotationProcessorConfigurationName()),
                            project.getConfigurations().getByName(test.getAnnotationProcessorConfigurationName()));
                    task.getDependencyFiles().from(externalModuleArtifacts(devModeClasspath.get()));
                    task.getPomClosureFiles().from(applicationTasks.applicationModelPomClosure()
                            .flatMap(GeneratePomClosureTask::getPomClosureFile));
                    task.getPreparationScopes().convention(List.of(
                            "normal", "development", "test", "code generation", "dev launcher"));
                    task.setGroup(QUARKUS_APPLICATION_GROUP);
                    task.setDescription("Resolves standalone Quarkus application dependencies for offline use.");
                });
        return new OfflinePreparationRegistration(project, taskNames, aggregate);
    }

    void registerNamedBuild(QuarkusApplicationBuild build) {
        String taskName = "quarkusApplication" + TaskNameSegment.of(build.getName()).value() + "PrepareOffline";
        taskNames.register(project, taskName);
        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet main = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        TaskProvider<QuarkusApplicationPrepareOfflineTask> preparation = project.getTasks().register(
                taskName, QuarkusApplicationPrepareOfflineTask.class, task -> {
                    addExternalModuleArtifacts(task,
                            project.getConfigurations().getByName(main.getCompileClasspathConfigurationName()),
                            project.getConfigurations().getByName(main.getRuntimeClasspathConfigurationName()),
                            project.getConfigurations().getByName(main.getAnnotationProcessorConfigurationName()));
                    task.getPreparationScopes().set(List.of("named build '" + build.getName() + "'"));
                    task.setDescription("Resolves additional dependencies selected for the '" + build.getName()
                            + "' Quarkus application build by the aggregate offline-preparation task.");
                });
        namedBuildPreparations.put(build.getName(), preparation);
        Provider<List<TaskProvider<QuarkusApplicationPrepareOfflineTask>>> selectedTask = build.getPrepareForOffline()
                .map(enabled -> enabled ? List.of(preparation) : List.of());
        aggregate.configure(task -> task.dependsOn(selectedTask));
    }

    void addNamedBuildConfigurations(String buildName, Configuration... configurations) {
        TaskProvider<QuarkusApplicationPrepareOfflineTask> preparation = namedBuildPreparations.get(buildName);
        if (preparation == null) {
            throw new IllegalStateException(
                    "No offline preparation task is registered for named build '" + buildName + "'");
        }
        preparation.configure(task -> addExternalModuleArtifacts(task, configurations));
    }

    private static void addExternalModuleArtifacts(QuarkusApplicationPrepareOfflineTask task,
            Configuration... configurations) {
        for (Configuration configuration : configurations) {
            task.getDependencyFiles().from(externalModuleArtifacts(configuration));
        }
    }

    private static FileCollection externalModuleArtifacts(Configuration configuration) {
        return configuration.getIncoming().artifactView(view -> view
                .componentFilter(ModuleComponentIdentifier.class::isInstance)).getFiles();
    }
}
