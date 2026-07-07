package io.quarkus.gradle.application.internal.plugin;

import static io.quarkus.gradle.application.internal.plugin.TaskRegistrationSupport.QUARKUS_APPLICATION_GROUP;

import java.io.File;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyFactory;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.TaskProvider;

import io.quarkus.gradle.application.internal.modelgen.ApplicationModelResolutionViews;
import io.quarkus.gradle.application.internal.modelgen.ClasspathBuilder;
import io.quarkus.gradle.application.internal.modelgen.GenerateModelTask;
import io.quarkus.gradle.application.internal.modelgen.ResolvedClasspath;
import io.quarkus.gradle.application.tasks.QuarkusApplicationShowModelTask;
import io.quarkus.gradle.model.config.LocalComponentOutputViews;
import io.quarkus.gradle.model.pom.DeclaredDependencyEnrichmentMode;
import io.quarkus.gradle.model.pom.ExternalModuleDeclaredDependencyInput;
import io.quarkus.gradle.model.pom.StrictDependencyDataCollector;
import io.quarkus.gradle.model.tasks.GeneratePomClosureTask;
import io.quarkus.runtime.LaunchMode;

final class ApplicationModelTaskRegistration {

    private static final List<String> JANDEX_TASK_NAMES = List.of("jandex", "processJandexIndex");

    private final TaskNameRegistry taskNames;

    ApplicationModelTaskRegistration(TaskNameRegistry taskNames) {
        this.taskNames = taskNames;
    }

    ModelTasks register(Project project, ClasspathBuilder classpath,
            ApplicationModelResolutionViews resolutionViews) {
        // Declared-dependency enrichment belongs to the normal package model. Dev,
        // test, and code-generation models do not need the additional POM closure.
        TaskProvider<GenerateModelTask> applicationModel = registerApplicationModelTask(project, classpath,
                resolutionViews,
                "quarkusApplicationModel", LaunchMode.NORMAL, SourceSet.MAIN_SOURCE_SET_NAME, true,
                "quarkus/application-model/quarkus-application-model.dat",
                "Resolves the Quarkus application model used by named application build tasks.",
                DeclaredDependencyEnrichmentMode.SELECTED_MODULE_POMS);
        TaskProvider<GenerateModelTask> devApplicationModel = registerApplicationModelTask(project, classpath,
                resolutionViews,
                "quarkusApplicationDevModel", LaunchMode.DEVELOPMENT, SourceSet.MAIN_SOURCE_SET_NAME, true,
                "quarkus/application-model/quarkus-application-dev-model.dat",
                "Resolves the Quarkus application model used by Gradle-native dev mode.",
                DeclaredDependencyEnrichmentMode.NONE);
        wireJandexTasksIntoApplicationModels(project, applicationModel, devApplicationModel);
        // These models are prerequisites of source generation and therefore must
        // not depend on classes, whose compilation already depends on generation.
        TaskProvider<GenerateModelTask> codegenApplicationModel = registerApplicationModelTask(project, classpath,
                resolutionViews,
                "quarkusApplicationCodegenModel", LaunchMode.NORMAL, SourceSet.MAIN_SOURCE_SET_NAME, false,
                "quarkus/application-model/quarkus-application-codegen-model.dat",
                "Resolves the Quarkus application model used before main-source code generation.",
                DeclaredDependencyEnrichmentMode.NONE);
        TaskProvider<GenerateModelTask> devCodegenApplicationModel = registerApplicationModelTask(project, classpath,
                resolutionViews,
                "quarkusApplicationDevCodegenModel", LaunchMode.DEVELOPMENT, SourceSet.MAIN_SOURCE_SET_NAME, false,
                "quarkus/application-model/quarkus-application-dev-codegen-model.dat",
                "Resolves the Quarkus application model used before development-mode code generation.",
                DeclaredDependencyEnrichmentMode.NONE);
        TaskProvider<GenerateModelTask> testCodegenApplicationModel = registerApplicationModelTask(project, classpath,
                resolutionViews,
                "quarkusApplicationTestCodegenModel", LaunchMode.TEST, SourceSet.TEST_SOURCE_SET_NAME, false,
                "quarkus/application-model/quarkus-application-test-codegen-model.dat",
                "Resolves the Quarkus application model used before test-source code generation.",
                DeclaredDependencyEnrichmentMode.NONE);
        TaskProvider<GenerateModelTask> testApplicationModel = registerApplicationModelTask(project, classpath,
                resolutionViews,
                "quarkusApplicationTestModel", LaunchMode.TEST, SourceSet.MAIN_SOURCE_SET_NAME, true,
                "quarkus/application-model/quarkus-application-test-model.dat",
                "Resolves the Quarkus application model used by Gradle finite test tasks.",
                DeclaredDependencyEnrichmentMode.NONE);
        TaskProvider<GenerateModelTask> continuousTestApplicationModel = registerApplicationModelTask(project, classpath,
                resolutionViews,
                "quarkusApplicationContinuousTestModel", LaunchMode.TEST, SourceSet.MAIN_SOURCE_SET_NAME, true,
                "quarkus/application-model/quarkus-application-continuous-test-model.dat",
                "Resolves the Quarkus application model used by Gradle-native continuous testing.",
                DeclaredDependencyEnrichmentMode.NONE, true);
        registerApplicationModelDiagnosticsTask(project, "quarkusApplicationShowModel", "normal",
                applicationModel, "quarkus-application-model.txt");
        registerApplicationModelDiagnosticsTask(project, "quarkusApplicationShowDevModel", "development",
                devApplicationModel, "quarkus-application-dev-model.txt");
        registerApplicationModelDiagnosticsTask(project, "quarkusApplicationShowTestModel", "test",
                testApplicationModel, "quarkus-application-test-model.txt");
        return new ModelTasks(applicationModel, devApplicationModel, codegenApplicationModel,
                devCodegenApplicationModel, testCodegenApplicationModel, testApplicationModel,
                continuousTestApplicationModel,
                project.getTasks().named("quarkusApplicationModelPomClosure", GeneratePomClosureTask.class));
    }

