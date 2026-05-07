package io.quarkus.it.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

@CommandDefinition(name = "hello", description = "Greets someone")
public class HelloCommand implements Command<CommandInvocation> {

    @Option(shortName = 'n', name = "name", description = "Name to greet", defaultValue = "World")
    private String name;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Hello " + name + "!");
        return CommandResult.SUCCESS;
    }
}
