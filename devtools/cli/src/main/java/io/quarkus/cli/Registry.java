package io.quarkus.cli;

import java.util.List;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.Unmatched;

@CommandLine.Command(name = "registry", sortOptions = false, mixinStandardHelpOptions = false, header = "Configure Quarkus registry client", subcommands = {
        RegistryListCommand.class,
        RegistryAddCommand.class,
        RegistryRemoveCommand.class }, headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", optionListHeading = "%nOptions:%n")
public class Registry implements Callable<Integer> {

    @CommandLine.Mixin
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected HelpOption helpOption;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    public Integer call() throws Exception {
        final ParseResult result = spec.commandLine().getParseResult();
        final CommandLine appCommand = spec.subcommands().get("list");
        return appCommand.execute(result.originalArgs().stream().filter(x -> !"registry".equals(x)).toArray(String[]::new));
    }
}
