package io.quarkus.it.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

@GroupCommandDefinition(name = "cli", description = "CLI-like parent command", groupCommands = { CliRunCommand.class,
        CliVersionCommand.class })
public class CliCommand implements Command<CommandInvocation> {

    @Option(shortName = 'v', name = "verbose", description = "Enable verbose output", hasValue = false)
    private boolean verbose;

    public boolean isVerbose() {
        return verbose;
    }

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("CLI - use a subcommand (run, version)");
        return CommandResult.SUCCESS;
    }
}
