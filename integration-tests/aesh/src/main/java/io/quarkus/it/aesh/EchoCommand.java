package io.quarkus.it.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

/**
 * A simple echo command for testing argument quoting.
 * Prints the value of the --text option as-is.
 */
@CommandDefinition(name = "echo", description = "Echo the text argument")
public class EchoCommand implements Command<CommandInvocation> {

    @Option(name = "text", shortName = 't', required = true)
    String text;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println(text);
        return CommandResult.SUCCESS;
    }
}
