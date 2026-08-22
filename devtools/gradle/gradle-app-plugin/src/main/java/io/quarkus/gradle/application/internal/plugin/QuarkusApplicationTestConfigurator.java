package io.quarkus.gradle.application.internal.plugin;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;

import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.internal.modelgen.GenerateModelTask;

final class QuarkusApplicationTestConfigurator {

    private final Project project;
    private final QuarkusApplicationExtension extension;
    private final TaskProvider<GenerateModelTask> testApplicationModel;
    private final TestOwnership ownership;
    private final SourceSet mainSourceSet;
    private final ConfigurableFileCollection outputSourceDirectories;
    private final Set<String> configuredTaskPaths = new LinkedHashSet<>();

    QuarkusApplicationTestConfigurator(Project project, QuarkusApplicationExtension extension,
            TaskProvider<GenerateModelTask> testApplicationModel, TestOwnership ownership, SourceSet mainSourceSet,
            FileCollection generatedMainSources, FileCollection generatedTestSources) {
        this.project = project;
        this.extension = extension;
        this.testApplicationModel = testApplicationModel;
        this.ownership = ownership;
        this.mainSourceSet = mainSourceSet;
        this.outputSourceDirectories = project.files(generatedMainSources, generatedTestSources);
    }

    void configure(TaskProvider<? extends Test> taskProvider) {
        if (ownership.ownedByLegacyPlugin()) {
            return;
        }
        taskProvider.configure(this::configure);
    }

    void configure(Test task) {
        if (ownership.ownedByLegacyPlugin() || !configuredTaskPaths.add(task.getPath())) {
            return;
        }
        task.dependsOn(testApplicationModel);
        task.systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        task.jvmArgs(
                "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-exports=java.base/jdk.internal.module=ALL-UNNAMED");
        task.useJUnitPlatform();
        task.getInputs().files(testApplicationModel);
        task.getInputs().files(composeDevFiles());
        systemPropertiesPrefixedBy("quarkus.test.").get()
                .forEach(task::systemProperty);
        ObjectFactory objects = project.getObjects();
        task.doFirst(new QuarkusApplicationBeforeTestAction(
                project.getLayout().getProjectDirectory().getAsFile(),
                testApplicationModel.flatMap(GenerateModelTask::getApplicationModel),
                outputSourceDirectories,
                mainSourceSet.getOutput().getClassesDirs(),
                mainSourceSet.getResources().getSourceDirectories(),
                mapProperty(objects, extension.getQuarkusBuildProperties()),
                mapProperty(objects, project.provider(Map::of)),
                mapProperty(objects, project.provider(Map::of)),
                mapProperty(objects, systemPropertiesPrefixedBy("quarkus.test."))));
    }

    private FileCollection composeDevFiles() {
        return project.fileTree(project.getLayout().getProjectDirectory(), tree -> tree.include(
                "docker-compose-devservice.yml",
                "docker-compose-devservices.yml",
                "docker-compose-dev-service.yml",
                "docker-compose-dev-services.yml",
                "compose-devservice.yml",
                "compose-devservices.yml",
                "compose-dev-service.yml",
                "compose-dev-services.yml",
                "docker-compose-devservice.yaml",
                "docker-compose-devservices.yaml",
                "docker-compose-dev-service.yaml",
                "docker-compose-dev-services.yaml",
                "compose-devservice.yaml",
                "compose-devservices.yaml",
                "compose-dev-service.yaml",
                "compose-dev-services.yaml"));
    }

    private Provider<Map<String, String>> systemPropertiesPrefixedBy(String prefix) {
        ProviderFactory providers = project.getProviders();
        return providers.systemPropertiesPrefixedBy(prefix);
    }

    private static MapProperty<String, String> mapProperty(ObjectFactory objects, Provider<Map<String, String>> provider) {
        MapProperty<String, String> property = objects.mapProperty(String.class, String.class);
        property.set(provider);
        return property;
    }
}
