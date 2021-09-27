package io.quarkus.cli;

import java.util.List;

import io.quarkus.cli.create.BaseCreateCommand;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.Unmatched;

@CommandLine.Command(name = "create", sortOptions = false, mixinStandardHelpOptions = false, header = "Create a new project.", subcommands = {
        CreateApp.class,
        CreateCli.class,
        CreateExtension.class }, headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "%nOptions:%n")
public class Create extends BaseCreateCommand {

    @Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    @Override
    public Integer call() throws Exception {
        output.info("Creating an app (default project type, see --help).");

        ParseResult result = spec.commandLine().getParseResult();
        CommandLine appCommand = spec.subcommands().get("app");
        return appCommand.execute(result.originalArgs().stream().filter(x -> !"create".equals(x)).toArray(String[]::new));
    }
}
