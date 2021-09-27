package io.quarkus.extension.gradle;

import java.io.File;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;

import io.quarkus.extension.gradle.tasks.ExtensionDescriptorTask;

public class QuarkusExtensionPlugin implements Plugin<Project> {

    public static final String DEFAULT_DEPLOYMENT_PROJECT_NAME = "deployment";
    public static final String EXTENSION_CONFIGURATION_NAME = "quarkusExtension";
    public static final String EXTENSION_DESCRIPTOR_TASK_NAME = "extensionDescriptor";

    public static final String QUARKUS_ANNOTATION_PROCESSOR = "io.quarkus:quarkus-extension-processor";

    @Override
    public void apply(Project project) {
        final QuarkusExtensionConfiguration quarkusExt = project.getExtensions().create(EXTENSION_CONFIGURATION_NAME,
                QuarkusExtensionConfiguration.class);
        registerTasks(project, quarkusExt);
    }

    private void registerTasks(Project project, QuarkusExtensionConfiguration quarkusExt) {
        TaskContainer tasks = project.getTasks();
        ExtensionDescriptorTask extensionDescriptorTask = tasks.create(EXTENSION_DESCRIPTOR_TASK_NAME,
                ExtensionDescriptorTask.class, task -> {
                    JavaPluginConvention convention = project.getConvention().getPlugin(JavaPluginConvention.class);
                    File resourcesDir = convention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput()
                            .getResourcesDir();
                    task.setResourcesDir(resourcesDir);
                    task.setQuarkusExtensionConfiguration(quarkusExt);
                });

        project.getPlugins().withType(
                JavaPlugin.class,
                javaPlugin -> {
                    Task jarTask = tasks.getByName(JavaPlugin.JAR_TASK_NAME);
                    jarTask.dependsOn(extensionDescriptorTask);

                    Configuration annotationProcessorConfiguration = project.getConfigurations()
                            .getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME);
                    addAnnotationProcessorDependency(annotationProcessorConfiguration, project.getDependencies());
                });

        Project deploymentProject = findDeploymentProject(project, quarkusExt);
        if (deploymentProject != null) {
            deploymentProject.getPlugins().withType(
                    JavaPlugin.class,
                    javaPlugin -> {
                        Configuration deploymentAnnotationProcessorConfiguration = deploymentProject.getConfigurations()
                                .getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME);
                        addAnnotationProcessorDependency(deploymentAnnotationProcessorConfiguration,
                                deploymentProject.getDependencies());
                    });
        }
    }

    private void addAnnotationProcessorDependency(Configuration configuration, DependencyHandler dependencyHandler) {
        configuration
                .withDependencies(dependencies -> {
                    Dependency annotationProcessor = dependencyHandler.create(QUARKUS_ANNOTATION_PROCESSOR);
                    dependencies.add(annotationProcessor);
                });
    }

    private Project findDeploymentProject(Project project, QuarkusExtensionConfiguration configuration) {

        String deploymentProjectName = configuration.getDeploymentModule();
        if (deploymentProjectName == null) {
            deploymentProjectName = DEFAULT_DEPLOYMENT_PROJECT_NAME;
        }

        Project deploymentProject = project.getRootProject().findProject(deploymentProjectName);
        if (deploymentProject == null) {
            project.getLogger().warn("Unable to find deployment project with name: " + deploymentProjectName
                    + ". You can configure the deployment project name by setting the 'deploymentProject' property in the plugin extension.");
        }

        return deploymentProject;
    }

}
