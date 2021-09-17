package io.quarkus.extension.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskContainer;

import io.quarkus.extension.gradle.tasks.ExtensionDescriptorTask;

public class QuarkusExtensionPlugin implements Plugin<Project> {

    public static final String EXTENSION_DESCRIPTOR_TASK_NAME = "extensionDescriptor";

    @Override
    public void apply(Project project) {
        registerTasks(project);
    }

    private void registerTasks(Project project) {
        TaskContainer tasks = project.getTasks();
        ExtensionDescriptorTask extensionDescriptorTask = tasks.create(EXTENSION_DESCRIPTOR_TASK_NAME,
                ExtensionDescriptorTask.class);

        project.getPlugins().withType(
                JavaPlugin.class,
                javaPlugin -> {
                    Task jarTask = tasks.getByName(JavaPlugin.JAR_TASK_NAME);
                    jarTask.dependsOn(extensionDescriptorTask);
                });
    }
}
