package io.quarkus.it.aesh;

import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;

@CommandDefinition(name = "multi-args", description = "Multiple args test")
public class MultiArgsCommand implements Command<CommandInvocation> {

    @Arguments
    private List<String> args;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("args: " + args);
        return CommandResult.SUCCESS;
    }
}
