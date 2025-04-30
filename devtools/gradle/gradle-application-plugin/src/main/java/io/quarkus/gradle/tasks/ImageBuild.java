
package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tasks.ImageCheckRequirementsTask.QUARKUS_CONTAINER_IMAGE_BUILD;
import static io.quarkus.gradle.tasks.ImageCheckRequirementsTask.QUARKUS_CONTAINER_IMAGE_BUILDER;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.tasks.TaskAction;

public abstract class ImageBuild extends ImageTask {

    @Inject
    public ImageBuild() {
        super("Perform an image build", true);
    }

    @TaskAction
    public void imageBuild() throws IOException {
        Map<String, String> forcedProperties = new HashMap<String, String>();
        File imageBuilder = getBuilderName().get().getAsFile();
        String inputString = new String(Files.readAllBytes(imageBuilder.toPath()));
        forcedProperties.put(QUARKUS_CONTAINER_IMAGE_BUILD, "true");
        forcedProperties.put(QUARKUS_CONTAINER_IMAGE_BUILDER, inputString);
        getAdditionalForcedProperties().get().getProperties().putAll(forcedProperties);
    }
}
