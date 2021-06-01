package io.quarkus.cli;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.cli.create.BaseCreateCommand;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.Unmatched;

@CommandLine.Command(name = "create", sortOptions = false, mixinStandardHelpOptions = false, description = "Create a new project.", subcommands = {
        CreateApp.class, CreateCli.class /* , CreateExtension.class */ })
public class Create extends BaseCreateCommand {

    @Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    @Override
    public Integer call() throws Exception {
        output.info("No subcommand specified, creating an app (see --help).");

        ParseResult result = spec.commandLine().getParseResult();
        List<String> args = result.originalArgs().stream().filter(x -> !"create".equals(x)).collect(Collectors.toList());
        CommandLine appCommand = spec.subcommands().get("app");
        return appCommand.execute(args.toArray(new String[0]));
    }
}
