package io.quarkus.extension.gradle;

import java.util.Collections;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Category;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.jvm.tasks.ProcessResources;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.extension.gradle.tasks.ExtensionDescriptorTask;
import io.quarkus.extension.gradle.tasks.ValidateExtensionTask;
import io.quarkus.gradle.GradleVersionSupport;
import io.quarkus.gradle.extension.ExtensionConstants;
import io.quarkus.gradle.model.config.ExtensionVariantConstants;
import io.quarkus.gradle.model.config.QuarkusExtensionAnnotationProcessorConfigurator;

public class QuarkusExtensionPlugin implements Plugin<Project> {

    public static final String EXTENSION_CONFIGURATION_NAME = ExtensionConstants.EXTENSION_CONFIGURATION_NAME;

    public static final String EXTENSION_DESCRIPTOR_TASK_NAME = "extensionDescriptor";
    public static final String VALIDATE_EXTENSION_TASK_NAME = "validateExtension";
    private static final String DEPLOYMENT_CLASSPATH_CONFIGURATION_NAME = "quarkusDeploymentClasspath";
    private static final String DEPLOYMENT_MARKER_CONFIGURATION_NAME = "quarkusDeploymentMarker";
    public static final String DEPLOYMENT_DEPENDENCY_ELEMENTS_CONFIGURATION_NAME = ExtensionVariantConstants.EXTENSION_DEPLOYMENT_DEPENDENCY_ELEMENTS_CONFIGURATION_NAME;

    public static final String QUARKUS_ANNOTATION_PROCESSOR = ExtensionVariantConstants.QUARKUS_ANNOTATION_PROCESSOR;

    public QuarkusExtensionPlugin() {
    }

    @Override
    public void apply(Project project) {
        GradleVersionSupport.requireMinimumGradleVersion();

        final QuarkusExtensionConfiguration quarkusExt = project.getExtensions().create(EXTENSION_CONFIGURATION_NAME,
                QuarkusExtensionConfiguration.class, project.getObjects());

        project.getPluginManager().apply(JavaPlugin.class);
        registerTasks(project, quarkusExt);
    }

    private void registerTasks(Project project, QuarkusExtensionConfiguration quarkusExt) {
        TaskContainer tasks = project.getTasks();

        SourceSet mainSourceSet = project.getExtensions().getByType(SourceSetContainer.class)
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        Configuration runtimeModuleClasspath = project.getConfigurations()
                .getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);

        Configuration deploymentClasspath = createDeploymentClasspath(project, quarkusExt);
        Configuration deploymentMarker = createDeploymentMarker(project, quarkusExt);
        createDeploymentDependencyElements(project, quarkusExt);
        Provider<Boolean> localDeploymentValidationEnabled = quarkusExt.getDeploymentArtifact()
                .map(deploymentArtifact -> false)
                .orElse(true);

        TaskProvider<ValidateExtensionTask> validateExtensionTask = tasks.register(VALIDATE_EXTENSION_TASK_NAME,
                ValidateExtensionTask.class, quarkusExt, runtimeModuleClasspath,
                deploymentClasspath, deploymentMarker, localDeploymentValidationEnabled);