    private void registerApplicationModelDiagnosticsTask(Project project, String taskName, String modelName,
            TaskProvider<GenerateModelTask> modelTask, String reportFileName) {
        taskNames.register(project, taskName);
        project.getTasks().register(taskName, QuarkusApplicationShowModelTask.class, task -> {
            task.getModelName().set(modelName);
            task.getProjectDirectoryPath().set(project.getLayout().getProjectDirectory().getAsFile().getAbsolutePath());
            task.getBuildRootPath().set(project.getLayout().getSettingsDirectory().getAsFile().getAbsolutePath());
            task.getApplicationModel().set(modelTask.flatMap(GenerateModelTask::getApplicationModel));
            task.getReportFile().set(project.getLayout().getBuildDirectory()
                    .file("reports/quarkus/application-model/" + reportFileName));
            task.setGroup(QUARKUS_APPLICATION_GROUP);
            task.setDescription("Displays the generated " + modelName + " Quarkus application model.");
        });
    }

    private TaskProvider<GenerateModelTask> registerApplicationModelTask(Project project,
            ClasspathBuilder classpath, ApplicationModelResolutionViews resolutionViews,
            String taskName, LaunchMode launchMode, String sourceSetName,
            boolean dependsOnClasses, String modelPath, String description,
            DeclaredDependencyEnrichmentMode enrichmentMode) {
        return registerApplicationModelTask(project, classpath, resolutionViews, taskName, launchMode, sourceSetName,
                dependsOnClasses, modelPath, description, enrichmentMode, false);
    }

