
package io.quarkus.gradle.tasks;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.gradle.api.tasks.options.Option;

public abstract class ImageBuild extends ImageTask {

    Optional<Builder> builder = Optional.empty();

    @Option(option = "builder", description = "The container image extension to use for building the image (e.g. docker, jib, buildpack, openshift).")
    public void setBuilder(Builder builder) {
        this.builder = Optional.of(builder);
    }

    @Inject
    public ImageBuild(QuarkusBuildConfiguration buildConfiguration) {
        super(buildConfiguration, "Perform an image build");
    }

    @Override
    public Map<String, String> forcedProperties() {
        return builder().map(b -> Map.of(QUARKUS_CONTAINER_IMAGE_BUILD, "true", QUARKUS_CONTAINER_IMAGE_BUILDER, b.name()))
                .orElse(Collections.emptyMap());
    }

    public Optional<Builder> builder() {
        return builder
                .or(() -> builderFromSystemProperties())
                .or(() -> availableBuilders().stream().findFirst());
    }
}