        TaskProvider<ProcessResources> processResourcesTask = tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME,
                ProcessResources.class);

        // The source YAML is an input to descriptor generation, not a resource that should be copied before the
        // generated descriptor. Keeping raw and generated descriptors out of the same Copy source also gives
        // processResources sole ownership of its destination directory.
        mainSourceSet.getResources().exclude(BootstrapConstants.DESCRIPTOR_PATH, BootstrapConstants.EXTENSION_METADATA_PATH);

        TaskProvider<ExtensionDescriptorTask> extensionDescriptorTask = tasks.register(EXTENSION_DESCRIPTOR_TASK_NAME,
                ExtensionDescriptorTask.class, quarkusExt, mainSourceSet, runtimeModuleClasspath);

        extensionDescriptorTask.configure(task -> task.dependsOn(validateExtensionTask));
        processResourcesTask.configure(
                task -> task.from(extensionDescriptorTask.flatMap(ExtensionDescriptorTask::getOutputDirectory)));

        project.getPlugins().withType(
                JavaPlugin.class,
                javaPlugin -> {
                    project.getConfigurations().named(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME,
                            configuration -> configuration
                                    .getAttributes().attribute(ExtensionVariantConstants.EXTENSION_RUNTIME_ATTRIBUTE, true));
                    tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME, task -> task.dependsOn(extensionDescriptorTask));
                    tasks.withType(Test.class).configureEach(Test::useJUnitPlatform);
                    addAnnotationProcessorDependency(project);
                });
    }

    private void addAnnotationProcessorDependency(Project project) {
        new QuarkusExtensionAnnotationProcessorConfigurator().configure(project);
    }

    private Configuration createDeploymentClasspath(Project project, QuarkusExtensionConfiguration quarkusExt) {
        Configuration deploymentClasspath = project.getConfigurations().create(DEPLOYMENT_CLASSPATH_CONFIGURATION_NAME);
        deploymentClasspath.setCanBeConsumed(false);
        deploymentClasspath.setCanBeResolved(true);
        deploymentClasspath.setTransitive(true);
        deploymentClasspath.getDependencies().addLater(deploymentProjectDependency(project, quarkusExt));
        return deploymentClasspath;
    }

    private Configuration createDeploymentMarker(Project project, QuarkusExtensionConfiguration quarkusExt) {
        Configuration deploymentMarker = project.getConfigurations().create(DEPLOYMENT_MARKER_CONFIGURATION_NAME);
        deploymentMarker.setCanBeConsumed(false);
        deploymentMarker.setCanBeResolved(true);
        deploymentMarker.setTransitive(false);
        deploymentMarker.getAttributes().attribute(Category.CATEGORY_ATTRIBUTE,
                project.getObjects().named(Category.class, ExtensionVariantConstants.EXTENSION_DEPLOYMENT_MARKER_CATEGORY));
        deploymentMarker.getAttributes().attribute(ExtensionVariantConstants.EXTENSION_DEPLOYMENT_ATTRIBUTE, true);
        deploymentMarker.getDependencies().addLater(deploymentProjectDependency(project, quarkusExt));
        return deploymentMarker;
    }

    private void createDeploymentDependencyElements(Project project, QuarkusExtensionConfiguration quarkusExt) {
        Configuration deploymentDependencyElements = project.getConfigurations()
                .create(DEPLOYMENT_DEPENDENCY_ELEMENTS_CONFIGURATION_NAME);
        deploymentDependencyElements.setCanBeConsumed(true);
        deploymentDependencyElements.setCanBeResolved(false);
        deploymentDependencyElements.setDescription(
                "Provides the local deployment project dependency for this Quarkus extension runtime module.");
        deploymentDependencyElements.getAttributes().attribute(Category.CATEGORY_ATTRIBUTE,
                project.getObjects().named(Category.class, ExtensionVariantConstants.EXTENSION_DEPLOYMENT_DEPENDENCY_CATEGORY));
        deploymentDependencyElements.getAttributes()
                .attribute(ExtensionVariantConstants.EXTENSION_DEPLOYMENT_DEPENDENCY_ATTRIBUTE, true);
        deploymentDependencyElements.getDependencies().addLater(deploymentProjectDependency(project, quarkusExt));
    }

    private static Provider<Dependency> deploymentProjectDependency(Project project,
            QuarkusExtensionConfiguration quarkusExt) {
        DependencyHandler dependencies = project.getDependencies();
        String projectPath = project.getPath();
        return quarkusExt.getDeploymentModule()
                .map(deploymentModule -> deploymentProjectPath(projectPath, deploymentModule))
                .map(deploymentProjectPath -> dependencies.project(Collections.singletonMap("path", deploymentProjectPath)));
    }

    private static String deploymentProjectPath(String projectPath, String deploymentModule) {
        if (deploymentModule.startsWith(":")) {
            return deploymentModule;
        }
        int lastSeparator = projectPath.lastIndexOf(':');
        if (lastSeparator <= 0) {
            return ":" + deploymentModule;
        }
        return projectPath.substring(0, lastSeparator) + ":" + deploymentModule;
    }
}
