package io.quarkus.cli.image;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import io.quarkus.cli.BuildToolContext;
import picocli.CommandLine;

@CommandLine.Command(name = "openshift", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Build a container image using OpenShift.", description = "%n"
        + "This command will build or push a container image for the project, using Openshift.", footer = "%n"
                + "For example (using default values), it will create a container image in OpenShift using the docker build strategy with REPOSITORY='${user.name}/<project.artifactId>' and TAG='<project.version>'.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class Openshift extends BaseImageSubCommand implements Callable<Integer> {

    private static final String OPENSHIFT = "openshift";
    private static final String OPENSHIFT_CONFIG_PREFIX = "quarkus.openshift.";
    private static final String BASE_JVM_IMAGE = "base-jvm-image";
    private static final String JVM_ARGUMENTS = "jvm-arguments";
    private static final String JVM_DOCKERFILE = "jvm-dockerfile";
    private static final String JAR_DIRECTORY = "jar-directory";
    private static final String JAR_FILE_NAME = "jar-file-name";

    private static final String BASE_NATIVE_IMAGE = "base-native-image";
    private static final String NATIVE_ARGUMENTS = "native-arguments";
    private static final String NATIVE_DOCKERFILE = "native-dockerfile";
    private static final String NATIVE_DIRECTORY = "native-directory";
    private static final String NATIVE_FILE_NAME = "native-file-name";

    private static final String BUILD_STRATEGY = "build-strategy";
    private static final String BUILD_TIMEOUT = "build-timeout";

    @CommandLine.Option(order = 7, names = { "--build-strategy" }, description = "The build strategy to use (docker or s2i).")
    public Optional<String> buildStrategy;

    @CommandLine.Option(order = 8, names = { "--base-image" }, description = "The base image to use.")
    public Optional<String> baseImage = Optional.empty();

    @CommandLine.Option(order = 9, names = {
            "--arg" }, description = "Additional argument to pass when starting the application.")
    public List<String> arguments = new ArrayList<>();

    @CommandLine.Option(order = 10, names = { "--dockerfile" }, description = "The dockerfile of the container image.")
    public String dockerfile;

    @CommandLine.Option(order = 11, names = {
            "--artifact-directory" }, description = "The directory where the jar/native binary is added during the assemble phase.")
    public String artifactDirectory;

    @CommandLine.Option(order = 12, names = { "--artifact-filename" }, description = "The filename of the jar/native binary.")
    public String artifactFilename;

    @CommandLine.Option(order = 13, names = { "--build-timeout" }, description = "The build timeout.")
    public String buildTimeout;

    @Override
    public void populateContext(BuildToolContext context) {
        Map<String, String> properties = context.getPropertiesOptions().properties;
        properties.put(QUARKUS_CONTAINER_IMAGE_BUILDER, OPENSHIFT);
        properties.put(OPENSHIFT_CONFIG_PREFIX + BUILD_STRATEGY, buildStrategy.orElse("docker"));

        baseImage.ifPresent(d -> properties
                .put(OPENSHIFT_CONFIG_PREFIX + (context.getBuildOptions().buildNative ? BASE_NATIVE_IMAGE : BASE_JVM_IMAGE),
                        d));

        if (!arguments.isEmpty()) {
            String joinedArgs = arguments.stream().collect(Collectors.joining(","));
            properties.put(OPENSHIFT_CONFIG_PREFIX + (context.getBuildOptions().buildNative ? NATIVE_ARGUMENTS : JVM_ARGUMENTS),
                    joinedArgs);
        }

        if (dockerfile != null && !dockerfile.isEmpty()) {
            properties.put(
                    OPENSHIFT_CONFIG_PREFIX + (context.getBuildOptions().buildNative ? NATIVE_DOCKERFILE : JVM_DOCKERFILE),
                    dockerfile);
        }

        if (artifactDirectory != null && !artifactDirectory.isEmpty()) {
            properties.put(OPENSHIFT_CONFIG_PREFIX + (context.getBuildOptions().buildNative ? NATIVE_DIRECTORY : JAR_DIRECTORY),
                    artifactDirectory);
        }

        if (artifactFilename != null && !artifactFilename.isEmpty()) {
            properties.put(OPENSHIFT_CONFIG_PREFIX + (context.getBuildOptions().buildNative ? NATIVE_FILE_NAME : JAR_FILE_NAME),
                    artifactFilename);
        }

        context.getForcedExtensions().add(QUARKUS_CONTAINER_IMAGE_EXTENSION_KEY_PREFIX + OPENSHIFT);
    }

    @Override
    public String toString() {
        return "Openshift {imageOptions='" + imageOptions + "', baseImage:'" + baseImage.orElse("<none>") + "'}";
    }
}
