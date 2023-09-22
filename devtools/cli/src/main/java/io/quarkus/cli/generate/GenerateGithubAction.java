package io.quarkus.cli.generate;

import picocli.CommandLine;

@CommandLine.Command(name = "github-action", sortOptions = false, mixinStandardHelpOptions = false, header = "Generate Github Action.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "%nOptions:%n")
public class GenerateGithubAction extends AbstractGenerateCodestart {

    @Override
    public String getCodestart() {
        return "github-action";
    }
}
