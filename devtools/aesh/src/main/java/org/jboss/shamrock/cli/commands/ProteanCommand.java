package org.jboss.shamrock.cli.commands;

import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@GroupCommandDefinition(name = ProteanCommand.COMMAND_NAME, groupCommands = { ListExtensionsCommand.class,
        AddExtensionCommand.class,
        CreateProjectCommand.class }, description = "<command> [<args>] \n\nThese are the common protean commands used in various situations")
public class ProteanCommand implements Command<CommandInvocation> {
    public static final String COMMAND_NAME = "protean";

    @Option(shortName = 'h', hasValue = false)
    private boolean help;

    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help)
            commandInvocation.println(commandInvocation.getHelpInfo("protean"));

        return CommandResult.SUCCESS;
    }
}
