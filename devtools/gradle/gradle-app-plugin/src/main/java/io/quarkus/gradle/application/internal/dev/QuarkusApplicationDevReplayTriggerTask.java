package io.quarkus.gradle.application.internal.dev;

import java.io.IOException;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "The replay trigger is mutable build-session notification state")
public abstract class QuarkusApplicationDevReplayTriggerTask extends DefaultTask {

    @OutputFile
    public abstract RegularFileProperty getTriggerFile();

    @TaskAction
    public final void initialize() throws IOException {
        ContinuousBuildTriggerFile.initialize(getTriggerFile().get().getAsFile().toPath());
    }
}
