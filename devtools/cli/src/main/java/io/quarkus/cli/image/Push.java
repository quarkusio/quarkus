package io.quarkus.cli.image;

import java.util.Map;
import java.util.Optional;

import io.quarkus.cli.build.BuildSystemRunner;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

@CommandLine.Command(name = "push", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Push a container image.", description = "%n"
        + "This command will build and push a container image for the project.", footer = "%n", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class Push extends BaseImageCommand {

    protected static final String QUARKUS_CONTAINER_IMAGE_EXTENSION = "io.quarkus:quarkus-container-image";
    private static final Map<BuildTool, String> ACTION_MAPPING = Map.of(BuildTool.MAVEN, "quarkus:image-push",
            BuildTool.GRADLE, "imagePush");

    private static final String QUARKUS_CONTAINER_IMAGE_BUILD = "quarkus.container-image.build";
    private static final String QUARKUS_CONTAINER_IMAGE_USERNAME = "quarkus.container-image.username";
    private static final String QUARKUS_CONTAINER_IMAGE_PASSWORD = "quarkus.container-image.password";

    @CommandLine.Option(order = 8, names = {
            "--registry-username" }, description = "The image registry username.")
    public Optional<String> registryUsername = Optional.empty();

    @CommandLine.Option(order = 9, names = {
            "--registry-password" }, description = "The image registry password.")
    public Optional<String> registryPassword = Optional.empty();

    @CommandLine.Option(order = 10, names = {
            "--registry-password-stdin" }, description = "Read the image registry password from stdin.")
    public boolean registryPasswordStdin;

    @CommandLine.Option(order = 11, names = {
            "--also-build" }, description = "(Re)build the image before pushing.")
    public boolean alsoBuild;

    @Override
    public void populateImageConfiguration(Map<String, String> properties) {
        super.populateImageConfiguration(properties);
        registryUsername.ifPresent(u -> properties.put(QUARKUS_CONTAINER_IMAGE_USERNAME, u));

        if (registryPasswordStdin && !runMode.isDryRun()) {
            String password = new String(System.console().readPassword("Registry password:"));
            properties.put(QUARKUS_CONTAINER_IMAGE_PASSWORD, password);
        } else {
            registryPassword.ifPresent(p -> properties.put(QUARKUS_CONTAINER_IMAGE_PASSWORD, p));
        }

        if (alsoBuild) {
            properties.put(QUARKUS_CONTAINER_IMAGE_BUILD, "true");
        } else {
            properties.put(QUARKUS_CONTAINER_IMAGE_BUILD, "false");
        }

        properties.put(QUARKUS_FORCED_EXTENSIONS, QUARKUS_CONTAINER_IMAGE_EXTENSION);
    }

    @Override
    public Optional<String> getAction() {
        return Optional.ofNullable(ACTION_MAPPING.get(getRunner().getBuildTool()));
    }

    @Override
    public Integer call() throws Exception {
        try {
            populateImageConfiguration(propertiesOptions.properties);
            BuildSystemRunner runner = getRunner();
            String action = getAction().orElseThrow(
                    () -> new IllegalStateException("Unknown image push action for " + runner.getBuildTool().name()));
            BuildSystemRunner.BuildCommandArgs commandArgs = runner.prepareAction(action, buildOptions, runMode, params);
            if (runMode.isDryRun()) {
                System.out.println("Dry run option detected. Target command:");
                System.out.println(commandArgs.showCommand());
                return ExitCode.OK;
            }
            return runner.run(commandArgs);
        } catch (Exception e) {
            return output.handleCommandException(e, "Unable to push image: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "Push {imageOptions='" + imageOptions + "'}";
    }
}
