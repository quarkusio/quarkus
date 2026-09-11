package io.quarkus.gradle.application.internal.plugin;

import static io.quarkus.gradle.application.QuarkusApplicationPlugin.EXTENSION_NAME;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testing.base.TestingExtension;

import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.dsl.QuarkusApplicationJvmTestSuite;
import io.quarkus.gradle.application.internal.plugin.ApplicationModelAndCodegenRegistration.ApplicationTasks;
import io.quarkus.gradle.application.tasks.QuarkusApplicationGenerateCodeTask;

final class ApplicationTestTaskRegistration {

    private ApplicationTestTaskRegistration() {
    }

    static QuarkusApplicationIntegrationTestConfigurator register(Project project,
            QuarkusApplicationExtension extension, ApplicationTasks applicationTasks, TaskNameRegistry taskNames,
            OfflinePreparationRegistration offlinePreparation, DslLifecycleCoordinator lifecycle) {
        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet mainSourceSet = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        TestOwnership ownership = new TestOwnership(project);
        QuarkusApplicationTestConfigurator configurator = new QuarkusApplicationTestConfigurator(
                project,
                extension,
                applicationTasks.testApplicationModel(),
                ownership,
                mainSourceSet,
                project.files(applicationTasks.generateCode()
                        .flatMap(QuarkusApplicationGenerateCodeTask::getGeneratedOutputDirectory)),
                project.files(applicationTasks.generateTestCode()
                        .flatMap(QuarkusApplicationGenerateCodeTask::getGeneratedOutputDirectory)));
        var integrationTestConfigurator = new QuarkusApplicationIntegrationTestConfigurator(project, taskNames,
                configurator, offlinePreparation, ownership, applicationTasks.generateTestCode(),
                applicationTasks.generatedTestSources(), extension.getCodegen().getProviders(), lifecycle);

        configurator.configure(project.getTasks().named(JavaPlugin.TEST_TASK_NAME, Test.class));
        lifecycle.attachTests(extension.getTests(), configurator::configure,
                () -> project.getTasks().withType(Test.class).configureEach(configurator::configure));
        configureJvmTestSuiteOptIn(project, configurator, integrationTestConfigurator, lifecycle);
        return integrationTestConfigurator;
    }

    private static void configureJvmTestSuiteOptIn(Project project, QuarkusApplicationTestConfigurator configurator,
            QuarkusApplicationIntegrationTestConfigurator integrationTestConfigurator,
            DslLifecycleCoordinator lifecycle) {
        project.getPlugins().withId("jvm-test-suite", ignored -> project.getExtensions()
                .configure(TestingExtension.class, testing -> testing.getSuites()
                        .withType(JvmTestSuite.class)
                        .configureEach(suite -> {
                            ExtensionAware extensionAwareSuite = (ExtensionAware) suite;
                            if (extensionAwareSuite.getExtensions()
                                    .findByName(EXTENSION_NAME) == null) {
                                Action<TaskProvider<? extends Test>> testAction = configurator::configure;
                                Action<QuarkusApplicationJvmTestSuite.IntegrationTestRequest> integrationTestAction = request -> integrationTestConfigurator
                                        .configure(request.suite(), request.build());
                                Action<QuarkusApplicationJvmTestSuite.StartupArchiveTrainingRequest> trainingAction = request -> integrationTestConfigurator
                                        .configureStartupArchiveTraining(request.suite(), request.training());
                                Action<QuarkusApplicationJvmTestSuite.IncludedTestsRequest> includedTestsAction = integrationTestConfigurator::includeTests;
                                QuarkusApplicationJvmTestSuite quarkusSuite = extensionAwareSuite.getExtensions()
                                        .create(EXTENSION_NAME,
                                                QuarkusApplicationJvmTestSuite.class, suite,
                                                testAction,
                                                integrationTestAction,
                                                trainingAction,
                                                includedTestsAction,
                                                lifecycle);
                                extensionAwareSuite.getExtensions().add("forQuarkusTests",
                                        new QuarkusApplicationJvmTestSuite.QuarkusTests(quarkusSuite));
                                extensionAwareSuite.getExtensions().add("forQuarkusIntegrationTests",
                                        new QuarkusApplicationJvmTestSuite.QuarkusIntegrationTests(quarkusSuite));
                                extensionAwareSuite.getExtensions().add("includeTestsFrom",
                                        new QuarkusApplicationJvmTestSuite.IncludedTests(quarkusSuite));
                                extensionAwareSuite.getExtensions().getExtraProperties().set("startupArchiveTraining",
                                        new QuarkusApplicationJvmTestSuite.StartupArchiveTraining(quarkusSuite));
                            }
                        })));
    }
}
