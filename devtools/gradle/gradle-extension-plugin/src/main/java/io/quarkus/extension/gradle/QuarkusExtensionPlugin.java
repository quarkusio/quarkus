package io.quarkus.extension.gradle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.extension.gradle.dependency.DeploymentClasspathBuilder;
import io.quarkus.extension.gradle.tasks.ExtensionDescriptorTask;
import io.quarkus.extension.gradle.tasks.ValidateExtensionTask;
import io.quarkus.gradle.dependency.ApplicationDeploymentClasspathBuilder;
import io.quarkus.gradle.extension.ExtensionConstants;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.gradle.tooling.dependency.DependencyUtils;
import io.quarkus.runtime.LaunchMode;

public class QuarkusExtensionPlugin implements Plugin<Project> {

    public static final String DEFAULT_DEPLOYMENT_PROJECT_NAME = "deployment";
    public static final String EXTENSION_CONFIGURATION_NAME = ExtensionConstants.EXTENSION_CONFIGURATION_NAME;

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

        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        Configuration runtimeModuleClasspath = project.getConfigurations()
                .getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);

        TaskProvider<ExtensionDescriptorTask> extensionDescriptorTask = tasks.register(EXTENSION_DESCRIPTOR_TASK_NAME,
                ExtensionDescriptorTask.class, quarkusExt, mainSourceSet, runtimeModuleClasspath);

        TaskProvider<ValidateExtensionTask> validateExtensionTask = tasks.register(VALIDATE_EXTENSION_TASK_NAME,
                ValidateExtensionTask.class, quarkusExt, runtimeModuleClasspath);

        project.getPlugins().withType(
                JavaPlugin.class,
                javaPlugin -> {
                    tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME, task -> task.finalizedBy(extensionDescriptorTask));
                    tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME, task -> task.dependsOn(extensionDescriptorTask));
                    tasks.withType(Test.class).configureEach(Test::useJUnitPlatform);
                    addAnnotationProcessorDependency(project);
                });

        project.afterEvaluate(innerProject -> {
            //This must be run after the extension has been configured
            Project deploymentProject = findDeploymentProject(project, quarkusExt);
            if (deploymentProject != null) {
                deploymentProject.getPlugins().apply(JavaPlugin.class);
                ApplicationDeploymentClasspathBuilder.initConfigurations(deploymentProject);
                deploymentProject.getPlugins().withType(
                        JavaPlugin.class,
                        javaPlugin -> addAnnotationProcessorDependency(deploymentProject));

                validateExtensionTask.configure(task -> {
                    Configuration deploymentModuleClasspath = deploymentProject.getConfigurations()
                            .getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
                    task.setDeploymentModuleClasspath(deploymentModuleClasspath);
                });

                deploymentProject.getTasks().withType(Test.class).configureEach(test -> {
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
        project.getConfigurations().getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME)
                .withDependencies(annotationProcessors -> {
                    Set<ResolvedArtifact> compileClasspathArtifacts = DependencyUtils
                            .duplicateConfiguration(project, project.getConfigurations()
                                    .getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME))
                            .getResolvedConfiguration()
                            .getResolvedArtifacts();

                    for (ResolvedArtifact artifact : compileClasspathArtifacts) {
                        ModuleVersionIdentifier id = artifact.getModuleVersion().getId();
                        if ("io.quarkus".equals(id.getGroup()) && "quarkus-core".equals(id.getName())
                                && !id.getVersion().isEmpty()) {
                            annotationProcessors.add(
                                    project.getDependencies().create(QUARKUS_ANNOTATION_PROCESSOR + ':' + id.getVersion()));
                        }
                    }
                });
    }

    private Project findDeploymentProject(Project project, QuarkusExtensionConfiguration configuration) {

        String deploymentProjectName = configuration.getDeploymentModule().get();
        if (deploymentProjectName == null) {
            deploymentProjectName = DEFAULT_DEPLOYMENT_PROJECT_NAME;
        }

        Project deploymentProject = ToolingUtils.findLocalProject(project, deploymentProjectName);
        if (deploymentProject == null) {
            project.getLogger().warn("Unable to find deployment project with name: " + deploymentProjectName
                    + ". You can configure the deployment project name by setting the 'deploymentModule' property in the plugin extension.");
        }

        return deploymentProject;
    }
}
