package io.quarkus.gradle.model.tasks;

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import io.quarkus.gradle.model.config.StrictApplicationDeploymentClasspathBuilder;
import io.quarkus.gradle.tooling.DefaultProjectDescriptor;
import io.quarkus.runtime.LaunchMode;

/**
 * Wires strict, provider-backed classpath inputs into an application-model task.
 * <p>
 * The configurator does not resolve configurations and does not inspect producer projects. It is shared plugin
 * implementation API rather than a user-facing task configuration surface.
 */
public final class StrictApplicationModelTaskConfigurator {

    private StrictApplicationModelTaskConfigurator() {
    }

    /**
     * Configures an application-model task without reading live Gradle project models for declared dependency enrichment.
     *
     * @param project project that owns the task and output
     * @param task task to configure
     * @param projectDescriptor lazy workspace descriptor
     * @param classpath strict resolution configurations
     * @param launchMode model launch mode
     * @param buildModel whether normal mode should use the build-model output path
     * @throws IllegalArgumentException if the launch mode/output combination is unsupported
     */
    public static void configureNoLiveProjectDeclaredDependencies(Project project, QuarkusApplicationModelTask task,
            Provider<DefaultProjectDescriptor> projectDescriptor,
            StrictApplicationDeploymentClasspathBuilder classpath,
            LaunchMode launchMode, boolean buildModel) {
        task.getProjectDescriptor().set(projectDescriptor);
        task.getLaunchMode().set(launchMode);
        task.getTypeModel().set(task.getPath());
        task.getApplicationModel()
                .set(project.getLayout().getBuildDirectory().file(GenerateApplicationModelTask.applicationModelPath(launchMode,
                        buildModel)));
        task.getOriginalClasspath().setFrom(classpath.getOriginalRuntimeClasspathAsInput());
        task.getAppClasspath().configureFrom(classpath.getRuntimeConfigurationWithoutResolvingDeployment());
        task.getPlatformConfiguration().configureFrom(classpath.getPlatformConfiguration());
        task.getPlatformInfo().configureFrom(classpath.getPlatformPropertiesConfiguration());
        task.getCompileOnlyClasspath().configureFrom(classpath.getCompileOnlyWithoutResolvingDeployment());
        task.getDeploymentClasspath().configureFrom(classpath.getDeploymentConfiguration());
        task.getDeploymentClasspathFiles()
                .from(classpath.getDeploymentConfiguration().getIncoming().getArtifacts().getArtifactFiles());
    }
}
