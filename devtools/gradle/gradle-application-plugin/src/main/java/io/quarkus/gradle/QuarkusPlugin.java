package io.quarkus.gradle;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;
import org.gradle.util.GradleVersion;

import io.quarkus.gradle.dependency.ApplicationDeploymentClasspathBuilder;
import io.quarkus.gradle.extension.QuarkusPluginExtension;
import io.quarkus.gradle.extension.SourceSetExtension;
import io.quarkus.gradle.tasks.Deploy;
import io.quarkus.gradle.tasks.ImageBuild;
import io.quarkus.gradle.tasks.ImagePush;
import io.quarkus.gradle.tasks.QuarkusAddExtension;
import io.quarkus.gradle.tasks.QuarkusBuild;
import io.quarkus.gradle.tasks.QuarkusBuildCacheableAppParts;
import io.quarkus.gradle.tasks.QuarkusBuildDependencies;
import io.quarkus.gradle.tasks.QuarkusDev;
import io.quarkus.gradle.tasks.QuarkusGenerateCode;
import io.quarkus.gradle.tasks.QuarkusGoOffline;
import io.quarkus.gradle.tasks.QuarkusGradleUtils;
import io.quarkus.gradle.tasks.QuarkusInfo;
import io.quarkus.gradle.tasks.QuarkusListCategories;
import io.quarkus.gradle.tasks.QuarkusListExtensions;
import io.quarkus.gradle.tasks.QuarkusListPlatforms;
import io.quarkus.gradle.tasks.QuarkusRemoteDev;
import io.quarkus.gradle.tasks.QuarkusRemoveExtension;
import io.quarkus.gradle.tasks.QuarkusRun;
import io.quarkus.gradle.tasks.QuarkusShowEffectiveConfig;
import io.quarkus.gradle.tasks.QuarkusTest;
import io.quarkus.gradle.tasks.QuarkusTestConfig;
import io.quarkus.gradle.tasks.QuarkusUpdate;
import io.quarkus.gradle.tooling.GradleApplicationModelBuilder;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.gradle.tooling.dependency.DependencyUtils;
import io.quarkus.gradle.tooling.dependency.ExtensionDependency;
import io.quarkus.gradle.tooling.dependency.ProjectExtensionDependency;
import io.quarkus.runtime.LaunchMode;

public class QuarkusPlugin implements Plugin<Project> {

    public static final String ID = "io.quarkus";
    public static final String DEFAULT_OUTPUT_DIRECTORY = "quarkus-app";
    public static final String QUARKUS_PACKAGE_TYPE = "quarkus.package.type"; // constant left here to not break potential users

    public static final String EXTENSION_NAME = "quarkus";
    public static final String LIST_EXTENSIONS_TASK_NAME = "listExtensions";
    public static final String LIST_CATEGORIES_TASK_NAME = "listCategories";
    public static final String LIST_PLATFORMS_TASK_NAME = "listPlatforms";
    public static final String ADD_EXTENSION_TASK_NAME = "addExtension";
    public static final String REMOVE_EXTENSION_TASK_NAME = "removeExtension";
    public static final String QUARKUS_GENERATE_CODE_TASK_NAME = "quarkusGenerateCode";
    public static final String QUARKUS_GENERATE_CODE_DEV_TASK_NAME = "quarkusGenerateCodeDev";
    public static final String QUARKUS_GENERATE_CODE_TESTS_TASK_NAME = "quarkusGenerateCodeTests";
    public static final String QUARKUS_BUILD_DEP_TASK_NAME = "quarkusDependenciesBuild";
    public static final String QUARKUS_BUILD_APP_PARTS_TASK_NAME = "quarkusAppPartsBuild";
    public static final String QUARKUS_SHOW_EFFECTIVE_CONFIG_TASK_NAME = "quarkusShowEffectiveConfig";
    public static final String QUARKUS_BUILD_TASK_NAME = "quarkusBuild";
    public static final String QUARKUS_DEV_TASK_NAME = "quarkusDev";
    public static final String QUARKUS_RUN_TASK_NAME = "quarkusRun";
    public static final String QUARKUS_REMOTE_DEV_TASK_NAME = "quarkusRemoteDev";
    public static final String QUARKUS_TEST_TASK_NAME = "quarkusTest";
    public static final String QUARKUS_GO_OFFLINE_TASK_NAME = "quarkusGoOffline";
    public static final String QUARKUS_INFO_TASK_NAME = "quarkusInfo";
    public static final String QUARKUS_UPDATE_TASK_NAME = "quarkusUpdate";
    public static final String IMAGE_BUILD_TASK_NAME = "imageBuild";
    public static final String IMAGE_PUSH_TASK_NAME = "imagePush";
    public static final String DEPLOY_TASK_NAME = "deploy";

