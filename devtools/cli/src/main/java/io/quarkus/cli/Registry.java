package io.quarkus.cli;

import java.util.List;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.quickcli.CommandLine;
import io.quarkus.quickcli.CommandSpec;
import io.quarkus.quickcli.ParseResult;
import io.quarkus.quickcli.annotations.Command;
import io.quarkus.quickcli.annotations.Mixin;
import io.quarkus.quickcli.annotations.Spec;
import io.quarkus.quickcli.annotations.Unmatched;

@Command(name = "registry", header = "Configure Quarkus registry client", subcommands = {
        RegistryListCommand.class,
        RegistryAddCommand.class,
        RegistryRemoveCommand.class })
public class Registry implements Callable<Integer> {

    @Mixin(name = "output")
    protected OutputOptionMixin output;

    @Mixin
    protected HelpOption helpOption;

    @Spec
    protected CommandSpec spec;

    @Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    public Integer call() throws Exception {
        final ParseResult result = spec.commandLine().getParseResult();
        final CommandLine appCommand = spec.commandLine().getSubcommands().get("list");
        return appCommand.execute(result.originalArgs().stream().filter(x -> !"registry".equals(x)).toArray(String[]::new));
    }
}
