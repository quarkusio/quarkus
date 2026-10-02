package io.quarkus.gradle.application.internal.plugin;

import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.QUARKUS_APPLICATION_GROUP;

import java.util.Locale;

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import io.quarkus.gradle.application.dsl.QuarkusAotJarOutput;
import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.internal.execution.ImageOperation;
import io.quarkus.gradle.application.internal.image.ImageReferenceClaimService;
import io.quarkus.gradle.application.internal.modelgen.GenerateModelTask;
import io.quarkus.gradle.application.internal.planning.TaskNames;
import io.quarkus.gradle.application.tasks.QuarkusApplicationImageBuildTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationImagePushTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationImageReferenceResolutionTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationImageTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupOptimizedImageBuildTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupOptimizedImagePushTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupOptimizedImageReferenceResolutionTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupOptimizedImageTask;

final class NamedImageTaskRegistration {

    private final TaskNameRegistry taskNames;
    private final TaskProvider<GenerateModelTask> applicationModel;
    private final Provider<ImageReferenceClaimService> imageReferenceClaims;

    NamedImageTaskRegistration(TaskNameRegistry taskNames, TaskProvider<GenerateModelTask> applicationModel,
            Provider<ImageReferenceClaimService> imageReferenceClaims) {
        this.taskNames = taskNames;
        this.applicationModel = applicationModel;
        this.imageReferenceClaims = imageReferenceClaims;
    }

    NamedImageTasks registerNormalImages(Project project, QuarkusApplicationExtension extension,
            NamedBuildTaskRegistration.BuildRegistration buildRegistration, TaskNames names) {
        // Claim effective references in predecessor tasks so collisions fail before
        // an image build or push performs externally visible work.
        TaskProvider<QuarkusApplicationImageReferenceResolutionTask> imageBuildPreflight = registerImagePreflightTask(
                project, extension, buildRegistration, names.imageBuild(), ImageOperation.BUILD);
        TaskProvider<QuarkusApplicationImageBuildTask> imageBuild = registerImageBuildTask(project, extension,
                buildRegistration, names.imageBuild(), imageBuildPreflight);
        TaskProvider<QuarkusApplicationImageReferenceResolutionTask> imagePushPreflight = registerImagePreflightTask(
                project, extension, buildRegistration, names.imagePush(), ImageOperation.PUSH);
        TaskProvider<QuarkusApplicationImagePushTask> imagePush = registerImagePushTask(project, extension,
                buildRegistration, names.imagePush(), imagePushPreflight);
        return new NamedImageTasks(imageBuildPreflight, imageBuild, imagePushPreflight, imagePush);
    }

    void registerStartupOptimizedImages(Project project, QuarkusApplicationExtension extension,
            NamedBuildTaskRegistration.BuildRegistration buildRegistration, TaskNames names,
            QuarkusAotJarOutput aotJar, NamedImageTasks imageTasks) {
        // Optimized references derive from the already claimed base reference. Their
        // own preflight must likewise complete before either image operation starts.
        TaskProvider<QuarkusApplicationStartupOptimizedImageReferenceResolutionTask> optimizedBuildPreflight = registerStartupOptimizedImagePreflightTask(
                project, buildRegistration, names.startupOptimizedImageBuild(), imageTasks.imageBuildPreflight(), aotJar);
        registerStartupOptimizedImageBuildTask(project, extension, buildRegistration,
                names.startupOptimizedImageBuild(), names, aotJar, optimizedBuildPreflight);
        TaskProvider<QuarkusApplicationStartupOptimizedImageReferenceResolutionTask> optimizedPushPreflight = registerStartupOptimizedImagePreflightTask(
                project, buildRegistration, names.startupOptimizedImagePush(), imageTasks.imageBuildPreflight(), aotJar);
        registerStartupOptimizedImagePushTask(project, extension, buildRegistration,
                names.startupOptimizedImagePush(), names, aotJar, optimizedPushPreflight);
    }

    private TaskProvider<QuarkusApplicationImageReferenceResolutionTask> registerImagePreflightTask(Project project,
            QuarkusApplicationExtension extension,
            NamedBuildTaskRegistration.BuildRegistration buildRegistration, String operationTaskName,
            ImageOperation operation) {
        String taskName = operationTaskName + "ReferencePreflight";
        taskNames.register(project, taskName);
        return project.getTasks().register(taskName,
                QuarkusApplicationImageReferenceResolutionTask.class, task -> {
                    configureImageTask(project, extension, task, buildRegistration);
                    task.getOperationKind().set(operation);
                    task.getOwnerProjectPath().set(project.getPath());
                    String operationPath = operation == ImageOperation.BUILD ? "image-build" : "image-push";
                    task.getOutputDirectory().set(project.getLayout().getBuildDirectory()
                            .dir(NamedBuildTaskRegistration.operationOutputPath(buildRegistration,
                                    operationPath + "-reference-preflight")));
                    task.getResolutionReceiptFile().set(project.getLayout().getBuildDirectory()
                            .file(NamedBuildTaskRegistration.operationResultPath(buildRegistration, operationPath,
                                    "image-reference-preflight.properties")));
                    task.getClaimService().set(imageReferenceClaims);
                    task.usesService(imageReferenceClaims);
                    task.setDescription("Resolves and claims the effective container image references for the '"
                            + buildRegistration.name() + "' Quarkus application "
                            + operation.name().toLowerCase(Locale.ROOT) + " operation.");
                });
    }