    private TaskProvider<GenerateModelTask> registerApplicationModelTask(Project project,
            ClasspathBuilder classpath, ApplicationModelResolutionViews resolutionViews,
            String taskName, LaunchMode launchMode, String sourceSetName,
            boolean dependsOnClasses, String modelPath, String description,
            DeclaredDependencyEnrichmentMode enrichmentMode, boolean continuousTest) {
        SourceSet sourceSet = project.getExtensions().getByType(JavaPluginExtension.class)
                .getSourceSets()
                .getByName(sourceSetName);
        TaskProvider<GeneratePomClosureTask> pomClosureTask = enrichmentMode == DeclaredDependencyEnrichmentMode.SELECTED_MODULE_POMS
                ? registerPomClosureTask(project, classpath, taskName, launchMode)
                : null;
        Configuration runtimeConfiguration = continuousTest
                ? classpath.getContinuousTestRuntimeConfiguration()
                : runtimeConfiguration(classpath, launchMode);
        Configuration deploymentConfiguration = continuousTest
                ? classpath.getContinuousTestDeploymentConfiguration()
                : deploymentConfiguration(classpath, launchMode);
        Configuration compileOnlyConfiguration = compileOnlyConfiguration(classpath, launchMode);
        LocalComponentOutputViews localComponentOutputs = continuousTest
                ? resolutionViews.forContinuousTest().localOutputs()
                : resolutionViews.forMode(launchMode).localOutputs();
        FileCollection originalClasspath = continuousTest
                ? classpath.getOriginalContinuousTestRuntimeClasspathAsInput()
                : originalClasspath(classpath, launchMode);
        return project.getTasks().register(taskName, GenerateModelTask.class, task -> {
            if (dependsOnClasses) {
                task.dependsOn(project.getTasks().named(JavaPlugin.CLASSES_TASK_NAME));
            }
            task.getLaunchMode().set(launchMode);
            task.getProjectGroup().set(project.getGroup().toString());
            task.getProjectName().set(project.getName());
            task.getProjectVersion().set(project.getVersion().toString());
            task.getProjectBuildFile().fileValue(project.getBuildFile());
            task.getProjectDirectory().set(project.getLayout().getProjectDirectory());
            task.getBuildDirectory().set(project.getLayout().getBuildDirectory());
            if (dependsOnClasses) {
                task.getApplicationClassesDirectories().from(sourceSet.getOutput().getClassesDirs());
                if (sourceSet.getOutput().getResourcesDir() != null) {
                    task.getApplicationResourcesDirectories().from(sourceSet.getOutput().getResourcesDir());
                }
            }
            task.getApplicationSourceDirectoryPaths().set(directoryPaths(sourceSet.getAllJava()));
            task.getApplicationResourceSourceDirectoryPaths().set(directoryPaths(sourceSet.getResources()));
            if (launchMode == LaunchMode.TEST && dependsOnClasses) {
                SourceSet testSourceSet = project.getExtensions().getByType(JavaPluginExtension.class)
                        .getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
                task.getTestClassesDirectoryPaths().set(filePaths(testSourceSet.getOutput().getClassesDirs()));
                if (testSourceSet.getOutput().getResourcesDir() != null) {
                    task.getTestResourcesDirectoryPaths()
                            .set(List.of(testSourceSet.getOutput().getResourcesDir().getAbsolutePath()));
                }
                task.getTestSourceDirectoryPaths().set(directoryPaths(testSourceSet.getAllJava()));
                task.getTestResourceSourceDirectoryPaths().set(directoryPaths(testSourceSet.getResources()));
            }
            task.getOriginalClasspath().setFrom(originalClasspath);
            task.getAppClasspath().configureFrom(runtimeConfiguration);
            task.getLocalClassOutputArtifacts().set(localComponentOutputs.classArtifacts());
            task.getLocalResourceOutputArtifacts().set(localComponentOutputs.resourceArtifacts());
            task.getLocalClassOutputMetadata()
                    .set(localComponentOutputs.classArtifacts().map(ResolvedClasspath::artifactMetadata));
            task.getLocalResourceOutputMetadata()
                    .set(localComponentOutputs.resourceArtifacts().map(ResolvedClasspath::artifactMetadata));
            task.getLocalComponentOutputFiles()
                    .from(localComponentOutputs.classFiles(), localComponentOutputs.resourceFiles());
            task.getPlatformConfiguration().configureFrom(classpath.getPlatformPropertiesConfiguration());
            task.getPlatformInfo().configureFrom(classpath.getPlatformPropertiesConfiguration());
            task.getCompileOnlyClasspath().configureFrom(compileOnlyConfiguration);
            task.getDeploymentClasspath().configureFrom(deploymentConfiguration);
            task.getDeploymentClasspathFiles()
                    .from(deploymentConfiguration.getIncoming().getArtifacts().getArtifactFiles());
            task.getMavenLocalRepositoryRoots().set(project.getProviders().systemProperty("maven.repo.local")
                    .map(List::of)
                    .orElse(List.of()));
            task.getDeclaredDependencyEnrichmentMode().set(enrichmentMode);
            if (pomClosureTask != null) {
                task.getPomClosureFile().set(pomClosureTask.flatMap(GeneratePomClosureTask::getPomClosureFile));
            }
            task.getApplicationModel().set(project.getLayout().getBuildDirectory().file(modelPath));
            task.setGroup(QUARKUS_APPLICATION_GROUP);
            task.setDescription(description);
        });
    }

