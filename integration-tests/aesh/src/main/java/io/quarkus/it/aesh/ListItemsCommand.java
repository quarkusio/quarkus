package io.quarkus.it.aesh;

import java.util.List;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;

@CommandDefinition(name = "list-items", description = "List all items from the database")
public class ListItemsCommand implements Command<CommandInvocation> {

    @Inject
    ItemService itemService;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        List<Item> items = itemService.listAll();
        if (items.isEmpty()) {
            invocation.println("No items found");
        } else {
            invocation.println("Items (" + items.size() + "):");
            for (Item item : items) {
                invocation.println("  - " + item.getName());
            }
        }
        return CommandResult.SUCCESS;
    }
}
