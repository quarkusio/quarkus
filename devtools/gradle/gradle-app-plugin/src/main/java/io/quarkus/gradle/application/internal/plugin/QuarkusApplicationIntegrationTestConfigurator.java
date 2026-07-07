package io.quarkus.gradle.application.internal.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testing.base.TestingExtension;

import io.quarkus.gradle.application.dsl.QuarkusApplicationJvmTestSuite;
import io.quarkus.gradle.application.dsl.QuarkusApplicationStartupArchiveTraining;
import io.quarkus.gradle.application.internal.planning.TaskNameSegment;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationJvmStartupArchiveType;
import io.quarkus.gradle.application.model.QuarkusApplicationStartupArchiveTrainingExecutionTarget;
import io.quarkus.gradle.application.tasks.QuarkusApplicationGenerateCodeTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupArchiveTrainingMetadataTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupArchiveValidationTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationStartupOptimizedImageTask;

final class QuarkusApplicationIntegrationTestConfigurator {

    private final Project project;
    private final TaskNameRegistry taskNames;
    private final QuarkusApplicationTestConfigurator testConfigurator;
    private final OfflinePreparationRegistration offlinePreparation;
    private final TestOwnership ownership;
    private final TaskProvider<QuarkusApplicationGenerateCodeTask> generateTestCode;
    private final Provider<Directory> generatedTestSources;
    private final Provider<List<String>> codegenProviders;
    private final Map<String, QuarkusApplicationIntegrationTestBuild> builds = new LinkedHashMap<>();
    private final List<PendingIntegrationTest> pendingTests = new ArrayList<>();
    private final Map<String, QuarkusApplicationStartupArchiveTraining> trainingBySuite = new LinkedHashMap<>();
    private final Map<String, PendingIntegrationTest> trainingProducerByBuild = new LinkedHashMap<>();
    private final Set<String> nativeTestSuites = new HashSet<>();
    private final Map<String, Set<String>> includedSuites = new HashMap<>();
    private final DslLifecycleCoordinator lifecycle;

    QuarkusApplicationIntegrationTestConfigurator(Project project, TaskNameRegistry taskNames,
            QuarkusApplicationTestConfigurator testConfigurator,
            OfflinePreparationRegistration offlinePreparation,
            TestOwnership ownership,
            TaskProvider<QuarkusApplicationGenerateCodeTask> generateTestCode,
            Provider<Directory> generatedTestSources,
            Provider<List<String>> codegenProviders,
            DslLifecycleCoordinator lifecycle) {
        this.project = project;
        this.taskNames = taskNames;
        this.testConfigurator = testConfigurator;
        this.offlinePreparation = offlinePreparation;
        this.ownership = ownership;
        this.generateTestCode = generateTestCode;
        this.generatedTestSources = generatedTestSources;
        this.codegenProviders = codegenProviders;
        this.lifecycle = lifecycle;
        project.afterEvaluate(ignored -> validateTrainingSuites());
    }

    void configure(JvmTestSuite suite, Object buildNotation) {
        var reference = QuarkusApplicationIntegrationTestBuildReference.of(buildNotation);
        suite.getTargets().configureEach(target -> configure(target.getTestTask(), suite, reference));
    }

    void registerBuild(QuarkusApplicationIntegrationTestBuild build) {
        builds.put(build.buildName(), build);
        for (PendingIntegrationTest pending : pendingTests) {
            pending.attachIfMatches(build);
        }
    }

