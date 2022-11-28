package io.quarkus.cli.image;

import java.util.Map;
import java.util.Optional;

import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;

@CommandLine.Command(name = "build", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Build a container image.", description = "%n"
        + "This command will build a container image for the project.", subcommands = { Docker.class, Buildpack.class,
                Jib.class,
                Openshift.class }, footer = { "%n"
                        + "For example (using default values), it will create a container image using docker with REPOSITORY='${user.name}/<project.artifactId>' and TAG='<project.version>'."
                        + "%n" }, headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class Build extends BaseImageCommand {

    private static final Map<BuildTool, String> ACTION_MAPPING = Map.of(BuildTool.MAVEN, "quarkus:image-build",
            BuildTool.GRADLE, "imageBuild");

    @Override
    public void prepareGradle() {
        //For gradle the builder options is meaningless so let's sepcicy the builder using `--builder`
        //This is done only for `--builder` as this is the only option that is processed from the gradle task.
        //The rest of the options are passed to the container image processors.
        Optional<String> optionalBuilder = Optional
                .ofNullable(propertiesOptions.properties.remove(QUARKUS_CONTAINER_IMAGE_BUILDER));
        String builder = optionalBuilder.orElse("docker");
        params.add("--builder=" + builder);
        forcedExtensions.add(QUARKUS_CONTAINER_IMAGE_EXTENSION_KEY_PREFIX + builder);
        // Always call super.prepareGralde after adding forcedExtension or else forcedExtensions will be ignored.
        super.prepareGradle();
    }

    @Override
    public Optional<String> getAction() {
        return Optional.ofNullable(ACTION_MAPPING.get(getRunner().getBuildTool()));
    }

    @Override
    public String toString() {
        return "Build {imageOptions='" + imageOptions + "'}";
    }
}
