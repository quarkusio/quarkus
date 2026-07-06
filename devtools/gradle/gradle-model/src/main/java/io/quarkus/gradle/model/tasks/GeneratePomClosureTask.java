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

@DisableCachingByDefault(because = "The closure records absolute paths into Gradle's dependency cache")
public abstract class GeneratePomClosureTask extends DefaultTask {

    @Nested
    public abstract Property<PomClosureTaskInput> getPomClosureInput();

    @OutputFile
    public abstract RegularFileProperty getPomClosureFile();

    @TaskAction
    public void execute() throws IOException {
        PomClosureResultCodec.write(getPomClosureInput().get().getResult(),
                getPomClosureFile().get().getAsFile().toPath());
    }
}
