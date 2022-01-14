package io.quarkus.deployment.console;

import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

import io.quarkus.dev.console.QuarkusConsole;

public abstract class QuarkusCommand implements Command {

    @Option(shortName = 'd', hasValue = false)
    public boolean done;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        var result = doExecute(commandInvocation);
        if (done) {
            QuarkusConsole.INSTANCE.exitCliMode();
        }
        return result;
    }

    public abstract CommandResult doExecute(CommandInvocation commandInvocation) throws CommandException, InterruptedException;
}
