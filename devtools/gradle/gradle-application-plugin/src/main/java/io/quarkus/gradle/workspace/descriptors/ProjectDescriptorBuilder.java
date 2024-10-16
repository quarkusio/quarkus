package io.quarkus.gradle.workspace.descriptors;

import static io.quarkus.gradle.workspace.descriptors.ProjectDescriptor.TaskType.COMPILE;
import static io.quarkus.gradle.workspace.descriptors.ProjectDescriptor.TaskType.RESOURCES;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile;

import com.google.common.collect.ImmutableSet;

public class ProjectDescriptorBuilder {

    private static final Set<String> DEPENDENCY_SOURCE_SETS = ImmutableSet.of(SourceSet.MAIN_SOURCE_SET_NAME,
            SourceSet.TEST_SOURCE_SET_NAME, "test-fixtures");

    private final File projectDir;
    private final File buildDir;
    private final File buildFile;
    private final Map<String, QuarkusTaskDescriptor> tasks;
    private final Map<String, Set<String>> sourceSetTasks;
    private final Set<String> collectOnlySourceSets;

    private ProjectDescriptorBuilder(Project project, Set<String> collectOnlySourceSets) {
        this.tasks = new LinkedHashMap<>();
        this.sourceSetTasks = new LinkedHashMap<>();
        this.buildFile = project.getBuildFile();
        this.projectDir = project.getLayout().getProjectDirectory().getAsFile();
        this.buildDir = project.getLayout().getBuildDirectory().get().getAsFile();
        this.collectOnlySourceSets = collectOnlySourceSets;
    }

    public static Provider<DefaultProjectDescriptor> buildForApp(Project target) {
        ProjectDescriptorBuilder builder = new ProjectDescriptorBuilder(target, Collections.emptySet());
        target.afterEvaluate(project -> {
            project.getTasks().withType(AbstractCompile.class)
                    .configureEach(builder::readConfigurationFor);
            builder.withKotlinJvmCompileType(project);
            project.getTasks().withType(ProcessResources.class)
                    .configureEach(builder::readConfigurationFor);
            project.getTasks().withType(Test.class)
                    .configureEach(builder::readConfigurationFor);
        });
        return target.getProviders().provider(() -> new DefaultProjectDescriptor(
                builder.projectDir,
                builder.buildDir,
                builder.buildFile,
                builder.tasks,
                builder.sourceSetTasks));
    }

    public static Provider<DefaultProjectDescriptor> buildForDependency(Project target) {
        ProjectDescriptorBuilder builder = new ProjectDescriptorBuilder(target, DEPENDENCY_SOURCE_SETS);
        target.afterEvaluate(project -> {
            project.getTasks().withType(AbstractCompile.class)
                    .configureEach(builder::readConfigurationFor);
            builder.withKotlinJvmCompileType(project);
            project.getTasks().withType(ProcessResources.class)
                    .configureEach(builder::readConfigurationFor);
        });
        return target.getProviders().provider(() -> new DefaultProjectDescriptor(
                builder.projectDir,
                builder.buildDir,
                builder.buildFile,
                builder.tasks,
                builder.sourceSetTasks));
    }

    private void readConfigurationFor(AbstractCompile task) {
        if (task.getEnabled() && !task.getSource().isEmpty()) {
            File destDir = task.getDestinationDirectory().getAsFile().get();
            task.getSource().visit(fileVisitDetails -> {
                if (fileVisitDetails.getRelativePath().getParent().toString().isEmpty()) {
                    File srcDir = fileVisitDetails.getFile().getParentFile();
                    tasks.put(task.getName(), new QuarkusTaskDescriptor(task.getName(), COMPILE, srcDir, destDir));
                    SourceSetContainer sourceSets = task.getProject().getExtensions().getByType(SourceSetContainer.class);
                    sourceSets.stream().filter(sourceSet -> sourceSet.getOutput().getClassesDirs().contains(destDir))
                            .forEach(sourceSet -> sourceSetTasks
                                    .computeIfAbsent(sourceSet.getName(), s -> new HashSet<>())
                                    .add(task.getName()));
                    fileVisitDetails.stopVisiting();
                }
            });
        }
    }

    private void readConfigurationFor(Test task) {
    }

    private void readConfigurationFor(ProcessResources task) {
        if (task.getEnabled() && !task.getSource().isEmpty()) {
            File destDir = task.getDestinationDir();
            task.getSource().getAsFileTree().visit(fileVisitDetails -> {
                if (fileVisitDetails.getRelativePath().getParent().toString().isEmpty()) {
                    File srcDir = fileVisitDetails.getFile().getParentFile();
                    tasks.put(task.getName(), new QuarkusTaskDescriptor(task.getName(), RESOURCES, srcDir, destDir));
                    SourceSetContainer sourceSets = task.getProject().getExtensions().getByType(SourceSetContainer.class);
                    sourceSets.stream().filter(sourceSet -> destDir.equals(sourceSet.getOutput().getResourcesDir()))
                            .forEach(sourceSet -> sourceSetTasks
                                    .computeIfAbsent(sourceSet.getName(), s -> new HashSet<>())
                                    .add(task.getName()));
                    fileVisitDetails.stopVisiting();
                }
            });
        }
    }

    private void withKotlinJvmCompileType(Project project) {
        try {
            Class.forName("org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile");
            project.getTasks().withType(KotlinJvmCompile.class).configureEach(this::readConfigurationFor);
        } catch (ClassNotFoundException e) {
            // Ignore
        }
    }

    private void readConfigurationFor(KotlinJvmCompile task) {
        if (task.getEnabled() && !task.getSources().isEmpty()) {
            File destDir = task.getDestinationDirectory().getAsFile().get();
            AtomicReference<File> srcDir = new AtomicReference<>();
            task.getSources().getAsFileTree().visit(fileVisitDetails -> {
                if (fileVisitDetails.getRelativePath().getParent().toString().isEmpty()) {
                    srcDir.set(fileVisitDetails.getFile().getParentFile());
                    fileVisitDetails.stopVisiting();
                }
            });
            tasks.put(task.getName(), new QuarkusTaskDescriptor(task.getName(), COMPILE, srcDir.get(), destDir));
            SourceSetContainer sourceSets = task.getProject().getExtensions().getByType(SourceSetContainer.class);
            sourceSets.stream().filter(sourceSet -> sourceSet.getOutput().getClassesDirs().contains(destDir))
                    .forEach(sourceSet -> sourceSetTasks
                            .computeIfAbsent(sourceSet.getName(), s -> new HashSet<>())
                            .add(task.getName()));
        }
    }
}
