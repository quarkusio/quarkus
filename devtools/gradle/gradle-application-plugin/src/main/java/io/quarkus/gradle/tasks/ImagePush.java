package io.quarkus.gradle.tasks;

import static io.quarkus.gradle.tasks.ImageCheckRequirementsTask.*;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.tasks.TaskAction;

public abstract class ImagePush extends ImageTask {

    @Inject
    public ImagePush() {
        super("Perform an image push", true);
    }

    @TaskAction
    public void imagePush() {
        Map<String, String> forcedProperties = new HashMap<String, String>();
        forcedProperties.put(QUARKUS_CONTAINER_IMAGE_BUILD, "true");
        forcedProperties.put(QUARKUS_CONTAINER_IMAGE_PUSH, "true");
        getAdditionalForcedProperties().get().getProperties().putAll(forcedProperties);
    }
}
