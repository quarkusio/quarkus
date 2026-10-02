package io.quarkus.gradle.model.tasks;

import java.io.IOException;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import io.quarkus.gradle.model.pom.PomClosureResultCodec;
import io.quarkus.gradle.model.pom.PomClosureTaskInput;

/**
 * Writes a configuration-cache-compatible POM-closure snapshot supplied through Gradle providers.
 * <p>
 * The task action performs no dependency resolution. The standard registration supplies resolved and known-missing
 * selected, parent, and imported-BOM POM results as its nested input; the output is a local absolute-path hand-off to
 * application-model generation.
 */
@DisableCachingByDefault(because = "The closure records absolute paths into Gradle's dependency cache")
public abstract class GeneratePomClosureTask extends DefaultTask {

    /** @return the nested, normalized closure input */
    @Nested
    public abstract Property<PomClosureTaskInput> getPomClosureInput();

    /** @return the local properties file written by this task */
    @OutputFile
    public abstract RegularFileProperty getPomClosureFile();

    /**
     * Serializes the configured closure input.
     *
     * @throws IOException if the output cannot be written
     */
    @TaskAction
    public void execute() throws IOException {
        PomClosureResultCodec.write(getPomClosureInput().get().getResult(),
                getPomClosureFile().get().getAsFile().toPath());
    }
}