    @Deprecated
    public static final String BUILD_NATIVE_TASK_NAME = "buildNative";
    @Deprecated
    public static final String TEST_NATIVE_TASK_NAME = "testNative";

    @Deprecated
    public static final String QUARKUS_TEST_CONFIG_TASK_NAME = "quarkusTestConfig";

    // this name has to be the same as the directory in which the tests reside
    public static final String NATIVE_TEST_SOURCE_SET_NAME = "native-test";

    public static final String NATIVE_TEST_IMPLEMENTATION_CONFIGURATION_NAME = "nativeTestImplementation";
    public static final String NATIVE_TEST_RUNTIME_ONLY_CONFIGURATION_NAME = "nativeTestRuntimeOnly";

    public static final String INTEGRATION_TEST_TASK_NAME = "quarkusIntTest";
    public static final String INTEGRATION_TEST_SOURCE_SET_NAME = "integrationTest";
    public static final String INTEGRATION_TEST_IMPLEMENTATION_CONFIGURATION_NAME = "integrationTestImplementation";
    public static final String INTEGRATION_TEST_RUNTIME_ONLY_CONFIGURATION_NAME = "integrationTestRuntimeOnly";

    private final ToolingModelBuilderRegistry registry;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    public QuarkusPlugin(ToolingModelBuilderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void apply(Project project) {
        verifyGradleVersion();

        // Apply the `java` plugin
        project.getPluginManager().apply(JavaPlugin.class);

        registerModel();

        // register extension
        final QuarkusPluginExtension quarkusExt = project.getExtensions().create(EXTENSION_NAME, QuarkusPluginExtension.class,
                project);

        createSourceSets(project);
        createConfigurations(project);
        registerTasks(project, quarkusExt);
    }

    private void registerTasks(Project project, QuarkusPluginExtension quarkusExt) {
        TaskContainer tasks = project.getTasks();

        final String devRuntimeConfigName = ApplicationDeploymentClasspathBuilder
                .getBaseRuntimeConfigName(LaunchMode.DEVELOPMENT);
        final Configuration devRuntimeDependencies = project.getConfigurations().maybeCreate(devRuntimeConfigName);

        tasks.register(LIST_EXTENSIONS_TASK_NAME, QuarkusListExtensions.class);
        tasks.register(LIST_CATEGORIES_TASK_NAME, QuarkusListCategories.class);
        tasks.register(LIST_PLATFORMS_TASK_NAME, QuarkusListPlatforms.class);
        tasks.register(ADD_EXTENSION_TASK_NAME, QuarkusAddExtension.class);
        tasks.register(REMOVE_EXTENSION_TASK_NAME, QuarkusRemoveExtension.class);
        tasks.register(QUARKUS_INFO_TASK_NAME, QuarkusInfo.class);
        tasks.register(QUARKUS_UPDATE_TASK_NAME, QuarkusUpdate.class);
        tasks.register(QUARKUS_GO_OFFLINE_TASK_NAME, QuarkusGoOffline.class, task -> {
            task.setCompileClasspath(project.getConfigurations()
                    .getByName(ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(LaunchMode.NORMAL)));
            task.setTestCompileClasspath(project.getConfigurations()
                    .getByName(ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(LaunchMode.TEST)));
            task.setQuarkusDevClasspath(project.getConfigurations()
                    .getByName(ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(LaunchMode.DEVELOPMENT)));
        });

        // quarkusGenerateCode
        TaskProvider<QuarkusGenerateCode> quarkusGenerateCode = tasks.register(QUARKUS_GENERATE_CODE_TASK_NAME,
                QuarkusGenerateCode.class, LaunchMode.NORMAL, SourceSet.MAIN_SOURCE_SET_NAME);
        quarkusGenerateCode.configure(task -> configureGenerateCodeTask(task, QuarkusGenerateCode.QUARKUS_GENERATED_SOURCES));
        // quarkusGenerateCodeDev
        TaskProvider<QuarkusGenerateCode> quarkusGenerateCodeDev = tasks.register(QUARKUS_GENERATE_CODE_DEV_TASK_NAME,
                QuarkusGenerateCode.class, LaunchMode.DEVELOPMENT, SourceSet.MAIN_SOURCE_SET_NAME);
        quarkusGenerateCodeDev.configure(task -> {
            task.dependsOn(quarkusGenerateCode);
            configureGenerateCodeTask(task, QuarkusGenerateCode.QUARKUS_GENERATED_SOURCES);
        });
        // quarkusGenerateCodeTests
        TaskProvider<QuarkusGenerateCode> quarkusGenerateCodeTests = tasks.register(QUARKUS_GENERATE_CODE_TESTS_TASK_NAME,
                QuarkusGenerateCode.class, LaunchMode.TEST, SourceSet.TEST_SOURCE_SET_NAME);
        quarkusGenerateCodeTests.configure(task -> {
            task.dependsOn("compileQuarkusTestGeneratedSourcesJava");
            configureGenerateCodeTask(task, QuarkusGenerateCode.QUARKUS_TEST_GENERATED_SOURCES);
        });

        tasks.register(QUARKUS_SHOW_EFFECTIVE_CONFIG_TASK_NAME,
                QuarkusShowEffectiveConfig.class, task -> {
                    task.setDescription("Show effective Quarkus build configuration.");
                });

        TaskProvider<QuarkusBuildDependencies> quarkusBuildDependencies = tasks.register(QUARKUS_BUILD_DEP_TASK_NAME,
                QuarkusBuildDependencies.class,
                task -> task.getOutputs().doNotCacheIf("Dependencies are never cached", t -> true));

        Property<Boolean> cacheLargeArtifacts = quarkusExt.getCacheLargeArtifacts();

        TaskProvider<QuarkusBuildCacheableAppParts> quarkusBuildCacheableAppParts = tasks.register(
                QUARKUS_BUILD_APP_PARTS_TASK_NAME,
                QuarkusBuildCacheableAppParts.class, task -> {
                    task.dependsOn(quarkusGenerateCode);
                    task.getOutputs().doNotCacheIf(
                            "Not adding uber-jars, native binaries and mutable-jar package type to Gradle " +
                                    "build cache by default. To allow caching of uber-jars, native binaries and mutable-jar " +
                                    "package type, set 'cacheUberAndNativeRunners' in the 'quarkus' Gradle extension to 'true'.",
                            new Spec<Task>() {
                                @Override
                                public boolean isSatisfiedBy(Task t) {
                                    QuarkusBuildCacheableAppParts q = (QuarkusBuildCacheableAppParts) t;
                                    return !q.isCachedByDefault() && !cacheLargeArtifacts.get();
                                }
                            });
                });

        TaskProvider<QuarkusBuild> quarkusBuild = tasks.register(QUARKUS_BUILD_TASK_NAME, QuarkusBuild.class, build -> {
            build.dependsOn(quarkusBuildDependencies, quarkusBuildCacheableAppParts);
            build.getOutputs().doNotCacheIf(
                    "Only collects and combines the outputs of " + QUARKUS_BUILD_APP_PARTS_TASK_NAME + " and "
                            + QUARKUS_BUILD_DEP_TASK_NAME + ", see 'cacheLargeArtifacts' in the 'quarkus' Gradle extension " +
                            "for details.",
                    new Spec<Task>() {
                        @Override
                        public boolean isSatisfiedBy(Task t) {
                            return !cacheLargeArtifacts.get();
                        }
                    });
        });

        tasks.register(IMAGE_BUILD_TASK_NAME, ImageBuild.class, task -> task.finalizedBy(quarkusBuild));

        tasks.register(IMAGE_PUSH_TASK_NAME, ImagePush.class, task -> task.finalizedBy(quarkusBuild));

        tasks.register(DEPLOY_TASK_NAME, Deploy.class, task -> task.finalizedBy(quarkusBuild));

        TaskProvider<QuarkusDev> quarkusDev = tasks.register(QUARKUS_DEV_TASK_NAME, QuarkusDev.class, devRuntimeDependencies,
                quarkusExt);
        TaskProvider<QuarkusRun> quarkusRun = tasks.register(QUARKUS_RUN_TASK_NAME, QuarkusRun.class,
                build -> build.dependsOn(quarkusBuild));
        TaskProvider<QuarkusRemoteDev> quarkusRemoteDev = tasks.register(QUARKUS_REMOTE_DEV_TASK_NAME, QuarkusRemoteDev.class,
                devRuntimeDependencies, quarkusExt);
        TaskProvider<QuarkusTest> quarkusTest = tasks.register(QUARKUS_TEST_TASK_NAME, QuarkusTest.class,
                devRuntimeDependencies, quarkusExt);
        tasks.register(QUARKUS_TEST_CONFIG_TASK_NAME, QuarkusTestConfig.class);

        tasks.register(BUILD_NATIVE_TASK_NAME, DefaultTask.class, task -> {
            task.finalizedBy(quarkusBuild);
            task.doFirst(t -> t.getLogger()
                    .warn("The 'buildNative' task has been deprecated in favor of 'build -Dquarkus.package.type=native'"));
        });

        configureBuildNativeTask(project);
        project.getPlugins().withType(
                BasePlugin.class,
                basePlugin -> tasks.named(BasePlugin.ASSEMBLE_TASK_NAME, task -> task.dependsOn(quarkusBuild)));
        project.getPlugins().withType(
                JavaPlugin.class,
                javaPlugin -> {

                    project.afterEvaluate(this::afterEvaluate);

                    tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME, JavaCompile.class,
                            compileJava -> {
                                compileJava.mustRunAfter(quarkusGenerateCode);
                                compileJava.mustRunAfter(quarkusGenerateCodeDev);
                            });
                    tasks.named(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME, JavaCompile.class,
                            compileTestJava -> {
                                compileTestJava.dependsOn(quarkusGenerateCode);
                                compileTestJava.dependsOn(quarkusGenerateCodeTests);
                                if (project.getGradle().getStartParameter().getTaskNames().contains(QUARKUS_DEV_TASK_NAME)) {
                                    compileTestJava.getOptions().setFailOnError(false);
                                }
                            });

                    TaskProvider<Task> classesTask = tasks.named(JavaPlugin.CLASSES_TASK_NAME);
                    TaskProvider<Task> resourcesTask = tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME);
                    TaskProvider<Task> testClassesTask = tasks.named(JavaPlugin.TEST_CLASSES_TASK_NAME);
                    TaskProvider<Task> testResourcesTask = tasks.named(JavaPlugin.PROCESS_TEST_RESOURCES_TASK_NAME);

                    quarkusGenerateCode.configure(task -> {
                        Configuration config = project.getConfigurations().getByName(
                                ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(LaunchMode.NORMAL));
                        task.dependsOn(resourcesTask, config);
                        task.setCompileClasspath(config);
                    });
                    quarkusGenerateCodeDev.configure(task -> {
                        Configuration config = project.getConfigurations().getByName(
                                ApplicationDeploymentClasspathBuilder
                                        .getBaseRuntimeConfigName(LaunchMode.DEVELOPMENT));
                        task.dependsOn(resourcesTask, config);
                        task.setCompileClasspath(config);
                    });
                    quarkusGenerateCodeTests.configure(task -> {
                        Configuration config = project.getConfigurations().getByName(
                                ApplicationDeploymentClasspathBuilder.getBaseRuntimeConfigName(LaunchMode.TEST));
                        task.dependsOn(resourcesTask, config);
                        task.setCompileClasspath(config);
                    });

                    quarkusDev.configure(task -> {
                        task.dependsOn(classesTask, resourcesTask, testClassesTask, testResourcesTask,
                                quarkusGenerateCodeDev,
                                quarkusGenerateCodeTests);
                    });
                    quarkusRemoteDev.configure(task -> {
                        task.dependsOn(classesTask, resourcesTask);
                    });
                    quarkusTest.configure(task -> {
                        task.dependsOn(classesTask, resourcesTask, testClassesTask, testResourcesTask,
                                quarkusGenerateCode,
                                quarkusGenerateCodeTests);
                    });
                    quarkusBuildCacheableAppParts.configure(
                            task -> task.dependsOn(classesTask, resourcesTask, tasks.named(JavaPlugin.JAR_TASK_NAME)));

                    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);

                    SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                    SourceSet testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);

