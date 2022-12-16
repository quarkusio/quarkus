package io.quarkus.cli.image;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import picocli.CommandLine;
import picocli.CommandLine.ParentCommand;

@CommandLine.Command(name = "buildpack", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Build a container image using Buildpack.", description = "%n"
        + "This command will build or push a container image for the project, using Buildpack.", footer = "%n"
                + "For example (using default values), it will create a container image using with REPOSITORY='${user.name}/<project.artifactId>' and TAG='<project.version>'.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class Buildpack extends BaseImageCommand {

    private static final String BUILDPACK = "buildpack";
    private static final String BUILDPACK_CONFIG_PREFIX = "quarkus.buildpack.";
    private static final String JVM_BUILDER_IMAGE = "jvm-builder-image";
    private static final String NATIVE_BUILDER_IMAGE = "native-builder-image";
    private static final String BUILDER_REGISTRY_USERNAME = "base-registry-username";
    private static final String BUILDER_REGISTRY_PASSWORD = "base-registry-password";
    private static final String PULL_TIMEOUT_SECONDS = "pull-timeout-seconds";
    private static final String RUN_IMAGE = "run-image";
    private static final String BUILDER_ENV = "builder-env";
    private static final String DOCKER_HOST = "docker-host";

    @CommandLine.Option(order = 7, names = { "--builder-image" }, description = "The builder image to use.")
    public Optional<String> builderImage = Optional.empty();

    @CommandLine.Option(order = 8, names = {
            "--builder-registry-username" }, description = "The builder image registry username.")
    public Optional<String> builderRegistryUsername = Optional.empty();

    @CommandLine.Option(order = 9, names = {
            "--builder-registry-password" }, description = "The builder image registry password.")
    public Optional<String> builderRegistryPassword = Optional.empty();

    @CommandLine.Option(order = 10, names = {
            "--pull-image-timeout" }, description = "The amount of time to wait in seconds for pulling the builder image.")
    public Optional<Integer> pullTimeout = Optional.empty();

    @CommandLine.Option(order = 11, names = { "--run-image" }, description = "The run image to use.")
    public Optional<String> runImage = Optional.empty();

    @CommandLine.Option(order = 12, names = { "--docker-host" }, description = "The docker host.")
    public Optional<String> dockerHost = Optional.empty();

    @CommandLine.Option(order = 13, names = { "--build-env" }, description = "A build environment variable.")
    public Map<String, String> buildEnv = new HashMap<>();

    @ParentCommand
    BaseImageCommand parent;

    @Override
    public Integer call() throws Exception {
        Map<String, String> properties = parent.getPropertiesOptions().properties;
        properties.put(QUARKUS_CONTAINER_IMAGE_BUILDER, BUILDPACK);

        builderImage.ifPresent(i -> properties
                .put(BUILDPACK_CONFIG_PREFIX + (buildOptions.buildNative ? NATIVE_BUILDER_IMAGE : JVM_BUILDER_IMAGE), i));
        builderRegistryUsername.ifPresent(u -> properties.put(BUILDPACK_CONFIG_PREFIX + BUILDER_REGISTRY_USERNAME, u));
        builderRegistryPassword.ifPresent(p -> properties.put(BUILDPACK_CONFIG_PREFIX + BUILDER_REGISTRY_PASSWORD, p));
        pullTimeout.ifPresent(t -> properties.put(BUILDPACK_CONFIG_PREFIX + PULL_TIMEOUT_SECONDS, t.toString()));
        dockerHost.ifPresent(h -> properties.put(BUILDPACK_CONFIG_PREFIX + DOCKER_HOST, h));
        runImage.ifPresent(i -> properties.put(BUILDPACK_CONFIG_PREFIX + RUN_IMAGE, i));
        buildEnv.forEach((k, v) -> properties.put(BUILDPACK_CONFIG_PREFIX + BUILDER_ENV + "." + k, v));

        parent.getForcedExtensions().add(QUARKUS_CONTAINER_IMAGE_EXTENSION_KEY_PREFIX + BUILDPACK);
        parent.runMode = runMode;
        parent.buildOptions = buildOptions;
        parent.imageOptions = imageOptions;
        parent.setOutput(output);
        return parent.call();
    }

    @Override
    public List<String> getForcedExtensions() {
        return Arrays.asList(QUARKUS_CONTAINER_IMAGE_EXTENSION_KEY_PREFIX + BUILDPACK);
    }

    @Override
    public String toString() {
        return "Buildpack {imageOptions:'" + imageOptions + "', buildEnv:'" + buildEnv + "'', builderImage:'" + builderImage
                + "', builderRegistryPassword:'"
                + builderRegistryPassword + "', builderRegistryUsername:'" + builderRegistryUsername + "', dockerHost:'"
                + dockerHost + "', parent:'" + parent + "', pullTimeout:'" + pullTimeout + "', runImage:'" + runImage + "'}";
    }
}
