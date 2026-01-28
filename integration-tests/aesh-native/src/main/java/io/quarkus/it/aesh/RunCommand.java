package io.quarkus.it.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.ParentCommand;

@CommandDefinition(name = "run", description = "Run a task")
public class RunCommand implements Command<CommandInvocation> {

    @ParentCommand
    private TopAeshCommand parent;

    @Argument(description = "Task name to run")
    private String taskName;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        if (parent != null && parent.isVerbose()) {
            invocation.println("[VERBOSE] Running task...");
        }
        invocation.println("Running task: " + (taskName != null ? taskName : "default"));
        return CommandResult.SUCCESS;
    }
}
