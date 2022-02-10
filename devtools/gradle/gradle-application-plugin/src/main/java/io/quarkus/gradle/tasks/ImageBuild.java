
package io.quarkus.gradle.tasks;

import java.util.Map;

import javax.inject.Inject;

public abstract class ImageBuild extends ImageTask {

    @Inject
    public ImageBuild(QuarkusBuildConfiguration buildConfiguration) {
        super(buildConfiguration, "Perform an image build");
    }

    @Override
    public Map<String, String> forcedProperties() {
        return Map.of("quarkus.container-image.build", "true");
    }
}
