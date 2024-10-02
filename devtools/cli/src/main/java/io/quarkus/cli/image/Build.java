package io.quarkus.cli.image;

import java.util.Map;
import java.util.Optional;

import io.quarkus.cli.BuildToolContext;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;

@CommandLine.Command(name = "build", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Build a container image.", description = "%n"
        + "This command will build a container image for the project.", subcommands = { Docker.class,
                Podman.class,
                Buildpack.class,
                Jib.class,
                Openshift.class }, footer = { "%n"
                        + "For example (using default values), it will create a container image using docker with REPOSITORY='${user.name}/<project.artifactId>' and TAG='<project.version>'."
                        + "%n" }, headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class Build extends BaseImageCommand {

    private static final String FALLBACK_BUILDER = "docker";

    private static final Map<BuildTool, String> ACTION_MAPPING = Map.of(BuildTool.MAVEN, "quarkus:image-build",
            BuildTool.GRADLE, "imageBuild");

    @Override
    public void populateContext(BuildToolContext context) {
        super.populateContext(context);
        Map<String, String> properties = context.getPropertiesOptions().properties;
        //If no builder is configured fallback
        if (!properties.containsKey(QUARKUS_CONTAINER_IMAGE_BUILDER)) {
            properties.put(QUARKUS_CONTAINER_IMAGE_BUILDER, FALLBACK_BUILDER);
        }
        if (context.getForcedExtensions().isEmpty()) {
            context.getForcedExtensions().add(QUARKUS_CONTAINER_IMAGE_EXTENSION_KEY_PREFIX + FALLBACK_BUILDER);
        }
    }

    @Override
    public void prepareGradle(BuildToolContext context) {
        super.prepareGradle(context);
        //For gradle the builder options is meaningless so let's sepcicy the builder using `--builder`
        //This is done only for `--builder` as this is the only option that is processed from the gradle task.
        //The rest of the options are passed to the container image processors.
        Optional<String> builder = Optional
                .ofNullable(context.getPropertiesOptions().properties.remove(QUARKUS_CONTAINER_IMAGE_BUILDER));
        context.getParams().add("--builder=" + builder.orElse("docker"));
    }

    @Override
    public Map<BuildTool, String> getActionMapping() {
        return ACTION_MAPPING;
    }

    @Override
    public String toString() {
        return "Build {imageOptions='" + imageOptions + "'}";
    }
}
