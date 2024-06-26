package io.quarkus.cli.image;

import java.util.Optional;

import io.quarkus.cli.BuildToolContext;
import picocli.CommandLine;

@CommandLine.Command(name = "podman", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Build a container image using Podman.", description = "%n"
        + "This command will build or push a container image for the project, using Podman.", footer = "%n"
                + "For example (using default values), it will create a container image using with REPOSITORY='${user.name}/<project.artifactId>' and TAG='<project.version>'.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class Podman extends BaseImageSubCommand {

    private static final String PODMAN = "podman";
    private static final String PODMAN_CONFIG_PREFIX = "quarkus.podman.";
    private static final String DOCKERFILE_JVM_PATH = "dockerfile-jvm-path";
    private static final String DOCKERFILE_NATIVE_PATH = "dockerfile-native-path";

    @CommandLine.Option(order = 7, names = { "--dockerfile" }, description = "The path to the Dockerfile.")
    public Optional<String> dockerFile;

    @Override
    public void populateContext(BuildToolContext context) {
        var properties = context.getPropertiesOptions().properties;
        properties.put(QUARKUS_CONTAINER_IMAGE_BUILDER, PODMAN);

        dockerFile.ifPresent(d -> properties.put(
                PODMAN_CONFIG_PREFIX + (context.getBuildOptions().buildNative ? DOCKERFILE_NATIVE_PATH : DOCKERFILE_JVM_PATH),
                d));

        context.getForcedExtensions().add(QUARKUS_CONTAINER_IMAGE_EXTENSION_KEY_PREFIX + PODMAN);
    }

    @Override
    public String toString() {
        return "Podman {imageOptions='" + imageOptions + "', dockerFile:'" + dockerFile.orElse("<none>") + "'}";
    }
}
