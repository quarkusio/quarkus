package io.quarkus.extension.gradle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.extension.gradle.dependency.DeploymentClasspathBuilder;
import io.quarkus.extension.gradle.tasks.ExtensionDescriptorTask;
import io.quarkus.extension.gradle.tasks.ValidateExtensionTask;
import io.quarkus.gradle.dependency.ApplicationDeploymentClasspathBuilder;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.runtime.LaunchMode;

public class QuarkusExtensionPlugin implements Plugin<Project> {

    public static final String DEFAULT_DEPLOYMENT_PROJECT_NAME = "deployment";
    public static final String EXTENSION_CONFIGURATION_NAME = "quarkusExtension";

    public static final String EXTENSION_DESCRIPTOR_TASK_NAME = "extensionDescriptor";
    public static final String VALIDATE_EXTENSION_TASK_NAME = "validateExtension";

    public static final String QUARKUS_ANNOTATION_PROCESSOR = "io.quarkus:quarkus-extension-processor";

    @Override
    public void apply(Project project) {
        final QuarkusExtensionConfiguration quarkusExt = project.getExtensions().create(EXTENSION_CONFIGURATION_NAME,
                QuarkusExtensionConfiguration.class);
        project.getPluginManager().apply(JavaPlugin.class);
        registerTasks(project, quarkusExt);
    }

    private void registerTasks(Project project, QuarkusExtensionConfiguration quarkusExt) {
        TaskContainer tasks = project.getTasks();
        Configuration runtimeModuleClasspath = project.getConfigurations()
                .getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);

        TaskProvider<ExtensionDescriptorTask> extensionDescriptorTask = tasks.register(EXTENSION_DESCRIPTOR_TASK_NAME,
                ExtensionDescriptorTask.class, task -> {
                    JavaPluginConvention convention = project.getConvention().getPlugin(JavaPluginConvention.class);
                    SourceSet mainSourceSet = convention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                    task.setOutputResourcesDir(mainSourceSet.getOutput().getResourcesDir());
                    task.setInputResourcesDir(mainSourceSet.getResources().getSourceDirectories().getAsPath());
                    task.setQuarkusExtensionConfiguration(quarkusExt);
                    task.setClasspath(runtimeModuleClasspath);
                });

        TaskProvider<ValidateExtensionTask> validateExtensionTask = tasks.register(VALIDATE_EXTENSION_TASK_NAME,
                ValidateExtensionTask.class, task -> {
                    task.setRuntimeModuleClasspath(runtimeModuleClasspath);
                    task.setQuarkusExtensionConfiguration(quarkusExt);
                    task.onlyIf(t -> !quarkusExt.isValidationDisabled());
                });

        project.getPlugins().withType(
                JavaPlugin.class,
                javaPlugin -> {
                    tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME, task -> task.finalizedBy(extensionDescriptorTask));
                    tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME, task -> task.dependsOn(extensionDescriptorTask));
                    tasks.withType(Test.class, test -> test.useJUnitPlatform());
                    addAnnotationProcessorDependency(project);
                });

        project.afterEvaluate(innerProject -> {
            //This must be run after the extension has been configured
            Project deploymentProject = findDeploymentProject(project, quarkusExt);
            if (deploymentProject != null) {
                ApplicationDeploymentClasspathBuilder.initConfigurations(deploymentProject);
                deploymentProject.getPlugins().withType(
                        JavaPlugin.class,
                        javaPlugin -> addAnnotationProcessorDependency(deploymentProject));

                validateExtensionTask.configure(task -> {
                    Configuration deploymentModuleClasspath = deploymentProject.getConfigurations()
                            .getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
                    task.setDeploymentModuleClasspath(deploymentModuleClasspath);
                });

                deploymentProject.getTasks().withType(Test.class, test -> {
                    test.useJUnitPlatform();
                    test.doFirst(task -> {
                        final Map<String, Object> props = test.getSystemProperties();
                        final ApplicationModel appModel = ToolingUtils.create(deploymentProject, LaunchMode.TEST);
                        try {
                            final Path serializedModel = ToolingUtils.serializeAppModel(appModel, task, true);
                            props.put(BootstrapConstants.SERIALIZED_TEST_APP_MODEL, serializedModel.toString());
                        } catch (IOException e) {
                            throw new GradleException("Unable to serialiaze gradle application model", e);
                        }
                    });
                });
                exportDeploymentClasspath(deploymentProject);
            }
        });
    }

    private void exportDeploymentClasspath(Project project) {
        DeploymentClasspathBuilder deploymentClasspathBuilder = new DeploymentClasspathBuilder(project);
        project.getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME).getIncoming()
                .beforeResolve((dependencies) -> deploymentClasspathBuilder
                        .exportDeploymentClasspath(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME));
        project.getConfigurations().getByName(JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME).getIncoming()
                .beforeResolve((testDependencies) -> deploymentClasspathBuilder
                        .exportDeploymentClasspath(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME));

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
            if (project.getParent() != null) {
                deploymentProject = project.getParent().findProject(deploymentProjectName);
            }
            if (deploymentProject == null) {
                project.getLogger().warn("Unable to find deployment project with name: " + deploymentProjectName
                        + ". You can configure the deployment project name by setting the 'deploymentArtifact' property in the plugin extension.");
            }
        }
        return deploymentProject;
    }

}
