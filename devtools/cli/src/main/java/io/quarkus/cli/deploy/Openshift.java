package io.quarkus.cli.deploy;

import java.util.Optional;

import io.quarkus.cli.BuildToolContext;
import picocli.CommandLine;

@CommandLine.Command(name = "openshift", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Perform the deploy action on OpenShift.", description = "%n"
        + "The command will deploy the application on OpenShift.", footer = "%n"
                + "For example (using default values), it will create a Deployment named '<project.artifactId>' using the image with REPOSITORY='${user.name}/<project.artifactId>' and TAG='<project.version>' and will deploy it to the target cluster.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class Openshift extends BaseKubernetesDeployCommand {

    private static final String OPENSHIFT = "openshift";
    private static final String OPENSHIFT_EXTENSION = "io.quarkus:quarkus-openshift";
    private static final String DEPLOYMENT_KIND = "quarkus.openshift.deployment-kind";

    public enum DeploymentKind {
        Deployment,
        DeploymentConfig,
        StatefulSet,
        Job
    }

    @CommandLine.Option(names = { "--deployment-kind" }, description = "The kind of resource to generate and deploy")
    public Optional<DeploymentKind> kind;

    @Override
    public void populateContext(BuildToolContext context) {
        super.populateContext(context);
        context.getPropertiesOptions().properties.put(String.format(QUARKUS_DEPLOY_FORMAT, OPENSHIFT), "true");
        context.getForcedExtensions().add(OPENSHIFT_EXTENSION);
        kind.ifPresent(k -> {
            context.getPropertiesOptions().properties.put(DEPLOYMENT_KIND, k.name());
        });
    }

    @Override
    public String getDefaultImageBuilder() {
        return OPENSHIFT;
    }
}
