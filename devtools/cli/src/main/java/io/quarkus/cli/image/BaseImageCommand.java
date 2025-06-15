package io.quarkus.cli.image;

import java.util.Map;
import java.util.concurrent.Callable;

import io.quarkus.cli.BuildToolContext;
import io.quarkus.cli.BuildToolDelegatingCommand;
import picocli.CommandLine;

public class BaseImageCommand extends BuildToolDelegatingCommand implements Callable<Integer> {

    private static final String QUARKUS_CONTAINER_IMAGE_GROUP = "quarkus.container-image.group";
    private static final String QUARKUS_CONTAINER_IMAGE_NAME = "quarkus.container-image.name";
    private static final String QUARKUS_CONTAINER_IMAGE_TAG = "quarkus.container-image.tag";

    protected static final String QUARKUS_CONTAINER_IMAGE_EXTENSION_KEY_PREFIX = "io.quarkus:quarkus-container-image-";
    protected static final String QUARKUS_CONTAINER_IMAGE_BUILDER = "quarkus.container-image.builder";
    protected static final String QUARKUS_CONTAINER_IMAGE_REGISTRY = "quarkus.container-image.registry";

    @CommandLine.ArgGroup(order = 2, exclusive = false, validate = false, heading = "%nImage options:%n")
    ImageOptions imageOptions = new ImageOptions();

    @Override
    public void populateContext(BuildToolContext context) {
        super.populateContext(context);
        Map<String, String> properties = context.getPropertiesOptions().properties;
        imageOptions.group.ifPresent(group -> properties.put(QUARKUS_CONTAINER_IMAGE_GROUP, group));
        imageOptions.name.ifPresent(name -> properties.put(QUARKUS_CONTAINER_IMAGE_NAME, name));
        imageOptions.tag.ifPresent(tag -> properties.put(QUARKUS_CONTAINER_IMAGE_TAG, tag));
        imageOptions.registry.ifPresent(registry -> properties.put(QUARKUS_CONTAINER_IMAGE_REGISTRY, registry));
    }

    public ImageOptions getImageOptions() {
        return imageOptions;
    }

    public void setImageOptions(ImageOptions imageOptions) {
        this.imageOptions = imageOptions;
    }
}
