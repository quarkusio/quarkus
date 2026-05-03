package io.quarkus.cli;

import java.util.List;
import java.util.concurrent.Callable;

import io.quarkus.cli.common.HelpOption;
import io.quarkus.cli.common.OutputOptionMixin;
import picocli.CommandLine;
import picocli.CommandLine.ParseResult;
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
        // If the user passed a non-flag argument that isn't a known subcommand,
        // they likely mistyped a subcommand name (e.g. "create ext" instead of "create extension").
        // Give a clear error instead of falling through to CreateApp which would fail with
        // a cryptic picocli UnmatchedArgumentException.
        if (unmatchedArgs != null && !unmatchedArgs.isEmpty()) {
            String first = unmatchedArgs.get(0);
            if (!first.startsWith("-")) {
                output.error("Unknown subcommand '%s' for 'quarkus create'.", first);
                output.info("Available subcommands are: %s", String.join(", ", spec.subcommands().keySet()));
                output.info("See 'quarkus create --help' for more information.");
                return CommandLine.ExitCode.USAGE;
            }
        }

        output.info("Creating an app (default project type, see --help).");

        ParseResult result = spec.commandLine().getParseResult();
        CommandLine appCommand = spec.subcommands().get("app");
        return appCommand.execute(result.originalArgs().stream().filter(x -> !"create".equals(x)).toArray(String[]::new));
    }
}
