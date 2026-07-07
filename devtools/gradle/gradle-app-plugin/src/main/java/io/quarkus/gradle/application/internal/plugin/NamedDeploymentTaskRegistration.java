package io.quarkus.gradle.application.internal.plugin;

import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.QUARKUS_APPLICATION_GROUP;

import java.util.HashMap;
import java.util.Map;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import io.quarkus.gradle.application.dsl.QuarkusApplicationDeployment;
import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.internal.modelgen.GenerateModelTask;
import io.quarkus.gradle.application.internal.planning.TaskNameSegment;
import io.quarkus.gradle.application.internal.planning.TaskNames;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentImageSource;
import io.quarkus.gradle.application.tasks.QuarkusApplicationDeployTask;

final class NamedDeploymentTaskRegistration {

    private final Map<String, String> deploymentNames = new HashMap<>();
    private final TaskNameRegistry taskNames;
    private final TaskProvider<GenerateModelTask> applicationModel;

    NamedDeploymentTaskRegistration(TaskNameRegistry taskNames, TaskProvider<GenerateModelTask> applicationModel) {
        this.taskNames = taskNames;
        this.applicationModel = applicationModel;
    }

    TaskProvider<QuarkusApplicationDeployTask> register(Project project, QuarkusApplicationExtension extension,
            NamedBuildTaskRegistration.BuildRegistration buildRegistration,
            QuarkusApplicationDeployment deployment, String taskName,
            NamedImageTaskRegistration.NamedImageTasks imageTasks) {
        taskNames.register(project, taskName);
        var build = buildRegistration.build();
        TaskProvider<QuarkusApplicationDeployTask> deploy = project.getTasks().register(taskName,
                QuarkusApplicationDeployTask.class, task -> {
                    NamedBuildTaskRegistration.configureNamedBuildTask(project, extension, task, buildRegistration,
                            applicationModel);
                    task.getDeploymentName().set(deployment.getName());
                    task.getDeploymentTarget().set(deployment.getTarget());
                    task.getImageSource().set(deployment.getImageSource());
                    task.getImageReference().set(deployment.getImageReference());
                    task.getReceiptFile().set(project.getLayout().getBuildDirectory()
                            .file(NamedBuildTaskRegistration.operationResultPath(buildRegistration,
                                    "deployments/" + deployment.getName(), "deployment-result.properties")));
                    task.setGroup(QUARKUS_APPLICATION_GROUP);
                    task.setDescription("Deploys the '" + build.getName() + "' Quarkus application build to the '"
                            + deployment.getName() + "' " + deployment.getTarget().quarkusDeployTarget() + " target.");
                });
        deploy.configure(task -> {
            if (deployment.getImageSource().getOrElse(
                    QuarkusApplicationDeploymentImageSource.NORMAL_IMAGE_PUSH) == QuarkusApplicationDeploymentImageSource.NORMAL_IMAGE_PUSH) {
                task.dependsOn(imageTasks.imagePush());
                task.getNormalImagePushReceiptFile().set(project.getLayout().getBuildDirectory()
                        .file(NamedBuildTaskRegistration.operationResultPath(buildRegistration, "image-push",
                                "image-push-result.properties")));
            }
        });
        return deploy;
    }

    void configureStartupOptimizedPredecessor(Project project,
            NamedBuildTaskRegistration.BuildRegistration buildRegistration,
            QuarkusApplicationDeployment deployment, TaskProvider<QuarkusApplicationDeployTask> deploy,
            TaskNames names) {
        deploy.configure(task -> {
            if (deployment.getImageSource().getOrElse(
                    QuarkusApplicationDeploymentImageSource.NORMAL_IMAGE_PUSH) == QuarkusApplicationDeploymentImageSource.STARTUP_OPTIMIZED_IMAGE_PUSH) {
                task.dependsOn(names.startupOptimizedImagePush());
                task.getStartupOptimizedImagePushReceiptFile().set(project.getLayout().getBuildDirectory()
                        .file(NamedBuildTaskRegistration.operationResultPath(buildRegistration,
                                "startup-optimized-image-push", "startup-optimized-image-push-result.properties")));
            }
        });
    }

    void validateName(QuarkusApplicationDeployment deployment) {
        String previous = deploymentNames.putIfAbsent(TaskNameSegment.of(deployment.getName()).collisionKey(),
                deployment.getName());
        if (previous != null) {
            throw new GradleException("Quarkus application deployment names '" + previous + "' and '"
                    + deployment.getName() + "' derive the same task-name segment");
        }
    }
}
