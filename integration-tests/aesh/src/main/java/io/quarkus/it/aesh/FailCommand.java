package io.quarkus.it.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

/**
 * A command that always fails with an exception.
 * Used to test error capture in the AeshLauncher test framework.
 */
@CommandDefinition(name = "fail", description = "Always fails with an error")
public class FailCommand implements Command<CommandInvocation> {

    @Option(name = "message", shortName = 'm', defaultValue = "intentional failure")
    String message;

    @Override
    public CommandResult execute(CommandInvocation invocation) throws CommandException {
        throw new CommandException(message);
    }
}
