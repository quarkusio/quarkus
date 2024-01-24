package io.quarkus.cli.generate;

import java.util.Map;

import io.quarkus.cli.BuildToolDelegatingCommand;
import io.quarkus.devtools.project.BuildTool;
import picocli.CommandLine;

@CommandLine.Command(name = "generate", sortOptions = false, mixinStandardHelpOptions = false, header = "Generate.", subcommands = {
        GenerateDockerfiles.class, GenerateGithubAction.class
}, headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "%nOptions:%n")
public class Generate extends BuildToolDelegatingCommand {

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
