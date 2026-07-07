package io.quarkus.gradle.application.internal.plugin;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import io.quarkus.gradle.application.internal.planning.TaskNameSegment;
import io.quarkus.gradle.application.tasks.QuarkusApplicationGenerateCodeTask;

final class KotlinGeneratedSourceWiring {

    private KotlinGeneratedSourceWiring() {
    }

    static void wireKotlinCompileTasks(Project project,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateCode,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateDevCode,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateTestCode) {
        wireCompileTask(project, "compileKotlin", generateCode, generateDevCode);
        wireCompileTask(project, "compileTestKotlin", generateTestCode, null);
    }

    static void wireKaptStubTasks(Project project,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateCode,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateDevCode,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateTestCode) {
        wireKaptStubTask(project, "kaptGenerateStubsKotlin", generateCode, generateDevCode);
        wireKaptStubTask(project, "kaptGenerateStubsTestKotlin", generateTestCode, null);
    }

    static void wireNamedTestSuite(Project project, String suiteName,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateTestCode,
            Provider<Directory> generatedTestSources) {
        String taskSegment = TaskNameSegment.of(suiteName).value();
        Provider<List<File>> generatedSources = GeneratedSourceDirectories.from(generatedTestSources);
        project.getPlugins().withId("org.jetbrains.kotlin.jvm",
                plugin -> wireCompileTask(project, "compile" + taskSegment + "Kotlin", generateTestCode, null,
                        generatedSources));
        project.getPlugins().withId("org.jetbrains.kotlin.kapt",
                plugin -> wireKaptStubTask(project, "kaptGenerateStubs" + taskSegment + "Kotlin", generateTestCode, null,
                        generatedSources));
    }

    private static void wireCompileTask(Project project, String taskName,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateTask,
            TaskProvider<QuarkusApplicationGenerateCodeTask> mustRunAfter) {
        wireCompileTask(project, taskName, generateTask, mustRunAfter,
                GeneratedSourceDirectories.from(
                        generateTask.flatMap(QuarkusApplicationGenerateCodeTask::getGeneratedOutputDirectory)));
    }

    private static void wireCompileTask(Project project, String taskName,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateTask,
            TaskProvider<QuarkusApplicationGenerateCodeTask> mustRunAfter,
            Provider<List<File>> generatedSources) {
        project.getTasks().matching(task -> task.getName().equals(taskName))
                .configureEach(task -> {
                    addGeneratedSources(task, generateTask, generatedSources);
                    if (mustRunAfter != null) {
                        task.mustRunAfter(mustRunAfter);
                    }
                });
    }

    private static void wireKaptStubTask(Project project, String taskName,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateTask,
            TaskProvider<QuarkusApplicationGenerateCodeTask> mustRunAfter) {
        wireKaptStubTask(project, taskName, generateTask, mustRunAfter,
                GeneratedSourceDirectories.from(
                        generateTask.flatMap(QuarkusApplicationGenerateCodeTask::getGeneratedOutputDirectory)));
    }

    private static void wireKaptStubTask(Project project, String taskName,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateTask,
            TaskProvider<QuarkusApplicationGenerateCodeTask> mustRunAfter,
            Provider<List<File>> generatedSources) {
        project.getTasks().matching(task -> task.getName().equals(taskName))
                .configureEach(task -> {
                    addGeneratedSources(task, generateTask, generatedSources);
                    if (mustRunAfter != null) {
                        task.mustRunAfter(mustRunAfter);
                    }
                });
    }

    private static void addGeneratedSources(Task task,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateTask,
            Provider<List<File>> generatedSourceDirectories) {
        task.dependsOn(generateTask);
        try {
            Method sourceMethod = task.getClass().getMethod("source", Object[].class);
            sourceMethod.invoke(task, (Object) new Object[] { generatedSourceDirectories });
        } catch (NoSuchMethodException e) {
            throw new GradleException("Kotlin task '" + task.getName() + "' does not expose source(Object...)", e);
        } catch (IllegalAccessException e) {
            throw new GradleException("Cannot configure generated sources for Kotlin task '" + task.getName() + "'", e);
        } catch (InvocationTargetException e) {
            throw new GradleException("Kotlin task '" + task.getName() + "' rejected Quarkus generated sources",
                    e.getCause());
        }
    }
}
