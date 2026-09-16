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

/**
 * Generates the marker artifact published by a Quarkus extension deployment project.
 * <p>
 * The {@code io.quarkus.extension.deployment} plugin registers this task under
 * {@link QuarkusExtensionDeploymentPlugin#MARKER_TASK_NAME}. Its output is consumed through the plugin-owned marker
 * variant so a runtime extension project can verify that its local deployment project applies the deployment plugin.
 * The marker is deterministic and build-cacheable.
 * <p>
 * This is plugin-owned Gradle infrastructure, not a general task-registration API. The supported contract covers the
 * plugin-registered task and its documented name and output; no compatibility commitment is made for direct
 * construction, additional registration, or subclassing.
 */
@CacheableTask
public abstract class QuarkusExtensionDeploymentMarkerTask extends DefaultTask {

    /**
     * Returns the marker file published by the deployment marker variant.
     *
     * @return the marker file
     */
    @OutputFile
    public abstract RegularFileProperty getMarkerFile();

    /**
     * Writes a marker containing the deployment plugin ID.
     *
     * @throws IOException when the marker cannot be written
     */
    @TaskAction
    public void generateMarker() throws IOException {
        Path markerFile = getMarkerFile().get().getAsFile().toPath();
        Files.createDirectories(markerFile.getParent());
        Files.writeString(markerFile, ExtensionVariantConstants.EXTENSION_DEPLOYMENT_PLUGIN_ID + "\n",
                StandardCharsets.UTF_8);
    }
}
