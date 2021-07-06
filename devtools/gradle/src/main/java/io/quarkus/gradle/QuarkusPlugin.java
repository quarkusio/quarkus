package io.quarkus.gradle;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;
import org.gradle.util.GradleVersion;

import io.quarkus.gradle.builder.QuarkusModelBuilder;
import io.quarkus.gradle.extension.QuarkusPluginExtension;
import io.quarkus.gradle.extension.SourceSetExtension;
import io.quarkus.gradle.tasks.QuarkusAddExtension;
import io.quarkus.gradle.tasks.QuarkusBuild;
import io.quarkus.gradle.tasks.QuarkusDev;
import io.quarkus.gradle.tasks.QuarkusGenerateCode;
import io.quarkus.gradle.tasks.QuarkusGenerateConfig;
import io.quarkus.gradle.tasks.QuarkusListCategories;
import io.quarkus.gradle.tasks.QuarkusListExtensions;
import io.quarkus.gradle.tasks.QuarkusListPlatforms;
import io.quarkus.gradle.tasks.QuarkusRemoteDev;
import io.quarkus.gradle.tasks.QuarkusRemoveExtension;
import io.quarkus.gradle.tasks.QuarkusTest;
import io.quarkus.gradle.tasks.QuarkusTestConfig;
import io.quarkus.gradle.tasks.QuarkusTestNative;

public class QuarkusPlugin implements Plugin<Project> {

    public static final String ID = "io.quarkus";
    public static final String QUARKUS_PACKAGE_TYPE = "quarkus.package.type";

    public static final String EXTENSION_NAME = "quarkus";
    public static final String LIST_EXTENSIONS_TASK_NAME = "listExtensions";
    public static final String LIST_CATEGORIES_TASK_NAME = "listCategories";
    public static final String LIST_PLATFORMS_TASK_NAME = "listPlatforms";
    public static final String ADD_EXTENSION_TASK_NAME = "addExtension";
    public static final String REMOVE_EXTENSION_TASK_NAME = "removeExtension";
    public static final String QUARKUS_GENERATE_CODE_TASK_NAME = "quarkusGenerateCode";
    public static final String QUARKUS_GENERATE_CODE_TESTS_TASK_NAME = "quarkusGenerateCodeTests";
    public static final String QUARKUS_BUILD_TASK_NAME = "quarkusBuild";
    public static final String GENERATE_CONFIG_TASK_NAME = "generateConfig";
    public static final String QUARKUS_DEV_TASK_NAME = "quarkusDev";
    public static final String QUARKUS_REMOTE_DEV_TASK_NAME = "quarkusRemoteDev";
    public static final String QUARKUS_TEST_TASK_NAME = "quarkusTest";
    public static final String DEV_MODE_CONFIGURATION_NAME = "quarkusDev";
    public static final String ANNOTATION_PROCESSOR_CONFIGURATION_NAME = "quarkusAnnotationProcessor";

    @Deprecated
    public static final String BUILD_NATIVE_TASK_NAME = "buildNative";
    public static final String TEST_NATIVE_TASK_NAME = "testNative";
    @Deprecated
    public static final String QUARKUS_TEST_CONFIG_TASK_NAME = "quarkusTestConfig";

    // this name has to be the same as the directory in which the tests reside
    public static final String NATIVE_TEST_SOURCE_SET_NAME = "native-test";

    public static final String NATIVE_TEST_IMPLEMENTATION_CONFIGURATION_NAME = "nativeTestImplementation";
    public static final String NATIVE_TEST_RUNTIME_ONLY_CONFIGURATION_NAME = "nativeTestRuntimeOnly";

    private final ToolingModelBuilderRegistry registry;

