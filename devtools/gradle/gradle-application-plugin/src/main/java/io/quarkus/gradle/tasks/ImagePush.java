
package io.quarkus.gradle.tasks;

import java.util.Map;

import javax.inject.Inject;

public abstract class ImagePush extends ImageTask {

    @Inject
    public ImagePush(QuarkusBuildConfiguration buildConfiguration) {
        super(buildConfiguration, "Perfom an image push");
    }

    @Override
    public Map<String, String> forcedProperties() {
        return Map.of("quarkus.container-image.push", "true");
    }
}
