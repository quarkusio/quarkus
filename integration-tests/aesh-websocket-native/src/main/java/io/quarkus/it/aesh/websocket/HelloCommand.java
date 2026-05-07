package io.quarkus.it.aesh.websocket;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

import io.quarkus.aesh.runtime.annotations.CliCommand;

@CommandDefinition(name = "hello", description = "Say hello")
@CliCommand
public class HelloCommand implements Command<CommandInvocation> {

    @Option(shortName = 'n', name = "name", defaultValue = "World")
    private String name;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Hello " + name + "!");
        return CommandResult.SUCCESS;
    }
}