    @Inject
    public QuarkusPlugin(ToolingModelBuilderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void apply(Project project) {
        verifyGradleVersion();
        registerModel();
        // register extension
        final QuarkusPluginExtension quarkusExt = project.getExtensions().create(EXTENSION_NAME, QuarkusPluginExtension.class,
                project);
        registerTasks(project, quarkusExt);
    }

    private void registerTasks(Project project, QuarkusPluginExtension quarkusExt) {
        TaskContainer tasks = project.getTasks();
        tasks.create(LIST_EXTENSIONS_TASK_NAME, QuarkusListExtensions.class);
        tasks.create(LIST_CATEGORIES_TASK_NAME, QuarkusListCategories.class);
        tasks.create(LIST_PLATFORMS_TASK_NAME, QuarkusListPlatforms.class);
        tasks.create(ADD_EXTENSION_TASK_NAME, QuarkusAddExtension.class);
        tasks.create(REMOVE_EXTENSION_TASK_NAME, QuarkusRemoveExtension.class);
        tasks.create(GENERATE_CONFIG_TASK_NAME, QuarkusGenerateConfig.class);

        QuarkusGenerateCode quarkusGenerateCode = tasks.create(QUARKUS_GENERATE_CODE_TASK_NAME, QuarkusGenerateCode.class);
        QuarkusGenerateCode quarkusGenerateCodeTests = tasks.create(QUARKUS_GENERATE_CODE_TESTS_TASK_NAME,
                QuarkusGenerateCode.class);
        quarkusGenerateCodeTests.setTest(true);

        Task quarkusBuild = tasks.create(QUARKUS_BUILD_TASK_NAME, QuarkusBuild.class);
        quarkusBuild.dependsOn(quarkusGenerateCode);
        Task quarkusDev = tasks.create(QUARKUS_DEV_TASK_NAME, QuarkusDev.class);
        Task quarkusRemoteDev = tasks.create(QUARKUS_REMOTE_DEV_TASK_NAME, QuarkusRemoteDev.class);
        Task quarkusTest = tasks.create(QUARKUS_TEST_TASK_NAME, QuarkusTest.class);
        tasks.create(QUARKUS_TEST_CONFIG_TASK_NAME, QuarkusTestConfig.class);

        Task buildNative = tasks.create(BUILD_NATIVE_TASK_NAME, DefaultTask.class);
        buildNative.finalizedBy(quarkusBuild);
        buildNative.doFirst(t -> project.getLogger()
                .warn("The 'buildNative' task has been deprecated in favor of 'build -Dquarkus.package.type=native'"));

        configureBuildNativeTask(project);

        final Consumer<Test> configureTestTask = t -> {
            // Quarkus test configuration action which should be executed before any Quarkus test
            // Use anonymous classes in order to leverage task avoidance.
            t.doFirst(new Action<Task>() {
                @Override
                public void execute(Task test) {
                    quarkusExt.beforeTest(t);
                }
            });
            // also make each task use the JUnit platform since it's the only supported test environment
            t.useJUnitPlatform();
            // quarkusBuild is expected to run after the project has passed the tests
            quarkusBuild.shouldRunAfter(t);
        };

        project.getPlugins().withType(
                BasePlugin.class,
                basePlugin -> tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(quarkusBuild));
        project.getPlugins().withType(
                JavaPlugin.class,
                javaPlugin -> {
                    project.afterEvaluate(this::afterEvaluate);
                    ConfigurationContainer configurations = project.getConfigurations();
                    JavaCompile compileJavaTask = (JavaCompile) tasks.getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);

                    // By default, gradle looks for annotation processors in the annotationProcessor configuration.
                    // This configure the compile task to look for annotation processors in the compileClasspath.
                    Configuration annotationProcessorConfig = configurations.create(ANNOTATION_PROCESSOR_CONFIGURATION_NAME)
                            .extendsFrom(
                                    configurations.getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME),
                                    configurations.getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME));
                    compileJavaTask.getOptions().setAnnotationProcessorPath(annotationProcessorConfig);

                    compileJavaTask.dependsOn(quarkusGenerateCode);

                    JavaCompile compileTestJavaTask = (JavaCompile) tasks.getByName(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME);
                    compileTestJavaTask.dependsOn(quarkusGenerateCodeTests);

                    Task classesTask = tasks.getByName(JavaPlugin.CLASSES_TASK_NAME);
                    Task resourcesTask = tasks.getByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME);
                    Task testClassesTask = tasks.getByName(JavaPlugin.TEST_CLASSES_TASK_NAME);
                    Task testResourcesTask = tasks.getByName(JavaPlugin.PROCESS_TEST_RESOURCES_TASK_NAME);
                    quarkusDev.dependsOn(classesTask, resourcesTask, testClassesTask, testResourcesTask, quarkusGenerateCode,
                            quarkusGenerateCodeTests);
                    quarkusRemoteDev.dependsOn(classesTask, resourcesTask);
                    quarkusTest.dependsOn(classesTask, resourcesTask, testClassesTask, testResourcesTask, quarkusGenerateCode,
                            quarkusGenerateCodeTests);
                    quarkusBuild.dependsOn(classesTask, resourcesTask, tasks.getByName(JavaPlugin.JAR_TASK_NAME));