    private TaskProvider<QuarkusApplicationImageBuildTask> registerImageBuildTask(Project project,
            QuarkusApplicationExtension extension,
            NamedBuildTaskRegistration.BuildRegistration buildRegistration, String taskName,
            TaskProvider<QuarkusApplicationImageReferenceResolutionTask> preflight) {
        taskNames.register(project, taskName);
        return project.getTasks().register(taskName, QuarkusApplicationImageBuildTask.class, task -> {
            configureImageTask(project, extension, task, buildRegistration);
            task.dependsOn(preflight);
            task.getImageReferencePreflightReceiptFile().set(preflight
                    .flatMap(QuarkusApplicationImageReferenceResolutionTask::getResolutionReceiptFile));
            task.getOutputDirectory().set(project.getLayout().getBuildDirectory()
                    .dir(NamedBuildTaskRegistration.operationOutputPath(buildRegistration, "image-build")));
            task.getReceiptFile().set(project.getLayout().getBuildDirectory()
                    .file(NamedBuildTaskRegistration.operationResultPath(buildRegistration, "image-build",
                            "image-build-result.properties")));
            task.setGroup(QUARKUS_APPLICATION_GROUP);
            task.setDescription("Builds the container image for the '" + buildRegistration.name()
                    + "' Quarkus application build.");
        });
    }

    private TaskProvider<QuarkusApplicationImagePushTask> registerImagePushTask(Project project,
            QuarkusApplicationExtension extension,
            NamedBuildTaskRegistration.BuildRegistration buildRegistration, String taskName,
            TaskProvider<QuarkusApplicationImageReferenceResolutionTask> preflight) {
        taskNames.register(project, taskName);
        return project.getTasks().register(taskName, QuarkusApplicationImagePushTask.class, task -> {
            configureImageTask(project, extension, task, buildRegistration);
            task.dependsOn(preflight);
            task.getImageReferencePreflightReceiptFile().set(preflight
                    .flatMap(QuarkusApplicationImageReferenceResolutionTask::getResolutionReceiptFile));
            task.getOutputDirectory().set(project.getLayout().getBuildDirectory()
                    .dir(NamedBuildTaskRegistration.operationOutputPath(buildRegistration, "image-push")));
            task.getReceiptFile().set(project.getLayout().getBuildDirectory()
                    .file(NamedBuildTaskRegistration.operationResultPath(buildRegistration, "image-push",
                            "image-push-result.properties")));
            task.setGroup(QUARKUS_APPLICATION_GROUP);
            task.setDescription("Builds and pushes the container image for the '" + buildRegistration.name()
                    + "' Quarkus application build.");
        });
    }

    private TaskProvider<QuarkusApplicationStartupOptimizedImageReferenceResolutionTask> registerStartupOptimizedImagePreflightTask(
            Project project, NamedBuildTaskRegistration.BuildRegistration buildRegistration, String operationTaskName,
            TaskProvider<QuarkusApplicationImageReferenceResolutionTask> basePreflight, QuarkusAotJarOutput aotJar) {
        String taskName = operationTaskName + "ReferencePreflight";
        taskNames.register(project, taskName);
        return project.getTasks().register(taskName,
                QuarkusApplicationStartupOptimizedImageReferenceResolutionTask.class, task -> {
                    task.getBuildName().set(buildRegistration.name());
                    task.getOwnerProjectPath().set(project.getPath());
                    task.getImageSuffix().set(aotJar.getStartupOptimizedImage().getImageSuffix());
                    task.dependsOn(basePreflight);
                    task.getBaseResolutionReceiptFile().set(basePreflight.flatMap(
                            QuarkusApplicationImageReferenceResolutionTask::getResolutionReceiptFile));
                    String operation = operationTaskName.endsWith("Push")
                            ? "startup-optimized-image-push"
                            : "startup-optimized-image-build";
                    task.getResolutionReceiptFile().set(project.getLayout().getBuildDirectory()
                            .file(NamedBuildTaskRegistration.operationResultPath(buildRegistration, operation,
                                    "image-reference-preflight.properties")));
                    task.getClaimService().set(imageReferenceClaims);
                    task.usesService(imageReferenceClaims);
                    task.setDescription("Resolves and claims the startup-optimized container image reference for the '"
                            + buildRegistration.name() + "' Quarkus application operation.");
                });
    }

