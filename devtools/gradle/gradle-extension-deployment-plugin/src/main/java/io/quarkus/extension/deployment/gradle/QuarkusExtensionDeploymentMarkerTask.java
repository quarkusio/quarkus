package io.quarkus.extension.deployment.gradle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import io.quarkus.gradle.model.config.ExtensionVariantConstants;

@CacheableTask
public abstract class QuarkusExtensionDeploymentMarkerTask extends DefaultTask {

    @OutputFile
    public abstract RegularFileProperty getMarkerFile();

    @TaskAction
    public void generateMarker() throws IOException {
        Path markerFile = getMarkerFile().get().getAsFile().toPath();
        Files.createDirectories(markerFile.getParent());
        Files.writeString(markerFile, ExtensionVariantConstants.EXTENSION_DEPLOYMENT_PLUGIN_ID + System.lineSeparator(),
                StandardCharsets.UTF_8);
    }
}
