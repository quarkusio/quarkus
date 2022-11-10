package io.quarkus.cli.image;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import io.quarkus.cli.build.BaseBuildCommand;
import io.quarkus.cli.common.BuildOptions;
import io.quarkus.cli.common.PropertiesOptions;
import io.quarkus.cli.common.RunModeOption;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

public class BaseImageCommand extends BaseBuildCommand implements Callable<Integer> {

    protected static final String QUARKUS_FORCED_EXTENSIONS = "quarkus.application.forced-extensions";
    protected static final String QUARKUS_CONTAINER_IMAGE_EXTENSION_KEY_PREFIX = "io.quarkus:quarkus-container-image-";

    protected static final String QUARKUS_CONTAINER_IMAGE_BUILDER = "quarkus.container-image.builder";
    private static final String QUARKUS_CONTAINER_IMAGE_GROUP = "quarkus.container-image.group";
    private static final String QUARKUS_CONTAINER_IMAGE_NAME = "quarkus.container-image.name";
    private static final String QUARKUS_CONTAINER_IMAGE_TAG = "quarkus.container-image.tag";
    protected static final String QUARKUS_CONTAINER_IMAGE_REGISTRY = "quarkus.container-image.registry";
    protected static final String QUARKUS_CONTAINER_IMAGE_DRY_RUN = "quarkus.container-image.dry-run";

    protected static final Map<BuildTool, String> ACTION_MAPPING = Map.of(BuildTool.MAVEN, "quarkus:image-build",
            BuildTool.GRADLE, "imageBuild");

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
        imageOptions.name.ifPresent(name -> properties.put(QUARKUS_CONTAINER_IMAGE_NAME, name));
        imageOptions.tag.ifPresent(tag -> properties.put(QUARKUS_CONTAINER_IMAGE_TAG, tag));
        imageOptions.registry.ifPresent(registry -> properties.put(QUARKUS_CONTAINER_IMAGE_REGISTRY, registry));
        if (runMode.isDryRun()) {
            properties.put(QUARKUS_CONTAINER_IMAGE_DRY_RUN, "true");
        }
    }

    public Optional<String> getAction() {
        return Optional.empty();
    }

    public PropertiesOptions getPropertiesOptions() {
        return this.propertiesOptions;
    }

    @Override
    public Integer call() throws Exception {
        return 0;
    }
}