                    mainSourceSet.getJava().srcDirs(quarkusGenerateCode, quarkusGenerateCodeDev);
                    testSourceSet.getJava().srcDirs(quarkusGenerateCodeTests);

                    quarkusGenerateCode.configure(task -> task.setSourcesDirectories(getSourcesParents(mainSourceSet)));
                    quarkusGenerateCodeDev.configure(task -> task.setSourcesDirectories(getSourcesParents(mainSourceSet)));
                    quarkusGenerateCodeTests.configure(task -> task.setSourcesDirectories(getSourcesParents(testSourceSet)));

                    SourceSet intTestSourceSet = sourceSets.getByName(INTEGRATION_TEST_SOURCE_SET_NAME);
                    intTestSourceSet.setCompileClasspath(
                            intTestSourceSet.getCompileClasspath()
                                    .plus(mainSourceSet.getOutput())
                                    .plus(testSourceSet.getOutput()));
                    intTestSourceSet.setRuntimeClasspath(
                            intTestSourceSet.getRuntimeClasspath()
                                    .plus(mainSourceSet.getOutput())
                                    .plus(testSourceSet.getOutput()));

                    TaskProvider<Test> testTask = tasks.named(JavaPlugin.TEST_TASK_NAME, Test.class);
                    FileCollection intTestSourceOutputClasses = intTestSourceSet.getOutput().getClassesDirs();
                    FileCollection intTestClasspath = intTestSourceSet.getRuntimeClasspath();

