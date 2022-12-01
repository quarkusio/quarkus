
package io.quarkus.gradle.tasks;

import java.util.Map;

public abstract class ImageTask extends QuarkusBuildProviderTask {

    public ImageTask(QuarkusBuildConfiguration buildConfiguration, String description) {
        super(buildConfiguration, description);
    }

    @Override
    public Map<String, String> forcedProperties() {
        return Map.of("quarkus.container-image.build", "true");
    }
}
