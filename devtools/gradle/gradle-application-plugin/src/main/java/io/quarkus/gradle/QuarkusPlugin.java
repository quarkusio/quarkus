package io.quarkus.gradle;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
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
import org.gradle.api.UnknownTaskException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;
import org.gradle.util.GradleVersion;

import io.quarkus.gradle.dependency.ApplicationDeploymentClasspathBuilder;
import io.quarkus.gradle.dependency.ConditionalDependenciesEnabler;
import io.quarkus.gradle.dependency.ExtensionDependency;
import io.quarkus.gradle.extension.QuarkusPluginExtension;
import io.quarkus.gradle.extension.SourceSetExtension;
import io.quarkus.gradle.tasks.QuarkusAddExtension;
import io.quarkus.gradle.tasks.QuarkusBuild;
import io.quarkus.gradle.tasks.QuarkusDev;
import io.quarkus.gradle.tasks.QuarkusGenerateCode;
import io.quarkus.gradle.tasks.QuarkusListCategories;
import io.quarkus.gradle.tasks.QuarkusListExtensions;
import io.quarkus.gradle.tasks.QuarkusListPlatforms;
import io.quarkus.gradle.tasks.QuarkusRemoteDev;
import io.quarkus.gradle.tasks.QuarkusRemoveExtension;
import io.quarkus.gradle.tasks.QuarkusTest;
import io.quarkus.gradle.tasks.QuarkusTestConfig;
import io.quarkus.gradle.tasks.QuarkusTestNative;
import io.quarkus.gradle.tooling.GradleApplicationModelBuilder;

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
    public static final String QUARKUS_GENERATE_CODE_DEV_TASK_NAME = "quarkusGenerateCodeDev";
    public static final String QUARKUS_GENERATE_CODE_TESTS_TASK_NAME = "quarkusGenerateCodeTests";
    public static final String QUARKUS_BUILD_TASK_NAME = "quarkusBuild";
    public static final String QUARKUS_DEV_TASK_NAME = "quarkusDev";
    public static final String QUARKUS_REMOTE_DEV_TASK_NAME = "quarkusRemoteDev";
    public static final String QUARKUS_TEST_TASK_NAME = "quarkusTest";
    public static final String DEV_MODE_CONFIGURATION_NAME = "quarkusDev";

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

        tasks.register(LIST_EXTENSIONS_TASK_NAME, QuarkusListExtensions.class);
        tasks.register(LIST_CATEGORIES_TASK_NAME, QuarkusListCategories.class);
        tasks.register(LIST_PLATFORMS_TASK_NAME, QuarkusListPlatforms.class);
        tasks.register(ADD_EXTENSION_TASK_NAME, QuarkusAddExtension.class);
        tasks.register(REMOVE_EXTENSION_TASK_NAME, QuarkusRemoveExtension.class);

        TaskProvider<QuarkusGenerateCode> quarkusGenerateCode = tasks.register(QUARKUS_GENERATE_CODE_TASK_NAME,
                QuarkusGenerateCode.class);
        TaskProvider<QuarkusGenerateCode> quarkusGenerateCodeDev = tasks.register(QUARKUS_GENERATE_CODE_DEV_TASK_NAME,
                QuarkusGenerateCode.class, task -> task.setDevMode(true));
        TaskProvider<QuarkusGenerateCode> quarkusGenerateCodeTests = tasks.register(QUARKUS_GENERATE_CODE_TESTS_TASK_NAME,
                QuarkusGenerateCode.class, task -> task.setTest(true));

        TaskProvider<QuarkusBuild> quarkusBuild = tasks.register(QUARKUS_BUILD_TASK_NAME, QuarkusBuild.class, build -> {
            build.dependsOn(quarkusGenerateCode);
        });

        TaskProvider<QuarkusDev> quarkusDev = tasks.register(QUARKUS_DEV_TASK_NAME, QuarkusDev.class);
        TaskProvider<QuarkusRemoteDev> quarkusRemoteDev = tasks.register(QUARKUS_REMOTE_DEV_TASK_NAME, QuarkusRemoteDev.class);
        TaskProvider<QuarkusTest> quarkusTest = tasks.register(QUARKUS_TEST_TASK_NAME, QuarkusTest.class);
        tasks.register(QUARKUS_TEST_CONFIG_TASK_NAME, QuarkusTestConfig.class);

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
            quarkusBuild.configure(task -> task.shouldRunAfter(t));
        };

        project.getPlugins().withType(
                BasePlugin.class,
                basePlugin -> tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(quarkusBuild));
        project.getPlugins().withType(
                JavaPlugin.class,
                javaPlugin -> {
                    project.afterEvaluate(this::afterEvaluate);
                    ConfigurationContainer configurations = project.getConfigurations();

                    tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME, JavaCompile.class,
                            compileJava -> {
                                compileJava.mustRunAfter(quarkusGenerateCode);
                                compileJava.mustRunAfter(quarkusGenerateCodeDev);
                            });
                    tasks.named(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME, JavaCompile.class,
                            compileTestJava -> {
                                compileTestJava.dependsOn(quarkusGenerateCodeTests);
                                if (project.getGradle().getStartParameter().getTaskNames().contains(QUARKUS_DEV_TASK_NAME)) {
                                    compileTestJava.getOptions().setFailOnError(false);
                                }
                            });

                    TaskProvider<Task> classesTask = tasks.named(JavaPlugin.CLASSES_TASK_NAME);
                    TaskProvider<Task> resourcesTask = tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME);
                    TaskProvider<Task> testClassesTask = tasks.named(JavaPlugin.TEST_CLASSES_TASK_NAME);
                    TaskProvider<Task> testResourcesTask = tasks.named(JavaPlugin.PROCESS_TEST_RESOURCES_TASK_NAME);

                    quarkusGenerateCode.configure(task -> task.dependsOn(resourcesTask));
                    quarkusGenerateCodeDev.configure(task -> task.dependsOn(resourcesTask));
                    quarkusGenerateCodeTests.configure(task -> task.dependsOn(resourcesTask));

                    quarkusDev.configure(task -> task.dependsOn(classesTask, resourcesTask, testClassesTask, testResourcesTask,
                            quarkusGenerateCodeDev,
                            quarkusGenerateCodeTests));
                    quarkusRemoteDev.configure(task -> task.dependsOn(classesTask, resourcesTask));
                    quarkusTest.configure(task -> task.dependsOn(classesTask, resourcesTask, testClassesTask, testResourcesTask,
                            quarkusGenerateCode,
                            quarkusGenerateCodeTests));
                    quarkusBuild.configure(
                            task -> task.dependsOn(classesTask, resourcesTask, tasks.named(JavaPlugin.JAR_TASK_NAME)));

                    SourceSetContainer sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class)
                            .getSourceSets();
                    SourceSet nativeTestSourceSet = sourceSets.create(NATIVE_TEST_SOURCE_SET_NAME);
                    SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                    SourceSet testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);

                    quarkusGenerateCode.configure(task -> task.setSourcesDirectories(getSourcesParents(mainSourceSet)));
                    quarkusGenerateCodeDev.configure(task -> task.setSourcesDirectories(getSourcesParents(mainSourceSet)));
                    quarkusGenerateCodeTests.configure(task -> task.setSourcesDirectories(getSourcesParents(testSourceSet)));

                    nativeTestSourceSet.setCompileClasspath(
                            nativeTestSourceSet.getCompileClasspath()
                                    .plus(mainSourceSet.getOutput())
                                    .plus(testSourceSet.getOutput()));

                    nativeTestSourceSet.setRuntimeClasspath(
                            nativeTestSourceSet.getRuntimeClasspath()
                                    .plus(mainSourceSet.getOutput())
                                    .plus(testSourceSet.getOutput()));

                    // create a custom configuration for devmode
                    configurations.create(DEV_MODE_CONFIGURATION_NAME);

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
                        mainSourceSet.getJava().srcDir(
                                new File(generatedSourceSet.getJava().getClassesDirectory().get().getAsFile(), provider));
                        testSourceSet.getJava().srcDir(
                                new File(generatedTestSourceSet.getJava().getClassesDirectory().get().getAsFile(), provider));
                    }
                });

        project.getPlugins().withId("org.jetbrains.kotlin.jvm", plugin -> {
            quarkusDev.configure(task -> task.shouldPropagateJavaCompilerArgs(false));
            tasks.named("compileKotlin", task -> {
                task.mustRunAfter(quarkusGenerateCode);
                task.mustRunAfter(quarkusGenerateCodeDev);
            });
            tasks.named("compileTestKotlin", task -> task.dependsOn(quarkusGenerateCodeTests));
        });
    }

    private void registerConditionalDependencies(Project project) {
        ApplicationDeploymentClasspathBuilder deploymentClasspathBuilder = new ApplicationDeploymentClasspathBuilder(
                project);
        project.getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME).getIncoming()
                .beforeResolve((dependencies) -> {
                    final String configName = JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME;
                    final Collection<ExtensionDependency> implementationExtensions = new ConditionalDependenciesEnabler(project,
                            configName).declareConditionalDependencies();
                    deploymentClasspathBuilder.createBuildClasspath(implementationExtensions, configName);
                });
        project.getConfigurations().getByName(DEV_MODE_CONFIGURATION_NAME).getIncoming().beforeResolve((devDependencies) -> {
            final String configName = DEV_MODE_CONFIGURATION_NAME;
            final Collection<ExtensionDependency> devModeExtensions = new ConditionalDependenciesEnabler(project, configName)
                    .declareConditionalDependencies();
            deploymentClasspathBuilder.createBuildClasspath(devModeExtensions, configName);
        });
        project.getConfigurations().getByName(JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME).getIncoming()
                .beforeResolve((testDependencies) -> {
                    final String configName = JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME;
                    final Collection<ExtensionDependency> testExtensions = new ConditionalDependenciesEnabler(project,
                            configName).declareConditionalDependencies();
                    deploymentClasspathBuilder.createBuildClasspath(testExtensions, configName);
                });
        project.getConfigurations().getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME).getIncoming()
                .beforeResolve(annotationProcessors -> {
                    Set<ResolvedArtifact> compileClasspathArtifacts = project.getConfigurations()
                            .getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME).getResolvedConfiguration()
                            .getResolvedArtifacts();

                    // enable the Panache annotation processor on the classpath, if it's found among the dependencies
                    for (ResolvedArtifact artifact : compileClasspathArtifacts) {
                        if ("quarkus-panache-common".equals(artifact.getName())
                                && "io.quarkus".equals(artifact.getModuleVersion().getId().getGroup())) {
                            project.getDependencies().add(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME,
                                    "io.quarkus:quarkus-panache-common:" + artifact.getModuleVersion().getId().getVersion());
                        }
                    }
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
        registry.register(new GradleApplicationModelBuilder());
    }

    private void verifyGradleVersion() {
        if (GradleVersion.current().compareTo(GradleVersion.version("6.1")) < 0) {
            throw new GradleException("Quarkus plugin requires Gradle 6.1 or later. Current version is: " +
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

        registerConditionalDependencies(project);

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

        final TaskProvider<Task> quarkusBuild = getLazyTaskOrNull(project, QUARKUS_BUILD_TASK_NAME);
        if (quarkusBuild != null) {
            final TaskProvider<Task> jarTask = getLazyTaskOrNull(dep, JavaPlugin.JAR_TASK_NAME);
            if (jarTask != null) {
                final TaskProvider<Task> quarkusPrepare = getLazyTaskOrNull(project, QUARKUS_GENERATE_CODE_TASK_NAME);
                final TaskProvider<Task> quarkusPrepareDev = getLazyTaskOrNull(project, QUARKUS_GENERATE_CODE_DEV_TASK_NAME);
                final TaskProvider<Task> quarkusPrepareTests = getLazyTaskOrNull(project,
                        QUARKUS_GENERATE_CODE_TESTS_TASK_NAME);
                quarkusBuild.configure(task -> task.dependsOn(jarTask));
                if (quarkusPrepare != null) {
                    quarkusPrepare.configure(task -> task.dependsOn(jarTask));
                }
                if (quarkusPrepareDev != null) {
                    quarkusPrepareDev.configure(task -> task.dependsOn(jarTask));
                }
                if (quarkusPrepareTests != null) {
                    quarkusPrepareTests.configure(task -> task.dependsOn(jarTask));
                }
            }
        }

        final TaskProvider<Task> quarkusDev = getLazyTaskOrNull(project, QUARKUS_DEV_TASK_NAME);
        if (quarkusDev != null) {
            final TaskProvider<Task> resourcesTask = getLazyTaskOrNull(dep, JavaPlugin.PROCESS_RESOURCES_TASK_NAME);
            if (resourcesTask != null) {
                quarkusDev.configure(task -> task.dependsOn(resourcesTask));
            }
            final TaskProvider<Task> resourcesTaskJandex = getLazyTaskOrNull(dep, "jandex");
            if (resourcesTaskJandex != null) {
                quarkusDev.configure(task -> task.dependsOn(resourcesTaskJandex));
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

    private TaskProvider<Task> getLazyTaskOrNull(Project project, String name) {
        try {
            return project.getTasks().named(name);
        } catch (UnknownTaskException e) {
            return null;
        }
    }
}