                    SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class)
                            .getSourceSets();
                    SourceSet nativeTestSourceSet = sourceSets.create(NATIVE_TEST_SOURCE_SET_NAME);
                    SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                    SourceSet testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);

                    quarkusGenerateCode.setSourcesDirectories(getSourcesParents(mainSourceSet));
                    quarkusGenerateCodeTests.setSourcesDirectories(getSourcesParents(testSourceSet));

                    nativeTestSourceSet.setCompileClasspath(
                            nativeTestSourceSet.getCompileClasspath()
                                    .plus(mainSourceSet.getOutput())
                                    .plus(testSourceSet.getOutput()));

                    nativeTestSourceSet.setRuntimeClasspath(
                            nativeTestSourceSet.getRuntimeClasspath()
                                    .plus(mainSourceSet.getOutput())
                                    .plus(testSourceSet.getOutput()));

                    // create a custom configuration for devmode
                    configurations.create(DEV_MODE_CONFIGURATION_NAME).extendsFrom(
                            configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME),
                            configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));

                    // create a custom configuration to be used for the dependencies of the testNative task
                    configurations.maybeCreate(NATIVE_TEST_IMPLEMENTATION_CONFIGURATION_NAME)
                            .extendsFrom(configurations.findByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME));
                    configurations.maybeCreate(NATIVE_TEST_RUNTIME_ONLY_CONFIGURATION_NAME)
                            .extendsFrom(configurations.findByName(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME));

                    Task testNative = tasks.create(TEST_NATIVE_TASK_NAME, QuarkusTestNative.class);
                    testNative.dependsOn(quarkusBuild);
                    testNative.setShouldRunAfter(Collections.singletonList(tasks.findByName(JavaPlugin.TEST_TASK_NAME)));

                    tasks.withType(Test.class).forEach(configureTestTask);
                    tasks.withType(Test.class).whenTaskAdded(configureTestTask::accept);

                    SourceSet generatedSourceSet = sourceSets.create(QuarkusGenerateCode.QUARKUS_GENERATED_SOURCES);
                    SourceSet generatedTestSourceSet = sourceSets.create(QuarkusGenerateCode.QUARKUS_TEST_GENERATED_SOURCES);

                    // Register the quarkus-generated-code
                    for (String provider : QuarkusGenerateCode.CODE_GENERATION_PROVIDER) {
                        mainSourceSet.getJava().srcDir(new File(generatedSourceSet.getJava().getOutputDir(), provider));
                        testSourceSet.getJava().srcDir(new File(generatedTestSourceSet.getJava().getOutputDir(), provider));
                    }

                });

        project.getPlugins().withId("org.jetbrains.kotlin.jvm", plugin -> {
            tasks.getByName("compileKotlin").dependsOn(quarkusGenerateCode);
            tasks.getByName("compileTestKotlin").dependsOn(quarkusGenerateCodeTests);
        });
    }

    private Set<Path> getSourcesParents(SourceSet mainSourceSet) {
        Set<File> srcDirs = mainSourceSet.getJava().getSrcDirs();
        return srcDirs.stream()
                .map(File::toPath)
                .map(Path::getParent)
                .collect(Collectors.toSet());
    }

    private void registerModel() {
        registry.register(new QuarkusModelBuilder());
    }

    private void verifyGradleVersion() {
        if (GradleVersion.current().compareTo(GradleVersion.version("5.0")) < 0) {
            throw new GradleException("Quarkus plugin requires Gradle 5.0 or later. Current version is: " +
                    GradleVersion.current());
        }
    }

    private void configureBuildNativeTask(Project project) {
        project.getGradle().getTaskGraph().whenReady(taskGraph -> {
            if (taskGraph.hasTask(project.getPath() + BUILD_NATIVE_TASK_NAME)
                    || taskGraph.hasTask(project.getPath() + TEST_NATIVE_TASK_NAME)) {
                project.getExtensions().getExtraProperties()
                        .set(QUARKUS_PACKAGE_TYPE, "native");
            }
        });
    }

    private void afterEvaluate(Project project) {
        final HashSet<String> visited = new HashSet<>();
        ConfigurationContainer configurations = project.getConfigurations();
        configurations.getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)
                .getIncoming().getDependencies()
                .forEach(d -> {
                    if (d instanceof ProjectDependency) {
                        visitProjectDep(project, ((ProjectDependency) d).getDependencyProject(), visited);
                    }
                });

        SourceSetExtension sourceSetExtension = project.getExtensions().getByType(QuarkusPluginExtension.class)
                .sourceSetExtension();

        if (sourceSetExtension.extraNativeTest() != null) {

            SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class)
                    .getSourceSets();
            SourceSet nativeTestSourceSets = sourceSets.getByName(NATIVE_TEST_SOURCE_SET_NAME);
            nativeTestSourceSets.setCompileClasspath(
                    nativeTestSourceSets.getCompileClasspath().plus(sourceSetExtension.extraNativeTest().getOutput()));
            nativeTestSourceSets.setRuntimeClasspath(
                    nativeTestSourceSets.getRuntimeClasspath().plus(sourceSetExtension.extraNativeTest().getOutput()));

            configurations.findByName(NATIVE_TEST_IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(
                    configurations.findByName(sourceSetExtension.extraNativeTest().getImplementationConfigurationName()));
            configurations.findByName(NATIVE_TEST_RUNTIME_ONLY_CONFIGURATION_NAME).extendsFrom(
                    configurations.findByName(sourceSetExtension.extraNativeTest().getRuntimeOnlyConfigurationName()));

            QuarkusTestNative nativeTest = (QuarkusTestNative) project.getTasks().getByName(TEST_NATIVE_TASK_NAME);
            nativeTest.setTestClassesDirs(nativeTestSourceSets.getOutput().getClassesDirs());
            nativeTest.setClasspath(nativeTestSourceSets.getRuntimeClasspath());
        }
    }

    private void visitProjectDep(Project project, Project dep, Set<String> visited) {
        if (dep.getState().getExecuted()) {
            setupQuarkusBuildTaskDeps(project, dep, visited);
        } else {
            dep.afterEvaluate(p -> {
                setupQuarkusBuildTaskDeps(project, p, visited);
            });
        }
    }

    private void setupQuarkusBuildTaskDeps(Project project, Project dep, Set<String> visited) {
        if (!visited.add(dep.getPath())) {
            return;
        }
        project.getLogger().debug("Configuring {} task dependencies on {} tasks", project, dep);

        final Task quarkusBuild = project.getTasks().findByName(QUARKUS_BUILD_TASK_NAME);
        if (quarkusBuild != null) {
            final Task jarTask = dep.getTasks().findByName(JavaPlugin.JAR_TASK_NAME);
            if (jarTask != null) {
                final Task quarkusPrepare = project.getTasks().findByName(QUARKUS_GENERATE_CODE_TASK_NAME);
                final Task quarkusPrepareTests = project.getTasks().findByName(QUARKUS_GENERATE_CODE_TESTS_TASK_NAME);
                quarkusBuild.dependsOn(jarTask);
                if (quarkusPrepare != null) {
                    quarkusPrepare.dependsOn(jarTask);
                }
                if (quarkusPrepareTests != null) {
                    quarkusPrepareTests.dependsOn(jarTask);
                }
            }
        }

        final Task quarkusDev = project.getTasks().findByName(QUARKUS_DEV_TASK_NAME);
        if (quarkusDev != null) {
            final Task resourcesTask = dep.getTasks().findByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME);
            if (resourcesTask != null) {
                quarkusDev.dependsOn(resourcesTask);
            }
            final Task resourcesTaskJandex = dep.getTasks().findByName("jandex");
            if (resourcesTaskJandex != null) {
                quarkusDev.dependsOn(resourcesTaskJandex);
            }

        }

        final Configuration compileConfig = dep.getConfigurations().findByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
        if (compileConfig != null) {
            compileConfig.getIncoming().getDependencies()
                    .forEach(d -> {
                        if (d instanceof ProjectDependency) {
                            visitProjectDep(project, ((ProjectDependency) d).getDependencyProject(), visited);
                        }
                    });
        }
    }
}
