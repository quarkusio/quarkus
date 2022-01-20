package io.quarkus.cli.image;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.quarkus.cli.build.BaseBuildCommand;
import io.quarkus.cli.common.BuildOptions;
import io.quarkus.cli.common.RunModeOption;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

public class BaseImageCommand extends BaseBuildCommand {

    protected static final String QUARKUS_FORCED_EXTENSIONS = "quarkus.application.forced-extensions";
    protected static final String QUARKUS_CONTAINER_IMAGE_EXTENSION_KEY_PREFIX = "io.quarkus:quarkus-container-image-";

    protected static final String QUARKUS_CONTAINER_IMAGE_BUILDER = "quarkus.container-image.builder";
    private static final String QUARKUS_CONTAINER_IMAGE_GROUP = "quarkus.container-image.group";
    private static final String QUARKUS_CONTAINER_IMAGE_NAME = "quarkus.container-image.name";
    private static final String QUARKUS_CONTAINER_IMAGE_TAG = "quarkus.container-image.tag";
    private static final String QUARKUS_CONTAINER_IMAGE_REGISTRY = "quarkus.container-image.registry";

    @CommandLine.Mixin
    protected RunModeOption runMode;

    @CommandLine.ArgGroup(order = 1, exclusive = false, validate = false, heading = "%nBuild options:%n")
    BuildOptions buildOptions = new BuildOptions();

    @CommandLine.ArgGroup(order = 2, exclusive = false, validate = false, heading = "%nImage options:%n")
    ImageOptions imageOptions = new ImageOptions();

    @Parameters(description = "Additional parameters passed to the build system")
    List<String> params = new ArrayList<>();

    public void populateImageConfiguration(Map<String, String> properties) {
        imageOptions.group.ifPresent(group -> properties.put(QUARKUS_CONTAINER_IMAGE_GROUP, group));
        imageOptions.group.ifPresent(name -> properties.put(QUARKUS_CONTAINER_IMAGE_NAME, name));
        imageOptions.group.ifPresent(tag -> properties.put(QUARKUS_CONTAINER_IMAGE_TAG, tag));
        imageOptions.group.ifPresent(registry -> properties.put(QUARKUS_CONTAINER_IMAGE_REGISTRY, registry));
    }
}
