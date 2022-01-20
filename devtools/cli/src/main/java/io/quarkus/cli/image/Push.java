package io.quarkus.cli.image;

import java.util.Map;
import java.util.concurrent.Callable;

import io.quarkus.cli.build.BuildSystemRunner;
import picocli.CommandLine;

@CommandLine.Command(name = "push", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Push a container image.", description = "%n"
        + "This command will build and push a container image for the project.", footer = "%n", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class Push extends BaseImageCommand implements Callable<Integer> {

    private static final String QUARKUS_CONTAINER_IMAGE_PUSH = "quarkus.container-image.push";

    @Override
    public void populateImageConfiguration(Map<String, String> properties) {
        super.populateImageConfiguration(properties);
        properties.put(QUARKUS_CONTAINER_IMAGE_PUSH, "true");
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
        return "Push {imageOptions='" + imageOptions + "'}";
    }
}
