package io.quarkus.gradle.application.internal.plugin;

import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.QUARKUS_APPLICATION_GROUP;
import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.configureConfigInputs;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.build.event.BuildEventsListenerRegistry;
import org.gradle.jvm.toolchain.JavaToolchainService;

import io.quarkus.deployment.dev.DevModeMain;
import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.internal.dev.QuarkusApplicationDevReplayTriggerTask;
import io.quarkus.gradle.application.internal.modelgen.ClasspathBuilder;
import io.quarkus.gradle.application.internal.modelgen.GenerateModelTask;
import io.quarkus.gradle.application.internal.plugin.ApplicationModelAndCodegenRegistration.ApplicationTasks;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationLaunchKind;
import io.quarkus.gradle.application.tasks.QuarkusApplicationContinuousTestTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationDevTask;
import io.quarkus.gradle.model.config.LocalComponentOutputViews;

final class DevTaskRegistration {

    private static final String DEV_MODE_CLASSPATH_CONFIGURATION = "quarkusApplicationDevModeClasspath";
    private static final String CONTINUOUS_TEST_TASK_NAME = "quarkusApplicationContinuousTest";
    private static final String DEV_REPLAY_TRIGGER_TASK_NAME = "quarkusApplicationDevInitializeReplayTrigger";
    private static final String CONTINUOUS_TEST_REPLAY_TRIGGER_TASK_NAME = "quarkusApplicationContinuousTestInitializeReplayTrigger";
    private final TaskNameRegistry taskNames;
    private final ContinuousBuildFailureRegistration continuousBuildFailures;
    private final RemoteDevTaskRegistration remoteDevTasks;

    DevTaskRegistration(TaskNameRegistry taskNames, BuildEventsListenerRegistry buildEventsListeners) {
        this.taskNames = taskNames;
        this.continuousBuildFailures = new ContinuousBuildFailureRegistration(buildEventsListeners);
        this.remoteDevTasks = new RemoteDevTaskRegistration(taskNames);
    }

    void register(Project project, QuarkusApplicationExtension extension, ClasspathBuilder classpath,
            Provider<Configuration> devModeClasspath, ApplicationTasks applicationTasks) {
        TestOwnership ownership = new TestOwnership(project);
        registerDevTask(project, extension, classpath, devModeClasspath, applicationTasks, ownership);
        registerContinuousTestTask(project, extension, classpath, devModeClasspath, applicationTasks, ownership);
        configureDefaultTestSuppression(project, extension);
        remoteDevTasks.register(project, extension, applicationTasks.applicationModel());
    }

    static Provider<Configuration> registerDevModeClasspathConfiguration(Project project) {
        return project.getConfigurations().register(DEV_MODE_CLASSPATH_CONFIGURATION, configuration -> {
            configuration.setDescription("Internal classpath used to launch Gradle-native Quarkus dev mode.");
            configuration.setCanBeConsumed(false);
            configuration.setCanBeResolved(true);
            configuration.setCanBeDeclared(true);
            configuration.getDependencies().add(devModeDependency(project, "quarkus-bootstrap-gradle-resolver"));
            configuration.getDependencies().add(devModeDependency(project, "quarkus-bootstrap-maven-resolver"));
            configuration.getDependencies().add(devModeDependency(project, "quarkus-core-deployment"));
        });
    }

    private static Dependency devModeDependency(Project project, String artifactId) {
        String pomPropsPath = "META-INF/maven/io.quarkus/" + artifactId + "/pom.properties";
        Properties properties = new Properties();
        try (InputStream stream = DevModeMain.class.getClassLoader().getResourceAsStream(pomPropsPath)) {
            if (stream == null) {
                throw new GradleException("Failed to locate " + pomPropsPath + " on the plugin classpath");
            }
            properties.load(stream);
        } catch (IOException e) {
            throw new GradleException("Failed to load " + pomPropsPath + " from the plugin classpath", e);
        }
        String groupId = requiredPomProperty(properties, pomPropsPath, "groupId");
        String version = requiredPomProperty(properties, pomPropsPath, "version");
        return project.getDependencies().create(groupId + ":" + artifactId + ":" + version);
    }

