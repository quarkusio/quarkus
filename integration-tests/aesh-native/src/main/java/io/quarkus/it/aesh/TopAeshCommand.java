package io.quarkus.it.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

import io.quarkus.aesh.runtime.annotations.TopCommand;

@GroupCommandDefinition(name = "app", description = "Test application", groupCommands = { HelloCommand.class, RunCommand.class,
        VersionCommand.class, FailCommand.class, ExplodingCommand.class, CdiGreetCommand.class, ListOptionsCommand.class,
        MultiArgsCommand.class })
@TopCommand
public class TopAeshCommand implements Command<CommandInvocation> {

    @Option(shortName = 'v', name = "verbose", description = "Enable verbose output", hasValue = false)
    private boolean verbose;

    public boolean isVerbose() {
        return verbose;
    }

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Use a subcommand: hello, run, version");
        return CommandResult.SUCCESS;
    }
}
