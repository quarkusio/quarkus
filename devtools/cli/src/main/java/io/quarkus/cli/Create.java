package io.quarkus.cli;

import java.util.List;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import picocli.CommandLine;
import picocli.CommandLine.Unmatched;

@CommandLine.Command(name = "create", header = "Create a new project.", subcommands = {
        CreateApp.class,
        CreateCli.class,
        CreateExtension.class })
public class Create implements Callable<Integer> {

    @CommandLine.Mixin(name = "output")
    protected OutputOptionMixin output;

    @CommandLine.Mixin
    protected HelpOption helpOption;

    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @Unmatched // avoids throwing errors for unmatched arguments
    List<String> unmatchedArgs;

    public Integer call() throws Exception {
        return DefaultSubcommand.forward(spec, output, "app", "Creating an app (default project type, see --help).");
    }
}
