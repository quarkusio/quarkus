package io.quarkus.gradle.application.internal.plugin;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.testing.Test;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gradle.application.internal.config.EffectiveConfigPlan;
import io.quarkus.gradle.application.internal.config.EffectiveConfigPlanner;
import io.quarkus.gradle.application.internal.config.EffectiveConfigRequest;
import io.quarkus.gradle.tooling.ToolingUtils;
import io.quarkus.runtime.LaunchMode;

final class QuarkusApplicationBeforeTestAction implements Action<Task> {

    private final File projectDirectory;
    private final Provider<RegularFile> applicationModelPath;
    private final FileCollection outputSourceDirectories;
    private final FileCollection mainClassesDirectories;
    private final FileCollection sourceDirectories;
    private final MapProperty<String, String> quarkusBuildProperties;
    private final MapProperty<String, String> gradleProperties;
    private final MapProperty<String, String> environmentVariables;
    private final MapProperty<String, String> systemProperties;

    QuarkusApplicationBeforeTestAction(
            File projectDirectory,
            Provider<RegularFile> applicationModelPath,
            FileCollection outputSourceDirectories,
            FileCollection mainClassesDirectories,
            FileCollection sourceDirectories,
            MapProperty<String, String> quarkusBuildProperties,
            MapProperty<String, String> gradleProperties,
            MapProperty<String, String> environmentVariables,
            MapProperty<String, String> systemProperties) {
        this.projectDirectory = projectDirectory;
        this.applicationModelPath = applicationModelPath;
        this.outputSourceDirectories = outputSourceDirectories;
        this.mainClassesDirectories = mainClassesDirectories;
        this.sourceDirectories = sourceDirectories;
        this.quarkusBuildProperties = quarkusBuildProperties;
        this.gradleProperties = gradleProperties;
        this.environmentVariables = environmentVariables;
        this.systemProperties = systemProperties;
    }

    @Override
    public void execute(Task task) {
        try {
            Test test = (Test) task;
            Path serializedModel = applicationModelPath.get().getAsFile().toPath();
            ApplicationModel applicationModel = ToolingUtils.deserializeAppModel(serializedModel);
            EffectiveConfigPlan effectiveConfig = new EffectiveConfigPlanner().plan(new EffectiveConfigRequest(
                    Map.of(),
                    applicationModel.getAppArtifact().getArtifactId(),
                    applicationModel.getAppArtifact().getVersion(),
                    sourceDirectories.getFiles(),
                    quarkusBuildProperties.get(),
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    gradleProperties.get(),
                    environmentVariables.get(),
                    systemProperties.get(),
                    Map.of(),
                    LaunchMode.TEST.getDefaultProfile()));

            Map<String, Object> testSystemProperties = test.getSystemProperties();
            testSystemProperties.putAll(effectiveConfig.quarkusWorkerValues());
            String activeTestProfile = effectiveConfig.fullValues().get(LaunchMode.TEST.getProfileKey());
            if (activeTestProfile != null) {
                testSystemProperties.put(LaunchMode.TEST.getProfileKey(), activeTestProfile);
            }
            testSystemProperties.put(BootstrapConstants.SERIALIZED_TEST_APP_MODEL, serializedModel.toString());
            testSystemProperties.put(BootstrapConstants.OUTPUT_SOURCES_DIR, outputSourceDirectories());
            test.environment(BootstrapConstants.TEST_TO_MAIN_MAPPINGS, testToMainMappings(test));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure Quarkus test task", e);
        }
    }

    private String outputSourceDirectories() {
        StringJoiner outputSourcesDir = new StringJoiner(",");
        for (File outputSourceDir : outputSourceDirectories.getFiles()) {
            outputSourcesDir.add(outputSourceDir.getAbsolutePath());
        }
        return outputSourcesDir.toString();
    }

    private String testToMainMappings(Test task) {
        File mainClassesDirectory = mainClassesDirectories.getFiles().stream()
                .filter(File::exists)
                .findFirst()
                .orElseGet(() -> mainClassesDirectories.getFiles().stream()
                        .findFirst()
                        .orElse(projectDirectory));
        Path projectPath = projectDirectory.toPath();
        Path mainClassesPath = projectPath.relativize(mainClassesDirectory.toPath());
        return task.getTestClassesDirs().getFiles().stream()
                .filter(File::exists)
                .distinct()
                .map(testClassesDir -> "%s:%s".formatted(
                        projectPath.relativize(testClassesDir.toPath()),
                        mainClassesPath))
                .collect(Collectors.joining(","));
    }
}
