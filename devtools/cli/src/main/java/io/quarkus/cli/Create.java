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

@Command(name = "create", header = "Create a new project.", subcommands = {
        CreateApp.class,
        CreateCli.class,
        CreateExtension.class })
public class Create implements Callable<Integer> {

    @Mixin(name = "output")
    protected OutputOptionMixin output;

    @Mixin
    protected HelpOption helpOption;

    @Spec
    protected CommandSpec spec;

    @Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    public Integer call() throws Exception {
        output.info("Creating an app (default project type, see --help).");

        ParseResult result = spec.commandLine().getParseResult();
        CommandLine appCommand = spec.commandLine().getSubcommands().get("app");
        return appCommand.execute(result.originalArgs().stream().filter(x -> !"create".equals(x)).toArray(String[]::new));
    }
}
