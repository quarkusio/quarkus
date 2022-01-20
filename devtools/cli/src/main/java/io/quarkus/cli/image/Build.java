package io.quarkus.cli.image;

import java.util.Map;
import java.util.concurrent.Callable;

import io.quarkus.cli.build.BuildSystemRunner;
import picocli.CommandLine;

@CommandLine.Command(name = "build", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Build a container image.", description = "%n"
        + "This command will build a container image for the project.", subcommands = { Docker.class, Jib.class,
                Openshift.class }, footer = { "%n"
                        + "For example (using default values), it will create a container image using docker with REPOSITORY='${user.name}/<project.artifactId>' and TAG='<project.version>'."
                        + "%n" }, headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class Build extends BaseImageCommand implements Callable<Integer> {

    private static final String QUARKUS_CONTAINER_IMAGE_BUILD = "quarkus.container-image.build";

    @Override
    public void populateImageConfiguration(Map<String, String> properties) {
        super.populateImageConfiguration(properties);
        properties.put(QUARKUS_CONTAINER_IMAGE_BUILD, "true");
    }

    @Override
    public Integer call() throws Exception {
        try {
            populateImageConfiguration(propertiesOptions.properties);
            BuildSystemRunner runner = getRunner();
            BuildSystemRunner.BuildCommandArgs commandArgs = runner.prepareBuild(buildOptions, runMode, params);
            return runner.run(commandArgs);
        } catch (Exception e) {
            return output.handleCommandException(e, "Unable to build image: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "Build {imageOptions='" + imageOptions + "'}";
    }
}
