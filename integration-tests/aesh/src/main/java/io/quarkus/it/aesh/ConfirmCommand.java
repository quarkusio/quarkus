package io.quarkus.it.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

/**
 * A command that prompts for confirmation before proceeding.
 * Used to test interactive input via AeshLauncher.
 */
@CommandDefinition(name = "confirm", description = "Confirm an action")
public class ConfirmCommand implements Command<CommandInvocation> {

    @Option(name = "action", shortName = 'a', defaultValue = "proceed")
    String action;

    @Override
    public CommandResult execute(CommandInvocation invocation) throws InterruptedException {
        invocation.println("Are you sure you want to " + action + "? (y/n)");
        String answer = invocation.inputLine();
        if ("y".equals(answer.trim())) {
            invocation.println("Confirmed: " + action);
            return CommandResult.SUCCESS;
        }
        invocation.println("Cancelled: " + action);
        return CommandResult.FAILURE;
    }
}
