package io.quarkus.gradle.application.tasks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.bootstrap.app.ApplicationModelSerializer;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelDiagnosticsFormatter;

/**
 * Writes and displays a deterministic, human-readable report of a generated Quarkus application model.
 * <p>
 * The plugin registers application, development, and test model diagnostic tasks. Each task always executes, writes its
 * report file, and logs the report. Reports contain project-specific absolute paths and are therefore not build-cacheable.
 * <p>
 * The supported compatibility contract covers plugin-registered instances and the documented task names and outputs.
 * No compatibility commitment is made for direct construction, additional registration, or subclassing.
 */
@DisableCachingByDefault(because = "Application-model diagnostics contain project-specific, non-relocatable paths")
public abstract class QuarkusApplicationShowModelTask extends DefaultTask {

    /**
     * Creates a model diagnostic task that always writes and logs its report.
     */
    public QuarkusApplicationShowModelTask() {
        getOutputs().upToDateWhen(task -> false);
    }

    /**
     * Returns the display name used to identify the model in the report.
     *
     * @return the model name
     */
    @Input
    public abstract Property<String> getModelName();

    /**
     * Returns the project-directory path used to abbreviate report paths.
     *
     * @return the project-directory path
     */
    @Input
    public abstract Property<String> getProjectDirectoryPath();

    /**
     * Returns the build-root path used to abbreviate report paths.
     *
     * @return the build-root path
     */
    @Input
    public abstract Property<String> getBuildRootPath();

    /**
     * Returns the serialized application model to report.
     *
     * @return the application-model file
     */
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getApplicationModel();

    /**
     * Returns the human-readable report file written by the task.
     *
     * @return the report file
     */
    @OutputFile
    public abstract RegularFileProperty getReportFile();

    /**
     * Deserializes the application model, writes its deterministic report, and logs the report location.
     *
     * @throws IOException when the report cannot be written
     */
    @TaskAction
    public void showModel() throws IOException {
        Path modelPath = getApplicationModel().get().getAsFile().toPath();
        ApplicationModel model = ApplicationModelSerializer.deserialize(modelPath);
        String diagnostics = ApplicationModelDiagnosticsFormatter.format(
                getModelName().get(), model, Path.of(getProjectDirectoryPath().get()),
                Path.of(getBuildRootPath().get()));
        Path reportPath = getReportFile().get().getAsFile().toPath();
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, diagnostics + '\n', StandardCharsets.UTF_8);
        getLogger().lifecycle("{}\nApplication model report: {}", diagnostics, reportPath);
    }
}
