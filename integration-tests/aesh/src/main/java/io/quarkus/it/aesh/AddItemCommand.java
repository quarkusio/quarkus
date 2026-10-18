package io.quarkus.it.aesh;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;

@CommandDefinition(name = "add-item", description = "Add an item to the database")
public class AddItemCommand implements Command<CommandInvocation> {

    @Inject
    ItemService itemService;

    @Argument(description = "Item name", required = true)
    String name;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        itemService.add(name);
        invocation.println("Added: " + name);
        return CommandResult.SUCCESS;
    }
}
