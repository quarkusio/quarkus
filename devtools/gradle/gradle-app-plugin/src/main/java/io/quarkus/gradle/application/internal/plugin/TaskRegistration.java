package io.quarkus.gradle.application.internal.plugin;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Provider;
import org.gradle.build.event.BuildEventsListenerRegistry;

import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.internal.image.ImageReferenceClaimService;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelResolutionViews;
import io.quarkus.gradle.application.internal.modelgen.ClasspathBuilder;
import io.quarkus.gradle.application.internal.plugin.ApplicationModelAndCodegenRegistration.ApplicationTasks;
import io.quarkus.gradle.application.tasks.QuarkusApplicationImageBuildTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationImagePushTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationImageReferenceResolutionTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupOptimizedImageBuildTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupOptimizedImagePushTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupOptimizedImageReferenceResolutionTask;

final class TaskRegistration {

    ApplicationModelResolutionViews register(Project project, QuarkusApplicationExtension extension,
            boolean legacyPluginPresent, BuildEventsListenerRegistry buildEventsListeners,
            DslLifecycleCoordinator lifecycle) {
        TaskNameRegistry taskNames = new TaskNameRegistry();
        Configuration developmentDependencies = DevelopmentDependencyRegistration.register(project, legacyPluginPresent);
        ClasspathBuilder classpath = new ClasspathBuilder(project, developmentDependencies);
        ApplicationModelResolutionViews resolutionViews = ApplicationModelResolutionViews.create(project, classpath);
        ApplicationTasks applicationTasks = new ApplicationModelAndCodegenRegistration(taskNames)
                .register(project, extension, classpath, resolutionViews);
        Provider<Configuration> devModeClasspath = DevTaskRegistration.registerDevModeClasspathConfiguration(project);
        OfflinePreparationRegistration offlinePreparation = OfflinePreparationRegistration.register(
                project, taskNames, classpath, devModeClasspath, applicationTasks);
        QuarkusApplicationIntegrationTestConfigurator integrationTests = ApplicationTestTaskRegistration
                .register(project, extension, applicationTasks, taskNames, offlinePreparation, lifecycle);
        new DevTaskRegistration(taskNames, buildEventsListeners)
                .register(project, extension, classpath, devModeClasspath, applicationTasks);
        Provider<ImageReferenceClaimService> imageReferenceClaims = project.getGradle().getSharedServices()
                .registerIfAbsent(imageReferenceClaimServiceName(project), ImageReferenceClaimService.class,
                        specification -> {
                        });
        configureImageReferencePreflightBarrier(project);
        NamedBuildTaskRegistration namedBuilds = new NamedBuildTaskRegistration(taskNames,
                applicationTasks.applicationModel(), integrationTests, offlinePreparation, imageReferenceClaims,
                lifecycle);
        extension.getBuilds().all(build -> namedBuilds.register(project, extension, build));
        return resolutionViews;
    }

    private static void configureImageReferencePreflightBarrier(Project project) {
        var normalResolvers = project.getTasks().withType(QuarkusApplicationImageReferenceResolutionTask.class);
        var optimizedResolvers = project.getTasks()
                .withType(QuarkusApplicationStartupOptimizedImageReferenceResolutionTask.class);
        project.getTasks().withType(QuarkusApplicationImageBuildTask.class)
                .configureEach(task -> task.mustRunAfter(normalResolvers, optimizedResolvers));
        project.getTasks().withType(QuarkusApplicationImagePushTask.class)
                .configureEach(task -> task.mustRunAfter(normalResolvers, optimizedResolvers));
        project.getTasks().withType(QuarkusApplicationStartupOptimizedImageBuildTask.class)
                .configureEach(task -> task.mustRunAfter(normalResolvers, optimizedResolvers));
        project.getTasks().withType(QuarkusApplicationStartupOptimizedImagePushTask.class)
                .configureEach(task -> task.mustRunAfter(normalResolvers, optimizedResolvers));
    }

    private static String imageReferenceClaimServiceName(Project project) {
        return "quarkusApplicationImageReferenceClaims-"
                + project.getPath().chars()
                        .mapToObj(Integer::toHexString)
                        .collect(java.util.stream.Collectors.joining("-"));
    }
}
