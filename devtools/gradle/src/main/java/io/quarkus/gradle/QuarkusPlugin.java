package io.quarkus.gradle;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.util.GradleVersion;

import io.quarkus.gradle.tasks.QuarkusAddExtension;
import io.quarkus.gradle.tasks.QuarkusBuild;
import io.quarkus.gradle.tasks.QuarkusDev;
import io.quarkus.gradle.tasks.QuarkusGenerateConfig;
import io.quarkus.gradle.tasks.QuarkusListExtensions;
import io.quarkus.gradle.tasks.QuarkusNative;
import io.quarkus.gradle.tasks.QuarkusTestConfig;
import io.quarkus.gradle.tasks.QuarkusTestNative;

public class QuarkusPlugin implements Plugin<Project> {

    public static final String ID = "io.quarkus";

    public static final String EXTENSION_NAME = "quarkus";
    public static final String LIST_EXTENSIONS_TASK_NAME = "listExtensions";
    public static final String ADD_EXTENSION_TASK_NAME = "addExtension";
    public static final String QUARKUS_BUILD_TASK_NAME = "quarkusBuild";
    public static final String GENERATE_CONFIG_TASK_NAME = "generateConfig";
    public static final String QUARKUS_DEV_TASK_NAME = "quarkusDev";
    public static final String BUILD_NATIVE_TASK_NAME = "buildNative";
    public static final String TEST_NATIVE_TASK_NAME = "testNative";
    public static final String QUARKUS_TEST_CONFIG_TASK_NAME = "quarkusTestConfig";

    // this name has to be the same as the directory in which the tests reside
    public static final String NATIVE_TEST_SOURCE_SET_NAME = "native-test";

    public static final String NATIVE_TEST_IMPLEMENTATION_CONFIGURATION_NAME = "nativeTestImplementation";
    public static final String NATIVE_TEST_RUNTIME_ONLY_CONFIGURATION_NAME = "nativeTestRuntimeOnly";

    private QuarkusPluginExtension extension;

    @Override
    public void apply(Project project) {
        verifyGradleVersion();

        // register extension
        extension = project.getExtensions().create(EXTENSION_NAME, QuarkusPluginExtension.class, project);

        registerTasks(project);
    }

    private void registerTasks(Project project) {
        TaskContainer tasks = project.getTasks();
        tasks.create(LIST_EXTENSIONS_TASK_NAME, QuarkusListExtensions.class);
        tasks.create(ADD_EXTENSION_TASK_NAME, QuarkusAddExtension.class);
        tasks.create(GENERATE_CONFIG_TASK_NAME, QuarkusGenerateConfig.class);

        Task quarkusBuild = tasks.create(QUARKUS_BUILD_TASK_NAME, QuarkusBuild.class);
        Task quarkusDev = tasks.create(QUARKUS_DEV_TASK_NAME, QuarkusDev.class);
        Task buildNative = tasks.create(BUILD_NATIVE_TASK_NAME, QuarkusNative.class);
        Task quarkusTestConfig = tasks.create(QUARKUS_TEST_CONFIG_TASK_NAME, QuarkusTestConfig.class);

        project.getPlugins().withType(
                BasePlugin.class,
                basePlugin -> tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(quarkusBuild));
        project.getPlugins().withType(
                JavaPlugin.class,
                javaPlugin -> {
                    project.afterEvaluate(this::afterEvaluate);

                    Task classesTask = tasks.getByName(JavaPlugin.CLASSES_TASK_NAME);
                    quarkusDev.dependsOn(classesTask);
                    quarkusBuild.dependsOn(classesTask, tasks.getByName(JavaPlugin.JAR_TASK_NAME));
                    quarkusTestConfig.dependsOn(classesTask);

                    buildNative.dependsOn(tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME));

                    SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class)
                            .getSourceSets();
                    SourceSet nativeTestSourceSet = sourceSets.create(NATIVE_TEST_SOURCE_SET_NAME);
                    SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                    SourceSet testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);

                    nativeTestSourceSet.setCompileClasspath(
                            nativeTestSourceSet.getCompileClasspath()
                                    .plus(mainSourceSet.getOutput())
                                    .plus(testSourceSet.getOutput()));

                    nativeTestSourceSet.setRuntimeClasspath(
                            nativeTestSourceSet.getRuntimeClasspath()
                                    .plus(mainSourceSet.getOutput())
                                    .plus(testSourceSet.getOutput()));

                    // create a custom configuration to be used for the dependencies of the testNative task
                    ConfigurationContainer configurations = project.getConfigurations();
                    configurations.maybeCreate(NATIVE_TEST_IMPLEMENTATION_CONFIGURATION_NAME)
                            .extendsFrom(configurations.findByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME));
                    configurations.maybeCreate(NATIVE_TEST_RUNTIME_ONLY_CONFIGURATION_NAME)
                            .extendsFrom(configurations.findByName(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME));

                    Task testNative = tasks.create(TEST_NATIVE_TASK_NAME, QuarkusTestNative.class);
                    testNative.dependsOn(buildNative);
                    testNative.setShouldRunAfter(Collections.singletonList(tasks.findByName(JavaPlugin.TEST_TASK_NAME)));

                    Consumer<Test> configureTestTask = t -> {
                        // Quarkus test configuration task which should be executed before any Quarkus test
                        t.dependsOn(quarkusTestConfig);
                        // also make each task use the JUnit platform since it's the only supported test environment
                        t.useJUnitPlatform();
                    };
                    tasks.withType(Test.class).forEach(configureTestTask);
                    tasks.withType(Test.class).whenTaskAdded(configureTestTask::accept);
                });
    }

    private void verifyGradleVersion() {
        if (GradleVersion.current().compareTo(GradleVersion.version("5.0")) < 0) {
            throw new GradleException("Quarkus plugin requires Gradle 5.0 or later. Current version is: " +
                    GradleVersion.current());
        }
    }

    private void afterEvaluate(Project project) {
        project.getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
                .getIncoming().getDependencies()
                .forEach(d -> {
                    if (d instanceof ProjectDependency) {
                        configProjectDependency(project, ((ProjectDependency) d).getDependencyProject());
                    }
                });
    }

    private void configProjectDependency(Project project, Project dep) {
        if (dep.getState().getExecuted()) {
            setupTaskDependencies(project, dep);
        } else {
            dep.afterEvaluate(p -> {
                setupTaskDependencies(project, p);
            });
        }
    }

    private void setupTaskDependencies(Project project, Project dep) {
        project.getLogger().debug("Configuring {} task dependencies on {} tasks", project, dep);
        try {
            final Task jarTask = dep.getTasks().getByName(JavaPlugin.JAR_TASK_NAME);
            final Task quarkusBuild = findTask(project.getTasks(), QUARKUS_BUILD_TASK_NAME);
            if (quarkusBuild != null) {
                quarkusBuild.dependsOn(jarTask);
            }
            extension.addProjectDepJarTask(dep, jarTask);
        } catch (UnknownTaskException e) {
            project.getLogger().debug("Project {} does not include {} task", dep, JavaPlugin.JAR_TASK_NAME, e);
        }
        for (Map.Entry<String, Project> entry : dep.getChildProjects().entrySet()) {
            configProjectDependency(project, entry.getValue());
        }
    }

    private static Task findTask(TaskContainer tasks, String name) {
        try {
            return tasks.findByName(name);
        } catch (UnknownTaskException e) {
            return null;
        }
    }
}