                    tasks.register(INTEGRATION_TEST_TASK_NAME, Test.class, intTestTask -> {
                        intTestTask.setGroup("verification");
                        intTestTask.setDescription("Runs Quarkus integration tests");
                        intTestTask.dependsOn(quarkusBuild, testTask);
                        intTestTask.setClasspath(intTestClasspath);
                        intTestTask.setTestClassesDirs(intTestSourceOutputClasses);
                    });

                    SourceSet nativeTestSourceSet = sourceSets.getByName(NATIVE_TEST_SOURCE_SET_NAME);
                    nativeTestSourceSet.setCompileClasspath(
                            nativeTestSourceSet.getCompileClasspath()
                                    .plus(mainSourceSet.getOutput())
                                    .plus(intTestSourceSet.getOutput())
                                    .plus(testSourceSet.getOutput()));
                    nativeTestSourceSet.setRuntimeClasspath(
                            nativeTestSourceSet.getRuntimeClasspath()
                                    .plus(mainSourceSet.getOutput())
                                    .plus(intTestSourceSet.getOutput())
                                    .plus(testSourceSet.getOutput()));

                    FileCollection nativeTestClassesDirs = project.files(
                            nativeTestSourceSet.getOutput().getClassesDirs(),
                            intTestSourceOutputClasses);
                    FileCollection nativeTestClasspath = nativeTestSourceSet.getRuntimeClasspath();

