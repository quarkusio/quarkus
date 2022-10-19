package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.extension.QuarkusPluginExtension;

public class QuarkusPluginTest {

    @Test
    public void shouldCreateTasks() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusPlugin.ID);

        assertTrue(project.getPluginManager().hasPlugin(QuarkusPlugin.ID));

        TaskContainer tasks = project.getTasks();
        assertNotNull(tasks.getByName(QuarkusPlugin.QUARKUS_BUILD_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.QUARKUS_DEV_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.BUILD_NATIVE_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.LIST_EXTENSIONS_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.ADD_EXTENSION_TASK_NAME));
    }

    @Test
    public void shouldMakeAssembleDependOnQuarkusBuild() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusPlugin.ID);
        project.getPluginManager().apply("base");

        TaskContainer tasks = project.getTasks();
        Task assemble = tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME);
        assertThat(getDependantProvidedTaskName(assemble))
                .contains(QuarkusPlugin.QUARKUS_BUILD_TASK_NAME);
    }

    @Test
    public void shouldMakeQuarkusDevAndQuarkusBuildDependOnClassesTask() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusPlugin.ID);
        project.getPluginManager().apply("java");

        TaskContainer tasks = project.getTasks();

        Task quarkusBuild = tasks.getByName(QuarkusPlugin.QUARKUS_BUILD_TASK_NAME);
        assertThat(getDependantProvidedTaskName(quarkusBuild))
                .contains(JavaPlugin.CLASSES_TASK_NAME);

        Task quarkusDev = tasks.getByName(QuarkusPlugin.QUARKUS_DEV_TASK_NAME);
        assertThat(getDependantProvidedTaskName(quarkusDev))
                .contains(JavaPlugin.CLASSES_TASK_NAME);
    }

    @Test
    public void shouldReturnMutlipleOutputSourceDirectories() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusPlugin.ID);
        project.getPluginManager().apply("java");
        project.getPluginManager().apply("scala");

        final QuarkusPluginExtension extension = project.getExtensions().getByType(QuarkusPluginExtension.class);

        final Set<File> outputSourceDirs = extension.combinedOutputSourceDirs();
        assertThat(outputSourceDirs).hasSize(4);
        assertThat(outputSourceDirs).contains(new File(project.getBuildDir(), "classes/java/main"),
                new File(project.getBuildDir(), "classes/java/test"),
                new File(project.getBuildDir(), "classes/scala/main"),
                new File(project.getBuildDir(), "classes/scala/test"));

    }

    private static final List<String> getDependantProvidedTaskName(Task task) {
        List<String> dependantTaskNames = new ArrayList<>();
        for (Object t : task.getDependsOn()) {
            try {
                dependantTaskNames.add(((Provider<Task>) t).get().getName());
            } catch (ClassCastException e) {
                // Nothing to do here
            }
        }
        return dependantTaskNames;
    }
}
