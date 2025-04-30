
package io.quarkus.gradle.tasks;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

public abstract class ImageTask extends QuarkusBuildTask {

    ImageTask(String description, boolean compatible) {
        super(description, compatible);
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getBuilderName();

    @TaskAction
    public void imageTask() {
    }
}
