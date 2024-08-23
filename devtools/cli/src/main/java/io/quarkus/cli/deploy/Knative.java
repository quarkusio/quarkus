package io.quarkus.cli.deploy;

import io.quarkus.cli.BuildToolContext;
import picocli.CommandLine;

@CommandLine.Command(name = "knative", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Perform the deploy action on Knative.", description = "%n"
        + "The command will deploy the application on Knative.", footer = "%n"
                + "For example (using default values), it will create a Deployment named '<project.artifactId>' using the image with REPOSITORY='${user.name}/<project.artifactId>' and TAG='<project.version>' and will deploy it to the target cluster.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class Knative extends BaseKubernetesDeployCommand {

    private static final String KNATIVE = "knative";
    private static final String KNATIVE_EXTENSION = "io.quarkus:quarkus-kubernetes";
    private static final String CONTAINER_IMAGE_EXTENSION = "io.quarkus:quarkus-container-image";

    @Override
    public void populateContext(BuildToolContext context) {
        super.populateContext(context);
        context.getPropertiesOptions().properties.put(String.format(QUARKUS_DEPLOY_FORMAT, KNATIVE), "true");
        context.getForcedExtensions().add(KNATIVE_EXTENSION);
        context.getForcedExtensions().add(CONTAINER_IMAGE_EXTENSION);
    }
}
