package io.quarkus.cli.deploy;

import java.util.Optional;

import io.quarkus.cli.BuildToolContext;
import picocli.CommandLine;

@CommandLine.Command(name = "minikube", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Perform the deploy action on minikube.", description = "%n"
        + "The command will deploy the application on minikube.", footer = "%n"
                + "For example (using default values), it will create a Deployment named '<project.artifactId>' using the image with REPOSITORY='${user.name}/<project.artifactId>' and TAG='<project.version>' and will deploy it to the target cluster.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class Minikube extends BaseKubernetesDeployCommand {

    private static final String MINIKUBE = "minikube";
    private static final String MINIKUBE_EXTENSION = "io.quarkus:quarkus-minikube";
    private static final String CONTAINER_IMAGE_EXTENSION = "io.quarkus:quarkus-container-image";
    private static final String DEPLOYMENT_KIND = "quarkus.kubernetes.deployment-kind";

    public enum DeploymentKind {
        Deployment,
        StatefulSet,
        Job
    }

    @CommandLine.Option(names = { "--deployment-kind" }, description = "The kind of resource to generate and deploy")
    public Optional<DeploymentKind> kind;

    @Override
    public void populateContext(BuildToolContext context) {
        super.populateContext(context);
        context.getPropertiesOptions().properties.put(String.format(QUARKUS_DEPLOY_FORMAT, MINIKUBE), "true");
        context.getForcedExtensions().add(MINIKUBE_EXTENSION);
        context.getForcedExtensions().add(CONTAINER_IMAGE_EXTENSION);
        kind.ifPresent(k -> {
            context.getPropertiesOptions().properties.put(DEPLOYMENT_KIND, k.name());
        });
    }
}
