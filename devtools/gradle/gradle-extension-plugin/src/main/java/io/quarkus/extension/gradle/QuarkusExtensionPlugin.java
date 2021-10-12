package io.quarkus.extension.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

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
        TaskProvider<ExtensionDescriptorTask> extensionDescriptorTask = tasks.register(EXTENSION_DESCRIPTOR_TASK_NAME,
                ExtensionDescriptorTask.class, task -> {
                    JavaPluginConvention convention = project.getConvention().getPlugin(JavaPluginConvention.class);
                    SourceSet mainSourceSet = convention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                    task.setOutputResourcesDir(mainSourceSet.getOutput().getResourcesDir());
                    task.setInputResourcesDir(mainSourceSet.getResources().getSourceDirectories().getAsPath());
                    task.setQuarkusExtensionConfiguration(quarkusExt);
                    Configuration classpath = project.getConfigurations()
                            .getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
                    task.setClasspath(classpath);
                });

        project.getPlugins().withType(
                JavaPlugin.class,
                javaPlugin -> {
                    Task jarTask = tasks.getByName(JavaPlugin.JAR_TASK_NAME);
                    jarTask.dependsOn(extensionDescriptorTask);

                    addAnnotationProcessorDependency(project);
                });

        project.afterEvaluate(innerProject -> {
            //This must be run after the extension has been configured
            Project deploymentProject = findDeploymentProject(project, quarkusExt);
            if (deploymentProject != null) {
                deploymentProject.getPlugins().withType(
                        JavaPlugin.class,
                        javaPlugin -> {
                            addAnnotationProcessorDependency(deploymentProject);
                        });
            }
        });
    }

    private void addAnnotationProcessorDependency(Project project) {
        project.getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
                .getResolutionStrategy().eachDependency(d -> {
                    if ("io.quarkus".equals(d.getRequested().getGroup())
                            && "quarkus-core".equals(d.getRequested().getName())
                            && !d.getRequested().getVersion().isEmpty()) {
                        project.getDependencies().add(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME,
                                QUARKUS_ANNOTATION_PROCESSOR + ':' + d.getRequested().getVersion());
                    }
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