    private TaskProvider<GeneratePomClosureTask> registerPomClosureTask(Project project, ClasspathBuilder classpath,
            String modelTaskName, LaunchMode launchMode) {
        Configuration runtimeConfiguration = runtimeConfiguration(classpath, launchMode);
        Configuration deploymentConfiguration = deploymentConfiguration(classpath, launchMode);
        // Keep both graph traversal and POM artifact resolution provider-backed.
        // They are task inputs, not work to perform while the plugin is configured.
        Provider<Set<ResolvedArtifactResult>> runtimeArtifacts = runtimeConfiguration.getIncoming()
                .getArtifacts()
                .getResolvedArtifacts();
        Provider<Set<ResolvedArtifactResult>> deploymentArtifacts = deploymentConfiguration.getIncoming()
                .getArtifacts()
                .getResolvedArtifacts();
        Provider<List<ExternalModuleDeclaredDependencyInput>> externalModuleInputs = runtimeArtifacts.zip(
                deploymentArtifacts, ApplicationModelTaskRegistration::externalModuleInputs);
        Provider<ResolvedComponentResult> runtimeRoot = runtimeConfiguration.getIncoming()
                .getResolutionResult()
                .getRootComponent();
        Provider<ResolvedComponentResult> deploymentRoot = deploymentConfiguration.getIncoming()
                .getResolutionResult()
                .getRootComponent();
        Provider<List<String>> selectedPomGavs = runtimeRoot.zip(deploymentRoot,
                ApplicationModelTaskRegistration::externalModuleGavs);
        Configuration selectedPomConfiguration = selectedPomConfiguration(project, modelTaskName, selectedPomGavs);
        ArtifactView selectedPomView = selectedPomConfiguration.getIncoming().artifactView(view -> view.lenient(true));
        Provider<Map<String, String>> selectedPomFiles = selectedPomView.getArtifacts().getResolvedArtifacts()
                .map(ApplicationModelTaskRegistration::pomFilesByGav);
        Provider<List<String>> mavenLocalRepositoryRoots = project.getProviders().systemProperty("maven.repo.local")
                .map(List::of)
                .orElse(List.of());
        var pomClosureInput = project.getProviders().provider(new PomClosureInputCalculator(
                project.getDependencies(), project.getProviders(), externalModuleInputs, selectedPomFiles,
                mavenLocalRepositoryRoots));
        String taskName = modelTaskName + "PomClosure";
        return project.getTasks().register(taskName, GeneratePomClosureTask.class, task -> {
            task.getPomClosureInput().set(pomClosureInput);
            task.getPomClosureFile().set(project.getLayout().getBuildDirectory()
                    .file("quarkus/application-model/pom-closure/" + modelTaskName + ".properties"));
            task.setGroup(QUARKUS_APPLICATION_GROUP);
            task.setDescription("Resolves the Maven POM closure used to enrich " + modelTaskName + ".");
        });
    }

    private static Configuration selectedPomConfiguration(Project project, String modelTaskName,
            Provider<List<String>> selectedPomGavs) {
        DependencyFactory dependencies = project.getDependencyFactory();
        return project.getConfigurations().resolvable(modelTaskName + "PomArtifacts", configuration -> {
            configuration.setTransitive(false);
            Provider<List<Dependency>> pomDependencies = selectedPomGavs.map(gavs -> gavs.stream()
                    .map(gav -> {
                        ExternalModuleDependency dependency = dependencies.create(gav + "@pom");
                        dependency.setTransitive(false);
                        return (Dependency) dependency;
                    })
                    .toList());
            // The dependency coordinates are known only after resolving the runtime
            // and deployment graphs; addAllLater preserves that lazy boundary.
            configuration.getDependencies().addAllLater(pomDependencies);
        }).get();
    }

    private static List<ExternalModuleDeclaredDependencyInput> externalModuleInputs(
            Set<ResolvedArtifactResult> runtimeArtifacts, Set<ResolvedArtifactResult> deploymentArtifacts) {
        return StrictDependencyDataCollector.externalModuleDeclaredDependencyInputs(
                Stream.concat(runtimeArtifacts.stream(), deploymentArtifacts.stream()).toList());
    }

    static List<String> externalModuleGavs(ResolvedComponentResult runtimeRoot,
            ResolvedComponentResult deploymentRoot) {
        Set<String> gavs = new HashSet<>();
        collectExternalModuleGavs(runtimeRoot, gavs);
        collectExternalModuleGavs(deploymentRoot, gavs);
        return gavs.stream().sorted().toList();
    }

