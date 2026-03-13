package io.quarkus.it.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.ParentCommand;

@CommandDefinition(name = "version", description = "Show version")
public class CliVersionCommand implements Command<CommandInvocation> {

    @ParentCommand
    private CliCommand parent;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        if (parent != null && parent.isVerbose()) {
            invocation.println("[VERBOSE] Fetching version...");
        }
        invocation.println("Version: 1.0.0");
        return CommandResult.SUCCESS;
    }
}
