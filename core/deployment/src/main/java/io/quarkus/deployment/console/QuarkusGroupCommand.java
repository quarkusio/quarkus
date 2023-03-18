package io.quarkus.deployment.console;

import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommand;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

public abstract class QuarkusGroupCommand implements GroupCommand {

    @Option(shortName = 'h', hasValue = false, overrideRequired = true)
    public boolean help;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        commandInvocation.getShell().write(commandInvocation.getHelpInfo());
        return CommandResult.SUCCESS;
    }

}
