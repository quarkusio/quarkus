package io.quarkus.cli.image;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.cli.BuildToolContext;
import picocli.CommandLine;

@CommandLine.Command(name = "jib", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Build a container image using Jib.", description = "%n"
        + "This command will build or push a container image for the project, using Jib.", footer = "%n"
                + "For example (using default values), it will create a container image using with REPOSITORY='${user.name}/<project.artifactId>' and TAG='<project.version>'.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class Jib extends BaseImageSubCommand {

    private static final String JIB = "jib";
    private static final String JIB_CONFIG_PREFIX = "quarkus.jib.";
    private static final String BASE_JVM_IMAGE = "base-jvm-image";
    private static final String JVM_ARGUMENTS = "jvm-arguments";
    private static final String JVM_ENTRYPOINT = "jvm-entrypoint";
    private static final String BASE_NATIVE_IMAGE = "base-native-image";
    private static final String NATIVE_ARGUMENTS = "native-arguments";
    private static final String NATIVE_ENTRYPOINT = "native-entrypoint";
    private static final String LABELS = "labels.";
    private static final String ENV_VARS = "environment-varialbes.";
    private static final String PORTS = "ports";
    private static final String PLATFORMS = "platforms";
    private static final String IMAGE_DIGEST_FILE = "image-digest-file";
    private static final String IMAGE_ID_FILE = "image-id-file";
    private static final String OFFLINE_MODE = "offline-mode";

    private static final String USER = "user";

    @CommandLine.Option(order = 7, names = { "--base-image" }, description = "The base image to use.")
    public Optional<String> baseImage;

    @CommandLine.Option(order = 8, names = {
            "--arg" }, description = "Additional argument to pass when starting the application.")
    public List<String> arguments = new ArrayList<>();

    @CommandLine.Option(order = 9, names = { "--entrypoint" }, description = "The entrypoint of the container image.")
    public List<String> entrypoint = new ArrayList<>();

    @CommandLine.Option(order = 10, names = {
            "--env" }, description = "Environment variables to add to the container image.")
    public Map<String, String> environmentVariables = new HashMap<>();

    @CommandLine.Option(order = 11, names = { "--label" }, description = "Custom labels to add to the generated image.")
    public Map<String, String> labels = new HashMap<>();

    @CommandLine.Option(order = 12, names = { "--port" }, description = "The ports to expose.")
    public List<Integer> ports = new ArrayList<>();

    @CommandLine.Option(order = 13, names = { "--user" }, description = "The user in the generated image.")
    public String user;

    @CommandLine.Option(order = 14, names = {
            "--always-cache-base-image" }, description = "Controls the optimization which skips downloading base image layers that exist in a target registry.")
    public boolean alwaysCacheBaseImage;

    @CommandLine.Option(order = 15, names = {
            "--platform" }, description = "The target platforms defined using the pattern <os>[/arch][/variant]|<os>/<arch>[/variant]")
    public Set<String> platforms = new HashSet<>();

    @CommandLine.Option(order = 16, names = {
            "--image-digest-file" }, description = "The path of a file that will be written containing the digest of the generated image.")
    public String imageDigestFile;

    @CommandLine.Option(order = 17, names = {
            "--image-id-file" }, description = "The path of a file that will be written containing the id of the generated image.")
    public String imageIdFile;

    @Override
    public void populateContext(BuildToolContext context) {
        super.populateContext(context);
        Map<String, String> properties = context.getPropertiesOptions().properties;

        properties.put(QUARKUS_CONTAINER_IMAGE_BUILDER, JIB);
        baseImage.ifPresent(d -> properties.put(
                JIB_CONFIG_PREFIX + (context.getBuildOptions().buildNative ? BASE_NATIVE_IMAGE : BASE_JVM_IMAGE), d));

        if (!arguments.isEmpty()) {
            String joinedArgs = arguments.stream().collect(Collectors.joining(","));
            properties.put(
                    JIB_CONFIG_PREFIX + (context.getBuildOptions().buildNative ? NATIVE_ARGUMENTS : JVM_ARGUMENTS),
                    joinedArgs);
        }

        if (!entrypoint.isEmpty()) {
            String joinedEntrypoint = entrypoint.stream().collect(Collectors.joining(","));
            properties.put(
                    JIB_CONFIG_PREFIX + (context.getBuildOptions().buildNative ? NATIVE_ENTRYPOINT : JVM_ENTRYPOINT),
                    joinedEntrypoint);
        }

        if (!environmentVariables.isEmpty()) {
            environmentVariables.forEach((key, value) -> {
                properties.put(JIB_CONFIG_PREFIX + ENV_VARS + key, value);
            });
        }

        if (!labels.isEmpty()) {
            labels.forEach((key, value) -> {
                properties.put(JIB_CONFIG_PREFIX + LABELS + key, value);
            });
        }

        if (!ports.isEmpty()) {
            String joinedPorts = ports.stream().map(String::valueOf).collect(Collectors.joining(","));
            properties.put(JIB_CONFIG_PREFIX + PORTS, joinedPorts);
        }

        if (!platforms.isEmpty()) {
            String joinedPlatforms = platforms.stream().collect(Collectors.joining(","));
            properties.put(JIB_CONFIG_PREFIX + PLATFORMS, joinedPlatforms);
        }

        if (user != null && !user.isEmpty()) {
            properties.put(JIB_CONFIG_PREFIX + USER, user);
        }

        if (imageDigestFile != null && !imageDigestFile.isEmpty()) {
            properties.put(JIB_CONFIG_PREFIX + IMAGE_DIGEST_FILE, imageDigestFile);
        }

        if (imageIdFile != null && !imageIdFile.isEmpty()) {
            properties.put(JIB_CONFIG_PREFIX + IMAGE_ID_FILE, imageIdFile);
        }

        if (context.getBuildOptions().offline) {
            properties.put(JIB_CONFIG_PREFIX + OFFLINE_MODE, "true");
        }

        context.getForcedExtensions().add(QUARKUS_CONTAINER_IMAGE_EXTENSION_KEY_PREFIX + JIB);
    }

    @Override
    public String toString() {
        return "Jib {imageOptions='" + imageOptions + "', baseImage:'" + baseImage.orElse("<none>") + "'}";
    }
}
