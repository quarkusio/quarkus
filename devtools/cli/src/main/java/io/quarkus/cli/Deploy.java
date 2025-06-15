package io.quarkus.cli;

import java.util.Map;

import io.quarkus.cli.deploy.Kind;
import io.quarkus.cli.deploy.Knative;
import io.quarkus.cli.deploy.Kubernetes;
import io.quarkus.cli.deploy.Minikube;
import io.quarkus.cli.deploy.Openshift;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;

@CommandLine.Command(name = "deploy", sortOptions = false, mixinStandardHelpOptions = false, header = "Deploy application.", subcommands = {
        Kubernetes.class, Openshift.class, Knative.class, Kind.class,
        Minikube.class, }, headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "%nOptions:%n")
public class Deploy extends BuildToolDelegatingCommand {

    private static final Map<BuildTool, String> ACTION_MAPPING = Map.of(BuildTool.MAVEN, "quarkus:deploy",
            BuildTool.GRADLE, "deploy");

    @Override
    public Map<BuildTool, String> getActionMapping() {
        return ACTION_MAPPING;
    }

    @Override
    public String toString() {
        return "Deploy {}";
    }
}
