
package io.quarkus.gradle.tasks;

import java.util.Optional;

import javax.inject.Inject;

import org.gradle.api.provider.MapProperty;
import org.gradle.api.tasks.options.Option;

public abstract class ImageBuild extends ImageTask {

    Optional<Builder> builder = Optional.empty();

    @Option(option = "builder", description = "The container image extension to use for building the image (e.g. docker, jib, buildpack, openshift).")
    public void setBuilder(Builder builder) {
        this.builder = Optional.of(builder);
    }

    @Inject
    public ImageBuild() {
        super("Perform an image build");
        MapProperty<String, String> forcedProperties = extension().forcedPropertiesProperty();
        forcedProperties.put(QUARKUS_CONTAINER_IMAGE_BUILD, "true");
        forcedProperties.put(QUARKUS_CONTAINER_IMAGE_BUILDER, getProject().provider(() -> builder().name()));
    }
}