    private void registerStartupOptimizedImageBuildTask(Project project, QuarkusApplicationExtension extension,
            NamedBuildTaskRegistration.BuildRegistration buildRegistration, String taskName, TaskNames names,
            QuarkusAotJarOutput aotJar,
            TaskProvider<QuarkusApplicationStartupOptimizedImageReferenceResolutionTask> preflight) {
        taskNames.register(project, taskName);
        project.getTasks().register(taskName, QuarkusApplicationStartupOptimizedImageBuildTask.class, task -> {
            configureStartupOptimizedImageTask(project, extension, task, buildRegistration, aotJar);
            task.dependsOn(preflight);
            task.getImageReferencePreflightReceiptFile().set(preflight
                    .flatMap(QuarkusApplicationStartupOptimizedImageReferenceResolutionTask::getResolutionReceiptFile));
            task.getOutputDirectory().set(project.getLayout().getBuildDirectory()
                    .dir(NamedBuildTaskRegistration.operationOutputPath(buildRegistration,
                            "startup-optimized-image-build")));
            task.dependsOn(names.imageBuild());
            task.getBaseImageReceiptFile().set(project.getLayout().getBuildDirectory()
                    .file(NamedBuildTaskRegistration.operationResultPath(buildRegistration, "image-build",
                            "image-build-result.properties")));
            task.getReceiptFile().set(project.getLayout().getBuildDirectory()
                    .file(NamedBuildTaskRegistration.operationResultPath(buildRegistration,
                            "startup-optimized-image-build", "startup-optimized-image-build-result.properties")));
            task.setGroup(QUARKUS_APPLICATION_GROUP);
            task.setDescription("Builds the startup-optimized container image for the '" + buildRegistration.name()
                    + "' Quarkus application build.");
        });
    }

    private void registerStartupOptimizedImagePushTask(Project project, QuarkusApplicationExtension extension,
            NamedBuildTaskRegistration.BuildRegistration buildRegistration, String taskName, TaskNames names,
            QuarkusAotJarOutput aotJar,
            TaskProvider<QuarkusApplicationStartupOptimizedImageReferenceResolutionTask> preflight) {
        taskNames.register(project, taskName);
        project.getTasks().register(taskName, QuarkusApplicationStartupOptimizedImagePushTask.class, task -> {
            configureStartupOptimizedImageTask(project, extension, task, buildRegistration, aotJar);
            task.dependsOn(preflight);
            task.getImageReferencePreflightReceiptFile().set(preflight
                    .flatMap(QuarkusApplicationStartupOptimizedImageReferenceResolutionTask::getResolutionReceiptFile));
            task.getOutputDirectory().set(project.getLayout().getBuildDirectory()
                    .dir(NamedBuildTaskRegistration.operationOutputPath(buildRegistration,
                            "startup-optimized-image-push")));
            task.dependsOn(names.imageBuild());
            task.getBaseImageReceiptFile().set(project.getLayout().getBuildDirectory()
                    .file(NamedBuildTaskRegistration.operationResultPath(buildRegistration, "image-build",
                            "image-build-result.properties")));
            task.getReceiptFile().set(project.getLayout().getBuildDirectory()
                    .file(NamedBuildTaskRegistration.operationResultPath(buildRegistration,
                            "startup-optimized-image-push", "startup-optimized-image-push-result.properties")));
            task.setGroup(QUARKUS_APPLICATION_GROUP);
            task.setDescription("Builds and pushes the startup-optimized container image for the '"
                    + buildRegistration.name() + "' Quarkus application build.");
        });
    }

    private void configureImageTask(Project project, QuarkusApplicationExtension extension,
            QuarkusApplicationImageTask task, NamedBuildTaskRegistration.BuildRegistration buildRegistration) {
        NamedBuildTaskRegistration.configureNamedBuildTask(project, extension, task, buildRegistration, applicationModel);
        var build = buildRegistration.build();
        task.getImageReference().set(build.getImage().getImageReference());
        task.getImageRepository().set(build.getImage().getRepository());
        task.getImageTag().set(build.getImage().getTag());
        task.getImageBuilder().set(build.getImage().getBuilder());
        task.getImageQuarkusBuildProperties().set(build.getImage().getQuarkusBuildProperties());
        task.getQuarkusBuildProperties().putAll(build.getImage().getQuarkusBuildProperties());
    }

    private void configureStartupOptimizedImageTask(Project project, QuarkusApplicationExtension extension,
            QuarkusApplicationStartupOptimizedImageTask task,
            NamedBuildTaskRegistration.BuildRegistration buildRegistration, QuarkusAotJarOutput aotJar) {
        NamedBuildTaskRegistration.configureNamedBuildTask(project, extension, task, buildRegistration, applicationModel);
        task.getQuarkusBuildProperties().putAll(aotJar.getImage().getQuarkusBuildProperties());
        task.getImageBuilder().set(aotJar.getImage().getBuilder());
        task.getArchiveType().set(aotJar.getStartupArchive().getType());
        task.getArchiveFile().set(aotJar.getStartupArchive().getFile());
        task.getArchiveDirectory().set(aotJar.getStartupArchive().getDirectory());
        task.getImageSuffix().set(aotJar.getStartupOptimizedImage().getImageSuffix());
    }

    record NamedImageTasks(
            TaskProvider<QuarkusApplicationImageReferenceResolutionTask> imageBuildPreflight,
            TaskProvider<QuarkusApplicationImageBuildTask> imageBuild,
            TaskProvider<QuarkusApplicationImageReferenceResolutionTask> imagePushPreflight,
            TaskProvider<QuarkusApplicationImagePushTask> imagePush) {
    }
}