                    tasks.register(TEST_NATIVE_TASK_NAME, Test.class, testNative -> {
                        testNative.setDescription("Runs native image tests");
                        testNative.setGroup("verification");
                        testNative.dependsOn(quarkusBuild, testTask);
                        testNative.setClasspath(nativeTestClasspath);
                        testNative.setTestClassesDirs(nativeTestClassesDirs);
                    });

                    tasks.withType(Test.class).configureEach(t -> {
                        // Calling this method tells Gradle that it should not fail the build.
                        // Side effect is that the configuration cache will be at least degraded,
                        // but the build will not fail.
                        t.notCompatibleWithConfigurationCache(
                                "The quarkus-plugin isn't compatible with the configuration cache");

                        // Quarkus test configuration action which should be executed before any Quarkus test
                        // Use anonymous classes in order to leverage task avoidance.
                        t.doFirst(new Action<Task>() {
                            @Override
                            public void execute(Task task) {
                                quarkusExt.beforeTest((Test) task);
                            }
                        });
                        // also make each task use the JUnit platform since it's the only supported test environment
                        t.useJUnitPlatform();
                    });
                    // quarkusBuild is expected to run after the project has passed the tests
                    quarkusBuildCacheableAppParts.configure(task -> task.shouldRunAfter(tasks.withType(Test.class)));