    private static void collectExternalModuleGavs(ResolvedComponentResult root, Set<String> target) {
        Set<ComponentIdentifier> visited = new HashSet<>();
        ArrayDeque<ResolvedComponentResult> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            ResolvedComponentResult component = pending.removeFirst();
            if (!visited.add(component.getId())) {
                continue;
            }
            if (component.getId() instanceof ModuleComponentIdentifier module) {
                target.add(module.getGroup() + ":" + module.getModule() + ":" + module.getVersion());
            }
            component.getDependencies().stream()
                    .filter(ResolvedDependencyResult.class::isInstance)
                    .map(ResolvedDependencyResult.class::cast)
                    .map(ResolvedDependencyResult::getSelected)
                    .forEach(pending::addLast);
        }
    }

    private static Map<String, String> pomFilesByGav(Set<ResolvedArtifactResult> artifacts) {
        Map<String, String> result = new HashMap<>();
        collectPomFilesByGav(artifacts, result);
        return result;
    }

    private static void collectPomFilesByGav(Set<ResolvedArtifactResult> artifacts, Map<String, String> target) {
        for (ResolvedArtifactResult artifact : artifacts) {
            ComponentIdentifier componentIdentifier = artifact.getId().getComponentIdentifier();
            if (componentIdentifier instanceof ModuleComponentIdentifier module) {
                target.put(module.getGroup() + ":" + module.getModule() + ":" + module.getVersion(),
                        artifact.getFile().getAbsolutePath());
            }
        }
    }

    private static FileCollection originalClasspath(ClasspathBuilder classpath, LaunchMode launchMode) {
        if (launchMode == LaunchMode.TEST) {
            return classpath.getOriginalTestRuntimeClasspathAsInput();
        }
        if (launchMode == LaunchMode.DEVELOPMENT) {
            return classpath.getOriginalDevRuntimeClasspathAsInput();
        }
        return classpath.getOriginalRuntimeClasspathAsInput();
    }

    private static Provider<List<String>> directoryPaths(SourceDirectorySet sourceDirectories) {
        return sourceDirectories.getSourceDirectories().getElements().map(elements -> elements.stream()
                .map(FileSystemLocation::getAsFile)
                .map(File::getAbsolutePath)
                .sorted()
                .toList());
    }

    private static Provider<List<String>> filePaths(FileCollection files) {
        return files.getElements().map(elements -> elements.stream()
                .map(FileSystemLocation::getAsFile)
                .map(File::getAbsolutePath)
                .sorted()
                .toList());
    }

    private static Configuration runtimeConfiguration(ClasspathBuilder classpath,
            LaunchMode launchMode) {
        if (launchMode == LaunchMode.TEST) {
            return classpath.getTestRuntimeConfiguration();
        }
        if (launchMode == LaunchMode.DEVELOPMENT) {
            return classpath.getDevRuntimeConfiguration();
        }
        return classpath.getRuntimeConfiguration();
    }

    private static Configuration deploymentConfiguration(ClasspathBuilder classpath,
            LaunchMode launchMode) {
        if (launchMode == LaunchMode.TEST) {
            return classpath.getTestDeploymentConfiguration();
        }
        if (launchMode == LaunchMode.DEVELOPMENT) {
            return classpath.getDevDeploymentConfiguration();
        }
        return classpath.getDeploymentConfiguration();
    }

    private static Configuration compileOnlyConfiguration(ClasspathBuilder classpath,
            LaunchMode launchMode) {
        if (launchMode == LaunchMode.TEST) {
            return classpath.getTestCompileOnlyConfiguration();
        }
        return classpath.getCompileOnlyConfiguration();
    }

    private void wireJandexTasksIntoApplicationModels(Project project,
            TaskProvider<GenerateModelTask> applicationModel,
            TaskProvider<GenerateModelTask> devApplicationModel) {
        for (String jandexTaskName : JANDEX_TASK_NAMES) {
            TaskCollection<?> jandexTasks = project.getTasks().matching(task -> task.getName().equals(jandexTaskName));
            applicationModel.configure(task -> task.dependsOn(jandexTasks));
            devApplicationModel.configure(task -> task.dependsOn(jandexTasks));
        }
    }

    record ModelTasks(
            TaskProvider<GenerateModelTask> applicationModel,
            TaskProvider<GenerateModelTask> devApplicationModel,
            TaskProvider<GenerateModelTask> codegenApplicationModel,
            TaskProvider<GenerateModelTask> devCodegenApplicationModel,
            TaskProvider<GenerateModelTask> testCodegenApplicationModel,
            TaskProvider<GenerateModelTask> testApplicationModel,
            TaskProvider<GenerateModelTask> continuousTestApplicationModel,
            TaskProvider<GeneratePomClosureTask> applicationModelPomClosure) {
    }
}
