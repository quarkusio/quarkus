package io.quarkus.extension.deployment.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Category;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;

import io.quarkus.gradle.GradleVersionSupport;
import io.quarkus.gradle.model.config.CurrentProjectDescriptorBuilder;
import io.quarkus.gradle.model.config.ExtensionVariantConstants;
import io.quarkus.gradle.model.config.LocalComponentOutputViews;
import io.quarkus.gradle.model.config.QuarkusExtensionAnnotationProcessorConfigurator;
import io.quarkus.gradle.model.config.StrictApplicationDeploymentClasspathBuilder;
import io.quarkus.gradle.model.tasks.GenerateApplicationModelTask;
import io.quarkus.gradle.model.tasks.IsolatedApplicationModelTaskConfigurator;
import io.quarkus.gradle.tooling.DefaultProjectDescriptor;
import io.quarkus.runtime.LaunchMode;

/**
 * Gradle plugin for a Quarkus extension's deployment module.
 * <p>
 * Apply plugin ID {@value #PLUGIN_ID} to the deployment project. The plugin applies Java support, configures the Quarkus
 * annotation processor and strict deployment classpaths, generates one isolated Quarkus test application model and
 * supplies it to every Gradle {@link Test} task, and publishes a marker variant that runtime extension projects use for
 * local deployment validation.
 * <p>
 * The plugin owns the marker configuration and task as well as deployment-test model wiring. Runtime extension
 * descriptor configuration remains owned by the {@code io.quarkus.extension} plugin.
 */
public class QuarkusExtensionDeploymentPlugin implements Plugin<Project> {

    /**
     * The deployment plugin ID, {@value}.
     */
    public static final String PLUGIN_ID = ExtensionVariantConstants.EXTENSION_DEPLOYMENT_PLUGIN_ID;

    /**
     * The consumable configuration that publishes the deployment marker.
     */
    public static final String MARKER_ELEMENTS_CONFIGURATION_NAME = ExtensionVariantConstants.EXTENSION_DEPLOYMENT_MARKER_ELEMENTS_CONFIGURATION_NAME;

    /**
     * The task that generates the deployment marker.
     */
    public static final String MARKER_TASK_NAME = ExtensionVariantConstants.EXTENSION_DEPLOYMENT_MARKER_TASK_NAME;

    /**
     * The Gradle category value used to select the deployment marker variant.
     */
    public static final String MARKER_CATEGORY = ExtensionVariantConstants.EXTENSION_DEPLOYMENT_MARKER_CATEGORY;
    private static final String ISOLATED_TEST_MODEL_CONFIGURATION_PREFIX = "quarkusExtensionDeploymentIsolated";

    /**
     * Applies deployment-module configuration to the target project.
     *
     * @param project the deployment project
     */
    @Override
    public void apply(Project project) {
        GradleVersionSupport.requireMinimumGradleVersion();

        project.getPluginManager().apply(JavaPlugin.class);
        StrictApplicationDeploymentClasspathBuilder.initConfigurations(project);
        new QuarkusExtensionAnnotationProcessorConfigurator().configure(project);
        registerTestApplicationModel(project);
        registerMarkerVariant(project);
    }

    private void registerMarkerVariant(Project project) {
        TaskProvider<QuarkusExtensionDeploymentMarkerTask> markerTask = project.getTasks().register(MARKER_TASK_NAME,
                QuarkusExtensionDeploymentMarkerTask.class, task -> task.getMarkerFile()
                        .convention(project.getLayout().getBuildDirectory()
                                .file("quarkus/extension-deployment-marker/" + PLUGIN_ID)));

        Configuration markerElements = project.getConfigurations().create(MARKER_ELEMENTS_CONFIGURATION_NAME);
        markerElements.setCanBeConsumed(true);
        markerElements.setCanBeResolved(false);
        markerElements.setDescription("Marker variant identifying this project as a Quarkus extension deployment module.");
        markerElements.getAttributes().attribute(Category.CATEGORY_ATTRIBUTE,
                project.getObjects().named(Category.class, MARKER_CATEGORY));
        markerElements.getAttributes().attribute(ExtensionVariantConstants.EXTENSION_DEPLOYMENT_ATTRIBUTE, true);
        markerElements.getOutgoing().artifact(markerTask.flatMap(QuarkusExtensionDeploymentMarkerTask::getMarkerFile));
    }

    private void registerTestApplicationModel(Project project) {
        Provider<DefaultProjectDescriptor> projectDescriptor = CurrentProjectDescriptorBuilder.buildForCurrentProject(project);
        StrictApplicationDeploymentClasspathBuilder testClasspath = new StrictApplicationDeploymentClasspathBuilder(project,
                LaunchMode.TEST, ISOLATED_TEST_MODEL_CONFIGURATION_PREFIX);
        LocalComponentOutputViews localComponentOutputs = LocalComponentOutputViews.of(project.getObjects(),
                testClasspath.getRuntimeConfigurationWithoutResolvingDeployment());

        TaskProvider<GenerateApplicationModelTask> generateTestAppModelTask = IsolatedApplicationModelTaskConfigurator
                .registerGenerateApplicationModelTask(project, projectDescriptor, testClasspath,
                        LaunchMode.TEST, localComponentOutputs);

        project.getTasks().withType(Test.class).configureEach(test -> {
            test.useJUnitPlatform();
            test.dependsOn(generateTestAppModelTask);
            SerializedTestApplicationModelArgumentProvider argumentProvider = project.getObjects()
                    .newInstance(SerializedTestApplicationModelArgumentProvider.class);
            argumentProvider.getApplicationModel()
                    .set(generateTestAppModelTask.flatMap(GenerateApplicationModelTask::getApplicationModel));
            test.getJvmArgumentProviders().add(argumentProvider);
        });
    }
}
