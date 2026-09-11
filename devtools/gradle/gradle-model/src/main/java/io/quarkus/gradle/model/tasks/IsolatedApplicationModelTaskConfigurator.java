package io.quarkus.gradle.model.tasks;

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import io.quarkus.gradle.model.config.LocalComponentOutputViews;
import io.quarkus.gradle.model.config.StrictApplicationDeploymentClasspathBuilder;
import io.quarkus.gradle.model.pom.DeclaredDependencyEnrichmentMode;
import io.quarkus.gradle.tooling.DefaultProjectDescriptor;
import io.quarkus.runtime.LaunchMode;

/**
 * Registers application-model tasks using only consumer-side providers and artifact views.
 * <p>
 * This configurator is the isolated-project-safe path shared by Quarkus Gradle plugins. It deliberately disables
 * declared-POM enrichment and represents local producer components through class/resource output variants. It is plugin
 * implementation API, not a build-script DSL.
 */
public final class IsolatedApplicationModelTaskConfigurator {

    private IsolatedApplicationModelTaskConfigurator() {
    }

    /**
     * Registers an application-model task path that avoids configuration-time producer-project model access.
     * <p>
     * This path uses Gradle artifact views to capture local component class/resource outputs. It intentionally avoids
     * declared dependency enrichment until the deployment classpath resolver is isolated-project safe for that path.
     *
     * @param project consumer project in which to register the task
     * @param projectDescriptor lazy workspace descriptor
     * @param classpath configured strict resolution inputs
     * @param launchMode model launch mode
     * @param localComponentOutputs variant-reselected local output views
     * @return provider of the registered task
     */
    public static TaskProvider<GenerateApplicationModelTask> registerGenerateApplicationModelTask(Project project,
            Provider<DefaultProjectDescriptor> projectDescriptor,
            StrictApplicationDeploymentClasspathBuilder classpath,
            LaunchMode launchMode,
            LocalComponentOutputViews localComponentOutputs) {
        var appModelTask = project.getTasks().register(GenerateApplicationModelTask.taskName(launchMode),
                GenerateApplicationModelTask.class, launchMode);
        appModelTask.configure(task -> {
            StrictApplicationModelTaskConfigurator.configureNoLiveProjectDeclaredDependencies(project, task,
                    projectDescriptor, classpath, launchMode, false);
            task.getDeclaredDependencyEnrichmentMode().set(DeclaredDependencyEnrichmentMode.NONE);
            configureLocalComponentOutputs(task, localComponentOutputs);
        });
        return appModelTask;
    }

    private static void configureLocalComponentOutputs(QuarkusApplicationModelTask task,
            LocalComponentOutputViews localComponentOutputs) {
        task.getLocalClassOutputArtifacts().set(localComponentOutputs.classArtifacts());
        task.getLocalResourceOutputArtifacts().set(localComponentOutputs.resourceArtifacts());
        task.getLocalClassOutputMetadata()
                .set(localComponentOutputs.classArtifacts().map(ResolvedInputFingerprint::artifactMetadata));
        task.getLocalResourceOutputMetadata()
                .set(localComponentOutputs.resourceArtifacts().map(ResolvedInputFingerprint::artifactMetadata));
        task.getLocalComponentOutputFiles().from(localComponentOutputs.classFiles(), localComponentOutputs.resourceFiles());
    }
}