    void registerNativeTestSuite(QuarkusApplicationIntegrationTestBuild build, String suiteName) {
        if (ownership.ownedByLegacyPlugin()) {
            return;
        }
        nativeTestSuites.add(suiteName);
        taskNames.register(project, suiteName);
        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);
        NamedDomainObjectProvider<JvmTestSuite> suiteProvider = testing.getSuites().register(
                suiteName, JvmTestSuite.class, suite -> {
                    QuarkusApplicationJvmTestSuite quarkusSuite = ((ExtensionAware) suite).getExtensions()
                            .getByType(QuarkusApplicationJvmTestSuite.class);
                    lifecycle.claimNamedNativeTestSuite(quarkusSuite, suite.getName());
                    configureNativeTestSuiteDependencies(suite);
                    suite.getTargets().named(suiteName).configure(target -> {
                        target.getTestTask().configure(task -> {
                            task.setGroup("verification");
                            task.setDescription("Runs tests against the '" + build.buildName() + "' native executable.");
                        });
                        configure(target.getTestTask(), suite, build);
                    });
                });
        // Gradle's JVM Test Suite implementation creates its conventional target and task
        // when the suite is realized. Realize the suite, but keep the target Test task lazy.
        suiteProvider.get();
    }

    void includeTests(QuarkusApplicationJvmTestSuite.IncludedTestsRequest request) {
        String suiteName = request.suite().getName();
        if (!nativeTestSuites.contains(suiteName)) {
            throw new GradleException("JVM test suite '" + suiteName
                    + "' is not a generated Quarkus named native-test suite; includeTestsFrom(...) is available only "
                    + "on suites created for named native executable builds");
        }
        String includedSuiteName = request.includedSuite().getName();
        JvmTestSuite includedSuite = project.getExtensions().getByType(TestingExtension.class)
                .getSuites().withType(JvmTestSuite.class).findByName(includedSuiteName);
        if (includedSuite == null) {
            throw new GradleException("Quarkus named native-test suite '" + suiteName
                    + "' cannot include JVM test suite '" + includedSuiteName
                    + "' because it does not identify a JVM test suite owned by project '"
                    + project.getPath() + "'");
        }
        includeSuite(request.suite(), includedSuite);
    }

    void configureStartupArchiveTraining(JvmTestSuite suite,
            QuarkusApplicationStartupArchiveTraining training) {
        QuarkusApplicationStartupArchiveTraining previous = trainingBySuite.putIfAbsent(suite.getName(), training);
        if (previous != null) {
            throw new GradleException("Quarkus integration-test suite '" + suite.getName()
                    + "' configures startup-archive training more than once");
        }
        for (PendingIntegrationTest pending : pendingTests) {
            if (pending.suiteName.equals(suite.getName())) {
                pending.configureTraining(training);
            }
        }
    }

    private void validateTrainingSuites() {
        for (String suiteName : trainingBySuite.keySet()) {
            long targets = pendingTests.stream().filter(pending -> pending.suiteName.equals(suiteName)).count();
            if (targets == 0) {
                throw new GradleException("JVM test suite '" + suiteName
                        + "' configures startup-archive training but is not configured for Quarkus integration tests");
            }
            if (targets > 1) {
                throw new GradleException("Quarkus integration-test suite '" + suiteName
                        + "' configures startup-archive training but has more than one test target");
            }
        }
    }

    private void configure(TaskProvider<? extends Test> testTask, JvmTestSuite suite,
            QuarkusApplicationIntegrationTestBuildReference reference) {
        testConfigurator.configure(testTask);
        String suiteName = suite.getName();
        var missingBuildAction = new QuarkusApplicationIntegrationTestMissingBuildAction(
                suiteName, reference.displayName());
        SourceSet sources = suite.getSources();
        PendingIntegrationTest pending = new PendingIntegrationTest(testTask, suiteName, reference, missingBuildAction,
                List.of(
                        project.getConfigurations().getByName(sources.getCompileClasspathConfigurationName()),
                        project.getConfigurations().getByName(sources.getRuntimeClasspathConfigurationName()),
                        project.getConfigurations().getByName(sources.getAnnotationProcessorConfigurationName())));
        pendingTests.add(pending);
        testTask.configure(task -> task.doFirst(missingBuildAction));
        QuarkusApplicationIntegrationTestBuild existing = builds.get(reference.buildName());
        if (existing != null) {
            pending.attachIfMatches(existing);
        }
    }

    private void configure(TaskProvider<? extends Test> testTask, JvmTestSuite suite,
            QuarkusApplicationIntegrationTestBuild build) {
        testConfigurator.configure(testTask);
        var reference = QuarkusApplicationIntegrationTestBuildReference.of(build.buildName());
        var missingBuildAction = new QuarkusApplicationIntegrationTestMissingBuildAction(
                suite.getName(), build.buildName());
        PendingIntegrationTest pending = new PendingIntegrationTest(testTask, suite.getName(), reference,
                missingBuildAction, dependencyConfigurations(suite));
        testTask.configure(task -> task.doFirst(missingBuildAction));
        pending.attachIfMatches(build);
    }

    private List<Configuration> dependencyConfigurations(JvmTestSuite suite) {
        SourceSet sources = suite.getSources();
        return List.of(
                project.getConfigurations().getByName(sources.getCompileClasspathConfigurationName()),
                project.getConfigurations().getByName(sources.getRuntimeClasspathConfigurationName()),
                project.getConfigurations().getByName(sources.getAnnotationProcessorConfigurationName()));
    }

    private void configureNativeTestSuiteDependencies(JvmTestSuite suite) {
        SourceSet sources = suite.getSources();
        SourceSet testSources = project.getExtensions().getByType(JavaPluginExtension.class)
                .getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
        ConfigurationContainer configurations = project.getConfigurations();
        configurations.named(sources.getImplementationConfigurationName())
                .configure(configuration -> configuration.extendsFrom(
                        configurations.getByName(testSources.getImplementationConfigurationName())));
        configurations.named(sources.getCompileOnlyConfigurationName())
                .configure(configuration -> configuration.extendsFrom(
                        configurations.getByName(testSources.getCompileOnlyConfigurationName())));
        configurations.named(sources.getRuntimeOnlyConfigurationName())
                .configure(configuration -> configuration.extendsFrom(
                        configurations.getByName(testSources.getRuntimeOnlyConfigurationName())));
        configurations.named(sources.getAnnotationProcessorConfigurationName())
                .configure(configuration -> configuration.extendsFrom(
                        configurations.getByName(testSources.getAnnotationProcessorConfigurationName())));
        suite.getDependencies().getImplementation().add(suite.getDependencies().project());
        project.getTasks().named(sources.getCompileJavaTaskName(), JavaCompile.class)
                .configure(task -> {
                    task.dependsOn(generateTestCode);
                    task.source(GeneratedSourceDirectories.fromConfiguredProviders(
                            generatedTestSources, codegenProviders));
                });
        KotlinGeneratedSourceWiring.wireNamedTestSuite(project, suite.getName(), generateTestCode,
                generatedTestSources);
    }

    private void includeSuite(JvmTestSuite nativeSuite, JvmTestSuite includedSuite) {
        String nativeSuiteName = nativeSuite.getName();
        String includedSuiteName = includedSuite.getName();
        validateIncludedSuite(nativeSuiteName, includedSuite);
        if (includedSuites.computeIfAbsent(nativeSuiteName, ignored -> new LinkedHashSet<>()).add(includedSuiteName)) {
            attachIncludedSuite(nativeSuite, includedSuite);
        }
    }

    private void validateIncludedSuite(String nativeSuiteName, JvmTestSuite includedSuite) {
        String includedSuiteName = includedSuite.getName();
        if (nativeSuiteName.equals(includedSuiteName)) {
            throw new GradleException("Quarkus named native-test suite '" + nativeSuiteName
                    + "' cannot include itself");
        }
        if (nativeTestSuites.contains(includedSuiteName)) {
            throw new GradleException("Quarkus named native-test suite '" + nativeSuiteName
                    + "' cannot include generated native-test suite '" + includedSuiteName
                    + "'; include a user-owned JVM test suite instead");
        }
    }

    private static void attachIncludedSuite(JvmTestSuite nativeSuite, JvmTestSuite includedSuite) {
        SourceSet nativeSources = nativeSuite.getSources();
        SourceSet includedSources = includedSuite.getSources();
        nativeSources.setCompileClasspath(nativeSources.getCompileClasspath()
                .plus(includedSources.getOutput())
                .plus(includedSources.getCompileClasspath()));
        nativeSources.setRuntimeClasspath(nativeSources.getRuntimeClasspath().plus(includedSources.getRuntimeClasspath()));
        nativeSuite.getTargets().named(nativeSuite.getName()).configure(target -> target.getTestTask().configure(test -> {
            test.setTestClassesDirs(test.getTestClassesDirs().plus(includedSources.getOutput().getClassesDirs()));
            test.setClasspath(test.getClasspath().plus(includedSources.getRuntimeClasspath()));
        }));
    }

    private final class PendingIntegrationTest {
        private final TaskProvider<? extends Test> testTask;
        private final String suiteName;
        private final QuarkusApplicationIntegrationTestBuildReference reference;
        private final QuarkusApplicationIntegrationTestMissingBuildAction missingBuildAction;
        private final List<Configuration> dependencyConfigurations;
        private final DirectoryProperty launcherMetadataDirectory = project.getObjects().directoryProperty();
        private QuarkusApplicationStartupArchiveTraining training;
        private QuarkusApplicationIntegrationTestBuild attachedBuild;
        private boolean attached;
        private boolean trainingAttached;

        private PendingIntegrationTest(TaskProvider<? extends Test> testTask, String suiteName,
                QuarkusApplicationIntegrationTestBuildReference reference,
                QuarkusApplicationIntegrationTestMissingBuildAction missingBuildAction,
                List<Configuration> dependencyConfigurations) {
            this.testTask = testTask;
            this.suiteName = suiteName;
            this.reference = reference;
            this.missingBuildAction = missingBuildAction;
            this.dependencyConfigurations = dependencyConfigurations;
            this.training = trainingBySuite.get(suiteName);
        }

        private void attachIfMatches(QuarkusApplicationIntegrationTestBuild build) {
            if (attached || !build.buildName().equals(reference.buildName())) {
                return;
            }
            attached = true;
            attachedBuild = build;
            attach(build);
            if (training != null) {
                attachTraining(build, training);
            }
        }

        private void attach(QuarkusApplicationIntegrationTestBuild build) {
            if (ownership.ownedByLegacyPlugin()
                    && build.buildType() == QuarkusApplicationBuildType.NATIVE_EXECUTABLE) {
                throw new GradleException("Quarkus integration-test suite '" + suiteName
                        + "' cannot use named native build '" + build.buildName()
                        + "' while legacy 'io.quarkus' owns Gradle Test task instrumentation; "
                        + "use the legacy native-test surface or apply only 'io.quarkus.application'");
            }
            if (build.buildType() == QuarkusApplicationBuildType.NATIVE_SOURCES) {
                throw new GradleException("Quarkus integration-test suite '" + suiteName
                        + "' references build '" + build.buildName()
                        + "', but native-sources builds do not produce a runnable artifact");
            }
            if (!build.buildType().isJar() && build.buildType() != QuarkusApplicationBuildType.NATIVE_EXECUTABLE) {
                throw new GradleException("Quarkus integration-test suite '" + suiteName
                        + "' references build '" + build.buildName()
                        + "', but " + build.buildType() + " does not produce a runnable test artifact");
            }
            String expectedSuiteName = suiteName;
            String expectedBuildName = build.buildName();
            var executableInput = build.buildType() == QuarkusApplicationBuildType.NATIVE_EXECUTABLE
                    ? new QuarkusApplicationNativeExecutableInput(
                            build.resultFile().get().getAsFile().getAbsolutePath(),
                            expectedSuiteName, expectedBuildName)
                    : null;
            testTask.configure(task -> {
                missingBuildAction.attach();
                task.dependsOn(build.task());
                task.getInputs().files(build.resultFile())
                        .withPropertyName("quarkusIntegrationTestBuildResult")
                        .withPathSensitivity(PathSensitivity.RELATIVE);
                if (build.buildType().isJar()) {
                    task.dependsOn(build.launcherMetadataTask()
                            .orElseThrow(() -> new GradleException("Quarkus integration-test suite '" + suiteName
                                    + "' references JVM package build '" + build.buildName()
                                    + "', but no integration-test metadata task was registered")));
                    launcherMetadataDirectory.set(build.launcherMetadataDirectory());
                    task.getInputs().dir(launcherMetadataDirectory);
                    task.doFirst(new QuarkusApplicationIntegrationTestPackageAction(launcherMetadataDirectory));
                } else {
                    task.getInputs().dir(build.metadataDirectory());
                    task.getJvmArgumentProviders().add(executableInput);
                    task.doFirst(new QuarkusApplicationIntegrationTestNativeAction(
                            build.metadataDirectory(), build.resultFile(), expectedSuiteName, expectedBuildName));
                }
            });
        }

        private void configureTraining(QuarkusApplicationStartupArchiveTraining configuredTraining) {
            if (training != null && training != configuredTraining) {
                throw new GradleException("Quarkus integration-test suite '" + suiteName
                        + "' configures startup-archive training more than once");
            }
            training = configuredTraining;
            if (attachedBuild != null) {
                attachTraining(attachedBuild, configuredTraining);
            }
        }

        private void attachTraining(QuarkusApplicationIntegrationTestBuild build,
                QuarkusApplicationStartupArchiveTraining configuredTraining) {
            if (trainingAttached) {
                return;
            }
            if (build.buildType() != QuarkusApplicationBuildType.AOT_JAR || build.aotJarOutput().isEmpty()) {
                throw new GradleException("Quarkus integration-test suite '" + suiteName + "' references build '"
                        + build.buildName() + "', but startup-archive training requires an AOT_JAR output");
            }
            var previous = trainingProducerByBuild.putIfAbsent(build.buildName(), this);
            if (previous != null && previous != this) {
                throw new GradleException("Quarkus AOT-JAR output '" + build.buildName()
                        + "' is claimed by more than one integration-test target or suite for startup-archive training");
            }
            var aotJar = build.aotJarOutput().orElseThrow();
            var archive = aotJar.getStartupArchive();
            lifecycle.claimIntegrationTestTraining(archive, build.buildName());
            if (!archive.getType().isPresent()) {
                throw new GradleException("Quarkus AOT-JAR output '" + build.buildName()
                        + "' must select a concrete type before startup-archive training");
            }
            QuarkusApplicationJvmStartupArchiveType type = archive.getType().get();
            if (type == QuarkusApplicationJvmStartupArchiveType.APP_CDS) {
                throw new GradleException("Integration-test startup-archive training does not support APP_CDS");
            }
            if (archive.getFile().isPresent() || archive.getDirectory().isPresent()) {
                throw new GradleException("Quarkus AOT-JAR output '" + build.buildName()
                        + "' cannot combine integration-test training with a user-supplied startup archive");
            }
            offlinePreparation.addNamedBuildConfigurations(build.buildName(),
                    dependencyConfigurations.toArray(Configuration[]::new));

            String suitePathSegment = TaskNameSegment.of(suiteName).value();
            var trainingDirectory = project.getLayout().getBuildDirectory()
                    .dir("quarkus-builds/" + build.buildName() + "/startup-archive-training/" + suitePathSegment);
            var rawArchiveFile = trainingDirectory.map(directory -> directory.file(type.getDefaultName()));
            var rawArchiveDirectory = trainingDirectory.map(directory -> directory.dir(type.getDefaultName()));
            String metadataTaskName = "quarkus" + TaskNameSegment.of(build.buildName()).value()
                    + TaskNameSegment.of(suiteName).value() + "StartupArchiveTrainingMetadata";
            taskNames.register(project, metadataTaskName);
            var metadataTask = project.getTasks().register(metadataTaskName,
                    QuarkusApplicationStartupArchiveTrainingMetadataTask.class, task -> {
                        task.setGroup("verification");
                        task.setDescription("Generates startup-archive training metadata for integration-test suite '"
                                + suiteName + "' and Quarkus application build '" + build.buildName() + "'.");
                        task.dependsOn(build.launcherMetadataTask().orElseThrow());
                        task.getBaseMetadataDirectory().set(build.launcherMetadataDirectory());
                        task.getArchiveType().set(type);
                        task.getExecutionTarget().set(configuredTraining.getExecutionTarget());
                        task.getSuitePathSegment().set(suitePathSegment);
                        task.getArchiveDestination().set((type.isDirectory()
                                ? rawArchiveDirectory.map(directory -> directory.getAsFile().getAbsolutePath())
                                : rawArchiveFile.map(file -> file.getAsFile().getAbsolutePath())));
                        if (configuredTraining.getExecutionTarget()
                                .get() == QuarkusApplicationStartupArchiveTrainingExecutionTarget.BASE_IMAGE) {
                            task.dependsOn(build.baseImageTask().orElseThrow());
                            task.getBaseImageReceiptFile().set(build.baseImageReceipt().orElseThrow());
                        }
                        task.getMetadataDirectory().set(project.getLayout().getBuildDirectory()
                                .dir("quarkus-build-results/" + build.buildName()
                                        + "/integration-test-training/" + suitePathSegment));
                    });

            String validationTaskName = "quarkus" + TaskNameSegment.of(build.buildName()).value()
                    + "StartupArchiveValidation";
            taskNames.register(project, validationTaskName);
            TaskProvider<QuarkusApplicationStartupArchiveValidationTask> validationTask = project.getTasks()
                    .register(validationTaskName, QuarkusApplicationStartupArchiveValidationTask.class, task -> {
                        task.setGroup("verification");
                        task.setDescription("Validates the " + type + " startup archive produced for the '"
                                + build.buildName() + "' Quarkus application build.");
                        task.getArchiveType().set(type);
                        task.dependsOn(testTask);
                        if (type.isDirectory()) {
                            task.getArchiveDirectory().set(rawArchiveDirectory);
                        } else {
                            task.getArchiveFile().set(rawArchiveFile);
                        }
                    });
            project.getTasks().withType(QuarkusApplicationStartupOptimizedImageTask.class)
                    .matching(task -> task.getName().equals(build.taskNames().startupOptimizedImageBuild())
                            || task.getName().equals(build.taskNames().startupOptimizedImagePush()))
                    .configureEach(task -> {
                        // Validation consumes the test-produced archive, so its mapped input carries the
                        // path but does not establish a producer dependency for the image task.
                        task.dependsOn(validationTask);
                        if (type.isDirectory()) {
                            task.getArchiveDirectory().set(validationTask.flatMap(
                                    QuarkusApplicationStartupArchiveValidationTask::getArchiveDirectory));
                        } else {
                            task.getArchiveFile().set(validationTask.flatMap(
                                    QuarkusApplicationStartupArchiveValidationTask::getArchiveFile));
                        }
                    });

            if (type.isDirectory()) {
                archive.getDirectory().set(validationTask.flatMap(
                        QuarkusApplicationStartupArchiveValidationTask::getArchiveDirectory));
                archive.getDirectory().disallowChanges();
                archive.getFile().disallowChanges();
            } else {
                archive.getFile().set(validationTask.flatMap(
                        QuarkusApplicationStartupArchiveValidationTask::getArchiveFile));
                archive.getFile().disallowChanges();
                archive.getDirectory().disallowChanges();
            }

            testTask.configure(task -> {
                launcherMetadataDirectory.set(metadataTask.flatMap(
                        QuarkusApplicationStartupArchiveTrainingMetadataTask::getMetadataDirectory));
                task.dependsOn(metadataTask);
                if (type.isDirectory()) {
                    task.getOutputs().dir(rawArchiveDirectory);
                } else {
                    task.getOutputs().file(rawArchiveFile);
                }
                task.getOutputs().doNotCacheIf(
                        "Startup-archive training depends on runtime workload and platform state",
                        ignored -> true);
                task.getOutputs().upToDateWhen(ignored -> false);
                task.doFirst(new QuarkusApplicationStartupArchiveCleanupAction(trainingDirectory));
                task.doFirst(new QuarkusApplicationStartupArchiveTestWorkerValidationAction(suiteName));
            });
            trainingAttached = true;
        }
    }
}