                    SourceSet generatedSourceSet = sourceSets.getByName(QuarkusGenerateCode.QUARKUS_GENERATED_SOURCES);
                    SourceSet generatedTestSourceSet = sourceSets.getByName(QuarkusGenerateCode.QUARKUS_TEST_GENERATED_SOURCES);

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

    private static void configureGenerateCodeTask(QuarkusGenerateCode task, String generateSourcesDir) {
        SourceSet generatedSources = QuarkusGradleUtils.getSourceSet(task.getProject(), generateSourcesDir);
        Set<File> sourceSetOutput = generatedSources.getOutput().filter(f -> f.getName().equals(generateSourcesDir)).getFiles();
        if (sourceSetOutput.isEmpty()) {
            throw new GradleException("Failed to configure " + task.getPath() + ": sourceSet " + generateSourcesDir
                    + " has no output");
        }
        task.getGeneratedOutputDirectory().set(generatedSources.getJava().getClassesDirectory().get().getAsFile());
    }

    private void createSourceSets(Project project) {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        sourceSets.create(INTEGRATION_TEST_SOURCE_SET_NAME);
        sourceSets.create(NATIVE_TEST_SOURCE_SET_NAME);
        sourceSets.create(QuarkusGenerateCode.QUARKUS_GENERATED_SOURCES);
        sourceSets.create(QuarkusGenerateCode.QUARKUS_TEST_GENERATED_SOURCES);
    }

    private void createConfigurations(Project project) {

        final ConfigurationContainer configContainer = project.getConfigurations();

        // Custom configuration to be used for the dependencies of the testNative task
        configContainer.getByName(NATIVE_TEST_IMPLEMENTATION_CONFIGURATION_NAME)
                .extendsFrom(configContainer.findByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME));
        configContainer.getByName(NATIVE_TEST_RUNTIME_ONLY_CONFIGURATION_NAME)
                .extendsFrom(configContainer.findByName(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME));

