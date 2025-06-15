package io.quarkus.cli.image;

import java.util.Map;
import java.util.Optional;

import io.quarkus.cli.BuildToolContext;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;

@CommandLine.Command(name = "push", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Push a container image.", description = "%n"
        + "This command will build and push a container image for the project.", footer = "%n", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class Push extends BaseImageCommand {

    protected static final String QUARKUS_CONTAINER_IMAGE_EXTENSION = "io.quarkus:quarkus-container-image";
    private static final Map<BuildTool, String> ACTION_MAPPING = Map.of(BuildTool.MAVEN, "quarkus:image-push",
            BuildTool.GRADLE, "imagePush");

    private static final String QUARKUS_CONTAINER_IMAGE_BUILD = "quarkus.container-image.build";
    private static final String QUARKUS_CONTAINER_IMAGE_USERNAME = "quarkus.container-image.username";
    private static final String QUARKUS_CONTAINER_IMAGE_PASSWORD = "quarkus.container-image.password";

    @CommandLine.Option(order = 8, names = { "--registry-username" }, description = "The image registry username.")
    public Optional<String> registryUsername = Optional.empty();

    @CommandLine.Option(order = 9, names = { "--registry-password" }, description = "The image registry password.")
    public Optional<String> registryPassword = Optional.empty();

    @CommandLine.Option(order = 10, names = {
            "--registry-password-stdin" }, description = "Read the image registry password from stdin.")
    public boolean registryPasswordStdin;

    @CommandLine.Option(order = 11, names = { "--also-build" }, description = "(Re)build the image before pushing.")
    public boolean alsoBuild;

    @Override
    public void populateContext(BuildToolContext context) {
        super.populateContext(context);
        Map<String, String> properties = context.getPropertiesOptions().properties;
        registryUsername.ifPresent(u -> properties.put(QUARKUS_CONTAINER_IMAGE_USERNAME, u));

        if (registryPasswordStdin && !context.getRunModeOption().isDryRun()) {
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
        context.getForcedExtensions().add(QUARKUS_CONTAINER_IMAGE_EXTENSION);
    }

    @Override
    public Map<BuildTool, String> getActionMapping() {
        return ACTION_MAPPING;
    }

    @Override
    public String toString() {
        return "Push {imageOptions='" + imageOptions + "'}";
    }
}
