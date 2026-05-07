package org.acme;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

@CommandDefinition(name = "hello", description = "Greet someone")
public class HelloCommand implements Command<CommandInvocation> {

    @Option(shortName = 'n', name = "name", description = "Your name.",
            defaultValue = "aesh")
    String name;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Hello " + name + ", go go commando!");
        return CommandResult.SUCCESS;
    }

}