        // create a custom configuration to be used for the dependencies of the quarkusIntTest task
        configContainer
                .maybeCreate(INTEGRATION_TEST_IMPLEMENTATION_CONFIGURATION_NAME)
                .extendsFrom(configContainer.findByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME));
        configContainer.maybeCreate(INTEGRATION_TEST_RUNTIME_ONLY_CONFIGURATION_NAME)
                .extendsFrom(configContainer.findByName(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME));

        ApplicationDeploymentClasspathBuilder.initConfigurations(project);

        // Also initialize the configurations that are specific to a LaunchMode
        for (LaunchMode launchMode : LaunchMode.values()) {
            new ApplicationDeploymentClasspathBuilder(project, launchMode);
        }
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
                // Nag user
                project.getLogger().warn("The Quarkus tasks {} and {} are deprecated and subject to removal. " +
                        "Please migrate your build to use 'test -Dquarkus.package.type=native' and " +
                        "'quarkusBuild -Dquarkus.package.type=native'.",
                        TEST_NATIVE_TASK_NAME, BUILD_NATIVE_TASK_NAME);
                project.getExtensions().getExtraProperties()
                        .set(QUARKUS_PACKAGE_TYPE, "native");
            }
        });
    }

    private void afterEvaluate(Project project) {

        visitProjectDependencies(project, project, new HashSet<>());

        ConfigurationContainer configurations = project.getConfigurations();

        SourceSetExtension sourceSetExtension = project.getExtensions().getByType(QuarkusPluginExtension.class)
                .sourceSetExtension();

        if (sourceSetExtension.extraNativeTest() != null) {
            SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            SourceSet nativeTestSourceSets = sourceSets.getByName(NATIVE_TEST_SOURCE_SET_NAME);
            nativeTestSourceSets.setCompileClasspath(
                    nativeTestSourceSets.getCompileClasspath()
                            .plus(sourceSets.getByName(INTEGRATION_TEST_SOURCE_SET_NAME).getOutput())
                            .plus(sourceSetExtension.extraNativeTest().getOutput()));
            nativeTestSourceSets.setRuntimeClasspath(
                    nativeTestSourceSets.getRuntimeClasspath()
                            .plus(sourceSets.getByName(INTEGRATION_TEST_SOURCE_SET_NAME).getOutput())
                            .plus(sourceSetExtension.extraNativeTest().getOutput()));

            configurations.getByName(NATIVE_TEST_IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(
                    configurations.findByName(sourceSetExtension.extraNativeTest().getImplementationConfigurationName()));
            configurations.getByName(NATIVE_TEST_RUNTIME_ONLY_CONFIGURATION_NAME).extendsFrom(
                    configurations.findByName(sourceSetExtension.extraNativeTest().getRuntimeOnlyConfigurationName()));
        }
    }

    private void visitProjectDep(Project project, Project dep, Set<String> visited) {
        if (dep.getState().getExecuted()) {
            setupQuarkusBuildTaskDeps(project, dep, visited);
        } else {
            dep.afterEvaluate(p -> setupQuarkusBuildTaskDeps(project, p, visited));
        }
    }

    private void setupQuarkusBuildTaskDeps(Project project, Project dep, Set<String> visited) {
        if (!visited.add(dep.getGroup() + ":" + dep.getName())) {
            return;
        }

        project.getLogger().debug("Configuring {} task dependencies on {} tasks", project, dep);

        getLazyTask(project, QUARKUS_BUILD_TASK_NAME)
                .flatMap(quarkusBuild -> getLazyTask(dep, JavaPlugin.JAR_TASK_NAME))
                .ifPresent(jarTask -> {
                    for (String taskName : new String[] { QUARKUS_GENERATE_CODE_TASK_NAME, QUARKUS_GENERATE_CODE_DEV_TASK_NAME,
                            QUARKUS_GENERATE_CODE_TESTS_TASK_NAME }) {
                        getLazyTask(project, taskName)
                                .ifPresent(quarkusTask -> quarkusTask.configure(t -> t.dependsOn(jarTask)));
                    }
                });

        getLazyTask(project, QUARKUS_DEV_TASK_NAME).ifPresent(quarkusDev -> {
            for (String taskName : new String[] { JavaPlugin.PROCESS_RESOURCES_TASK_NAME,
                    // This is the task of the 'org.kordamp.gradle.jandex' Gradle plugin
                    "jandex",
                    // This is the task of the 'com.github.vlsi.jandex' Gradle plugin
                    "processJandexIndex" }) {
                getLazyTask(dep, taskName).ifPresent(t -> quarkusDev.configure(qd -> qd.dependsOn(t)));
            }
        });

        visitProjectDependencies(project, dep, visited);
    }

    protected void visitProjectDependencies(Project project, Project dep, Set<String> visited) {
        final Configuration compileConfig = dep.getConfigurations().findByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
        if (compileConfig != null) {
            final Configuration compilePlusRuntimeConfig = dep.getConfigurations().detachedConfiguration()
                    .extendsFrom(compileConfig);
            final Configuration runtimeOnlyConfig = dep.getConfigurations()
                    .findByName(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME);
            if (runtimeOnlyConfig != null) {
                compilePlusRuntimeConfig.extendsFrom(runtimeOnlyConfig);
            }
            compilePlusRuntimeConfig.getIncoming().getDependencies()
                    .forEach(d -> {
                        Project depProject = null;

                        if (d instanceof ProjectDependency) {
                            depProject = ((ProjectDependency) d).getDependencyProject();
                        } else if (d instanceof ExternalModuleDependency) {
                            depProject = ToolingUtils.findIncludedProject(project, (ExternalModuleDependency) d);
                        }

                        if (depProject == null) {
                            return;
                        }

                        if (depProject.getState().getExecuted()) {
                            visitLocalProject(project, depProject, visited);
                        } else {
                            depProject.afterEvaluate(p -> visitLocalProject(project, p, visited));
                        }
                    });
        }
    }

    private void visitLocalProject(Project project, Project localProject, Set<String> visited) {
        // local dependency, so we collect also its dependencies
        visitProjectDep(project, localProject, visited);

        ExtensionDependency<?> extensionDependency = DependencyUtils
                .getExtensionInfoOrNull(project, localProject);

        if (extensionDependency instanceof ProjectExtensionDependency) {
            visitProjectDep(project,
                    ((ProjectExtensionDependency) extensionDependency).getDeploymentModule(), visited);
        }
    }

    private Optional<TaskProvider<Task>> getLazyTask(Project project, String name) {
        try {
            return Optional.of(project.getTasks().named(name));
        } catch (UnknownTaskException e) {
            return Optional.empty();
        }
    }
}
