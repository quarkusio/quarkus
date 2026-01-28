package io.quarkus.it.aesh;

import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.OptionList;

@CommandDefinition(name = "list-options", description = "List option test")
public class ListOptionsCommand implements Command<CommandInvocation> {

    @OptionList(shortName = 'i', name = "items")
    private List<String> items;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("items: " + items);
        return CommandResult.SUCCESS;
    }
}
