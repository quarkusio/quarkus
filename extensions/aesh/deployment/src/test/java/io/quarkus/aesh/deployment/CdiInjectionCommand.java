package io.quarkus.aesh.deployment;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

@CommandDefinition(name = "greet", description = "Greet with CDI injection")
public class CdiInjectionCommand implements Command<CommandInvocation> {

    @Inject
    GreetingService greetingService;

    @Option(shortName = 'n', name = "name", description = "Name to greet", defaultValue = "World")
    private String name;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println(greetingService.greet(name));
        return CommandResult.SUCCESS;
    }
}
