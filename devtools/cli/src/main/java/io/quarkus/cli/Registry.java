package io.quarkus.cli;

import java.util.List;

import io.quarkus.cli.registry.BaseRegistryCommand;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.Unmatched;

@CommandLine.Command(name = "registry", sortOptions = false, mixinStandardHelpOptions = false, header = "Manage extension registries.", subcommands = {
        RegistryAddCommand.class,
        RegistryListCommand.class,
        RegistryRemoveCommand.class }, headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "%nOptions:%n")
public class Registry extends BaseRegistryCommand {

    @Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    @Override
    public Integer call() throws Exception {
        final ParseResult result = spec.commandLine().getParseResult();
        final CommandLine appCommand = spec.subcommands().get("list");
        return appCommand.execute(result.originalArgs().stream().filter(x -> !"registry".equals(x)).toArray(String[]::new));
    }
}