    private static String requiredPomProperty(Properties properties, String source, String name) {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new GradleException("Classpath resource " + source + " is missing " + name);
        }
        return value;
    }

    private void registerDevTask(Project project, QuarkusApplicationExtension extension, ClasspathBuilder classpath,
            Provider<Configuration> devModeClasspath, ApplicationTasks applicationTasks, TestOwnership ownership) {
        TaskProvider<QuarkusApplicationDevReplayTriggerTask> replayTrigger = registerReplayTrigger(
                project, DEV_REPLAY_TRIGGER_TASK_NAME, "quarkus-dev/live-reload-replay.trigger");
        taskNames.register(project, "quarkusApplicationDev");
        project.getTasks().register("quarkusApplicationDev", QuarkusApplicationDevTask.class, task -> {
            configureDevTask(project, extension, classpath, devModeClasspath, applicationTasks, ownership, task);
            task.dependsOn(replayTrigger);
            task.getReplayTriggerFile().set(replayTrigger.flatMap(QuarkusApplicationDevReplayTriggerTask::getTriggerFile));
            task.setGroup(QUARKUS_APPLICATION_GROUP);
            task.setDescription("Runs Gradle-native Quarkus dev mode using Gradle continuous build.");
            task.notCompatibleWithConfigurationCache(
                    "Gradle-native Quarkus dev mode requires Gradle continuous build and keeps a long-lived deployment session.");
        });
        continuousBuildFailures.register(project, "quarkusApplicationDev",
                explicitlyRequested(project, "quarkusApplicationDev"));
    }

    private void configureDevTask(Project project, QuarkusApplicationExtension extension, ClasspathBuilder classpath,
            Provider<Configuration> devModeClasspath, ApplicationTasks applicationTasks, TestOwnership ownership,
            QuarkusApplicationDevTask task) {
        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet mainSourceSet = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        SourceSet testSourceSet = java.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
        LocalComponentOutputViews localComponentOutputs = LocalComponentOutputViews.of(project.getObjects(),
                classpath.getDevRuntimeConfiguration());

        configureCommonLaunchOptions(project, extension, devModeClasspath, ownership, task, java);
        task.dependsOn(project.getTasks().named(JavaPlugin.CLASSES_TASK_NAME));
        task.dependsOn(applicationTasks.devApplicationModel());
        task.dependsOn(applicationTasks.generateDevCode());
        task.getContinuousTesting().set(extension.getDev().getContinuousTesting());
        task.dependsOn(task.getContinuousTesting().map(enabled -> enabled
                ? List.of(applicationTasks.continuousTestApplicationModel(), applicationTasks.generateTestCode(),
                        project.getTasks().named(JavaPlugin.TEST_CLASSES_TASK_NAME))
                : List.of()));
        task.getLaunchKind().set(QuarkusApplicationLaunchKind.DEV);
        task.getBuildName().set("dev");
        task.getBuildType().set(QuarkusApplicationBuildType.FAST_JAR);
        task.getApplicationModel()
                .set(applicationTasks.devApplicationModel().flatMap(GenerateModelTask::getApplicationModel));
        task.getTestApplicationModel()
                .set(applicationTasks.continuousTestApplicationModel().flatMap(GenerateModelTask::getApplicationModel));
        task.getSourceDirectories().from(mainSourceSet.getResources().getSourceDirectories());
        task.getApplicationClasses().from(mainSourceSet.getOutput().getClassesDirs());
        if (mainSourceSet.getOutput().getResourcesDir() != null) {
            task.getApplicationResources().from(mainSourceSet.getOutput().getResourcesDir());
        }
        configureConditionalTestOutputs(project, task, testSourceSet);
        task.getDependencyClasses().from(localComponentOutputs.classFiles());
        task.getDependencyResources().from(localComponentOutputs.resourceFiles());
        task.getRuntimeJarsWithoutOutputVariants()
                .from(localComponentOutputs.jarFilesWithoutOutputVariants(project.getProviders()));
        task.getReceiptFile().set(project.getLayout().getBuildDirectory()
                .file("quarkus-dev/dev-iteration.properties"));
        task.getCloseReceiptFile().set(project.getLayout().getBuildDirectory()
                .file("quarkus-dev/dev-session-closed.txt"));
        task.getOutputSnapshotFile().set(project.getLayout().getBuildDirectory()
                .file("quarkus-dev/dev-output-snapshot.tsv"));
        configureConfigInputs(task, extension.getConfigInputs());
    }

    private void registerContinuousTestTask(Project project, QuarkusApplicationExtension extension,
            ClasspathBuilder classpath, Provider<Configuration> devModeClasspath, ApplicationTasks applicationTasks,
            TestOwnership ownership) {
        TaskProvider<QuarkusApplicationDevReplayTriggerTask> replayTrigger = registerReplayTrigger(
                project, CONTINUOUS_TEST_REPLAY_TRIGGER_TASK_NAME,
                "quarkus-continuous-test/live-reload-replay.trigger");
        taskNames.register(project, CONTINUOUS_TEST_TASK_NAME);
        project.getTasks().register(CONTINUOUS_TEST_TASK_NAME, QuarkusApplicationContinuousTestTask.class, task -> {
            JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
            SourceSet main = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            SourceSet test = java.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
            LocalComponentOutputViews localOutputs = LocalComponentOutputViews.of(project.getObjects(),
                    classpath.getContinuousTestRuntimeConfiguration());

            configureCommonLaunchOptions(project, extension, devModeClasspath, ownership, task, java);
            task.dependsOn(replayTrigger);
            task.getReplayTriggerFile().set(replayTrigger.flatMap(QuarkusApplicationDevReplayTriggerTask::getTriggerFile));
            task.dependsOn(project.getTasks().named(JavaPlugin.TEST_CLASSES_TASK_NAME),
                    applicationTasks.continuousTestApplicationModel(),
                    applicationTasks.generateDevCode(),
                    applicationTasks.generateTestCode());
            task.getLaunchKind().set(QuarkusApplicationLaunchKind.CONTINUOUS_TEST);
            task.getBuildName().set("continuousTest");
            task.getBuildType().set(QuarkusApplicationBuildType.FAST_JAR);
            task.getContinuousTesting().set(true);
            task.getApplicationModel()
                    .set(applicationTasks.continuousTestApplicationModel().flatMap(GenerateModelTask::getApplicationModel));
            task.getTestApplicationModel()
                    .set(applicationTasks.continuousTestApplicationModel().flatMap(GenerateModelTask::getApplicationModel));
            task.getSourceDirectories().from(main.getAllSource().getSourceDirectories());
            task.getTestSourceDirectories().from(test.getAllSource().getSourceDirectories());
            task.getApplicationClasses().from(main.getOutput().getClassesDirs());
            if (main.getOutput().getResourcesDir() != null) {
                task.getApplicationResources().from(main.getOutput().getResourcesDir());
            }
            task.getTestClasses().from(test.getOutput().getClassesDirs());
            if (test.getOutput().getResourcesDir() != null) {
                task.getTestResources().from(test.getOutput().getResourcesDir());
            }
            task.getDependencyClasses().from(localOutputs.classFiles());
            task.getDependencyResources().from(localOutputs.resourceFiles());
            task.getRuntimeJarsWithoutOutputVariants()
                    .from(localOutputs.jarFilesWithoutOutputVariants(project.getProviders()));
            task.getReceiptFile().set(project.getLayout().getBuildDirectory()
                    .file("quarkus-continuous-test/iteration.properties"));
            task.getCloseReceiptFile().set(project.getLayout().getBuildDirectory()
                    .file("quarkus-continuous-test/session-closed.txt"));
            task.getOutputSnapshotFile().set(project.getLayout().getBuildDirectory()
                    .file("quarkus-continuous-test/output-snapshot.tsv"));
            configureConfigInputs(task, extension.getConfigInputs());
            task.setGroup("verification");
            task.setDescription("Runs Gradle-native Quarkus continuous testing for the default test suite.");
            task.notCompatibleWithConfigurationCache(
                    "Gradle-native Quarkus continuous testing requires Gradle continuous build and keeps a long-lived session.");
        });
        continuousBuildFailures.register(project, CONTINUOUS_TEST_TASK_NAME,
                explicitlyRequested(project, CONTINUOUS_TEST_TASK_NAME));
    }

    private TaskProvider<QuarkusApplicationDevReplayTriggerTask> registerReplayTrigger(Project project, String taskName,
            String relativePath) {
        taskNames.register(project, taskName);
        return project.getTasks().register(taskName, QuarkusApplicationDevReplayTriggerTask.class, task -> task
                .getTriggerFile().set(project.getLayout().getBuildDirectory().file(relativePath)));
    }

    private static void configureCommonLaunchOptions(Project project, QuarkusApplicationExtension extension,
            Provider<Configuration> devModeClasspath, TestOwnership ownership, QuarkusApplicationDevTask task,
            JavaPluginExtension java) {
        task.getContinuousBuild().set(project.getGradle().getStartParameter().isContinuous());
        task.getLegacyTestsOwned().set(ownership.ownedByLegacyPlugin());
        task.getApplicationName().set(project.getName());
        task.getApplicationVersion().set(project.getVersion().toString());
        task.getQuarkusBuildProperties().set(extension.getQuarkusBuildProperties());
        task.getQuarkusBuildProperties().putAll(extension.getDev().getQuarkusBuildProperties());
        task.getDevJvmArgs().set(extension.getDev().getForkOptions().getJvmArgs());
        task.getDevSystemProperties().set(extension.getDev().getForkOptions().getSystemProperties());
        task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
        task.getBuildDirectory().set(project.getLayout().getBuildDirectory());
        task.getWorkingDirectory().set(extension.getDev().getWorkingDirectory());
        task.getEnvironmentVariables().set(extension.getDev().getEnvironmentVariables());
        task.getDebug().set(extension.getDev().getDebug());
        task.getDebugMode().set(extension.getDev().getDebugMode());
        task.getDebugHost().set(extension.getDev().getDebugHost());
        task.getDebugPort().set(extension.getDev().getDebugPort());
        task.getSuspend().set(extension.getDev().getSuspend());
        task.getForceC2().set(extension.getDev().getForceC2());
        task.getExtensionJvmOptions().getDisableAll()
                .set(extension.getDev().getExtensionJvmOptions().getDisableAll());
        task.getExtensionJvmOptions().getDisableFor()
                .set(extension.getDev().getExtensionJvmOptions().getDisableFor());
        task.getDevModeClasspath().from(devModeClasspath);
        JavaToolchainService javaToolchains = project.getExtensions().getByType(JavaToolchainService.class);
        task.getJavaLauncher().convention(javaToolchains.launcherFor(java.getToolchain()));
    }

    private static String taskPath(Project project, String taskName) {
        return project.getPath().equals(":") ? ":" + taskName : project.getPath() + ":" + taskName;
    }

    private static void configureConditionalTestOutputs(Project project, QuarkusApplicationDevTask task,
            SourceSet testSourceSet) {
        Provider<Boolean> enabled = task.getContinuousTesting();
        // Keep test outputs absent from the dev task unless that session owns
        // continuous testing; the provider also tracks a command-line override.
        task.getTestSourceDirectories().from(enabled.map(value -> value
                ? testSourceSet.getAllSource().getSourceDirectories()
                : project.files()));
        task.getTestClasses().from(enabled.map(value -> value ? testSourceSet.getOutput().getClassesDirs() : project.files()));
        if (testSourceSet.getOutput().getResourcesDir() != null) {
            task.getTestResources().from(enabled.map(value -> value
                    ? project.files(testSourceSet.getOutput().getResourcesDir())
                    : project.files()));
        }
    }

    private static void configureDefaultTestSuppression(Project project, QuarkusApplicationExtension extension) {
        boolean dedicatedRequested = explicitlyRequested(project, CONTINUOUS_TEST_TASK_NAME);
        boolean devRequested = explicitlyRequested(project, "quarkusApplicationDev");
        Provider<Boolean> devContinuousTesting = commandLineContinuousTestingOverride(project);
        if (devContinuousTesting == null) {
            devContinuousTesting = extension.getDev().getContinuousTesting();
        }
        // Suppress the ordinary test task only when an explicitly requested
        // continuous session owns test execution in this invocation.
        Provider<Boolean> continuousSessionRequested = devContinuousTesting
                .map(enabled -> dedicatedRequested || devRequested && enabled);
        project.getTasks().named(JavaPlugin.TEST_TASK_NAME, Test.class).configure(task -> task.onlyIf(
                "The default Test action is replaced by the explicitly requested Quarkus continuous-test session",
                ignored -> !continuousSessionRequested.get()));
    }

    private static Provider<Boolean> commandLineContinuousTestingOverride(Project project) {
        Boolean override = null;
        for (var request : project.getGradle().getStartParameter().getTaskRequests()) {
            for (String argument : request.getArgs()) {
                if (argument.equals("--continuous-testing")) {
                    override = true;
                } else if (argument.equals("--no-continuous-testing")) {
                    override = false;
                }
            }
        }
        if (override == null) {
            return null;
        }
        boolean enabled = override;
        return project.getProviders().provider(() -> enabled);
    }

    private static boolean explicitlyRequested(Project project, String taskName) {
        String taskPath = taskPath(project, taskName);
        for (var request : project.getGradle().getStartParameter().getTaskRequests()) {
            for (String argument : request.getArgs()) {
                if (argument.equals(taskName) || argument.equals(taskPath)) {
                    return true;
                }
            }
        }
        return false;
    }

}
