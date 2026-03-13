package io.quarkus.it.aesh;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

@CommandDefinition(name = "cdi-greet", description = "CDI greeting")
public class CdiGreetCommand implements Command<CommandInvocation> {

    @Inject
    GreetingService service;

    @Option(shortName = 'n', name = "name", description = "Name to greet", defaultValue = "World")
    private String name;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println(service.greet(name));
        return CommandResult.SUCCESS;
    }
}
