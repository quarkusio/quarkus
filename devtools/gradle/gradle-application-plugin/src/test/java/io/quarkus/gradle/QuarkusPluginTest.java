package io.quarkus.gradle;

import static io.quarkus.gradle.QuarkusPlugin.QUARKUS_BUILD_TASK_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.extension.QuarkusPluginExtension;

public class QuarkusPluginTest {

    @Test
    public void shouldCreateTasks() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusPlugin.ID);

        assertTrue(project.getPluginManager().hasPlugin(QuarkusPlugin.ID));

        TaskContainer tasks = project.getTasks();
        assertNotNull(tasks.getByName(QuarkusPlugin.QUARKUS_BUILD_APP_PARTS_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.QUARKUS_BUILD_DEP_TASK_NAME));
        assertNotNull(tasks.getByName(QUARKUS_BUILD_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.QUARKUS_DEV_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.BUILD_NATIVE_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.LIST_EXTENSIONS_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.ADD_EXTENSION_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.IMAGE_BUILD_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.IMAGE_PUSH_TASK_NAME));
        assertNotNull(tasks.getByName(QuarkusPlugin.DEPLOY_TASK_NAME));
    }

    @Test
    public void shouldMakeAssembleDependOnQuarkusBuild() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusPlugin.ID);
        project.getPluginManager().apply("base");

        TaskContainer tasks = project.getTasks();
        Task assemble = tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME);
        assertThat(getDependantProvidedTaskName(assemble))
                .contains(QUARKUS_BUILD_TASK_NAME);
    }

    @Test
    public void shouldMakeQuarkusDevAndQuarkusBuildDependOnClassesTask() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusPlugin.ID);
        project.getPluginManager().apply("java");

        TaskContainer tasks = project.getTasks();

        Task quarkusAppPartsBuild = tasks.getByName(QuarkusPlugin.QUARKUS_BUILD_APP_PARTS_TASK_NAME);
        assertThat(getDependantProvidedTaskName(quarkusAppPartsBuild))
                .contains(JavaPlugin.CLASSES_TASK_NAME)
                .contains(QuarkusPlugin.QUARKUS_GENERATE_CODE_TASK_NAME);

        Task quarkusDepBuild = tasks.getByName(QuarkusPlugin.QUARKUS_BUILD_DEP_TASK_NAME);
        assertThat(getDependantProvidedTaskName(quarkusDepBuild))
                .isEmpty();

        Task quarkusBuild = tasks.getByName(QUARKUS_BUILD_TASK_NAME);
        assertThat(getDependantProvidedTaskName(quarkusBuild))
                .contains(QuarkusPlugin.QUARKUS_BUILD_APP_PARTS_TASK_NAME)
                .contains(QuarkusPlugin.QUARKUS_BUILD_APP_PARTS_TASK_NAME);

        Task quarkusDev = tasks.getByName(QuarkusPlugin.QUARKUS_DEV_TASK_NAME);
    }

    @Test
    public void shouldReturnMultipleOutputSourceDirectories() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusPlugin.ID);
        project.getPluginManager().apply("java");
        project.getPluginManager().apply("scala");

        final QuarkusPluginExtension extension = project.getExtensions().getByType(QuarkusPluginExtension.class);

        final Set<File> outputSourceDirs = extension.combinedOutputSourceDirs();
        assertThat(outputSourceDirs).hasSize(4);
        assertThat(outputSourceDirs).contains(
                new File(project.getLayout().getBuildDirectory().getAsFile().get(), "classes/java/main"),
                new File(project.getLayout().getBuildDirectory().getAsFile().get(), "classes/java/test"),
                new File(project.getLayout().getBuildDirectory().getAsFile().get(), "classes/scala/main"),
                new File(project.getLayout().getBuildDirectory().getAsFile().get(), "classes/scala/test"));
    }

    @Test
    public void shouldNotFailOnProjectDependenciesWithoutMain(@TempDir Path testProjectDir) throws IOException {
        var kotlinVersion = System.getProperty("kotlin_version", "2.2.0");
        var settingFile = testProjectDir.resolve("settings.gradle");
        var mppProjectDir = testProjectDir.resolve("mpp");
        var quarkusProjectDir = testProjectDir.resolve("quarkus");
        var mppBuild = mppProjectDir.resolve("build.gradle");
        var quarkusBuild = quarkusProjectDir.resolve("build.gradle");
        Files.createDirectory(mppProjectDir);
        Files.createDirectory(quarkusProjectDir);
        Files.writeString(settingFile, """
                pluginManagement {
                    plugins {
                        id 'org.jetbrains.kotlin.jvm' version "%1$s"
                        id 'org.jetbrains.kotlin.plugin.allopen' version "%1$s"
                    }
                }

                rootProject.name = "quarkus-mpp-sample"

                include(
                    "mpp",
                    "quarkus"
                )
                """.formatted(kotlinVersion));

        Files.writeString(mppBuild, """
                plugins {
                    id 'org.jetbrains.kotlin.jvm'
                }

                compileKotlin {
                    kotlinOptions.javaParameters = true
                }""");

        Files.writeString(quarkusBuild, """
                plugins {
                    id("io.quarkus")
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    implementation(project(":mpp"))
                }""");

        BuildResult result = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("quarkusGenerateCode", "--stacktrace")
                .build();

        assertEquals(SUCCESS, result.task(":quarkus:quarkusGenerateCode").getOutcome());
    }

    @Test
    public void analyticsAfterBuild() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusPlugin.ID);

        TaskContainer tasks = project.getTasks();
        Task quarkusBuild = tasks.getByName(QUARKUS_BUILD_TASK_NAME);
    }

    private static List<String> getDependantProvidedTaskName(Task task) {
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
