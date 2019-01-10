package org.jboss.shamrock.cli.commands;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@CommandDefinition(name ="add-extension", description = "Adds extensions to a project")
public class AddExtensionCommand implements Command<CommandInvocation>{

    @Option(shortName = 'h', hasValue = false)
    private boolean help;

    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if(help) {
            commandInvocation.println(commandInvocation.getHelpInfo("protean add-extension"));
            return CommandResult.SUCCESS;
        }

        commandInvocation.println("here we'll add extensions");

        return CommandResult.SUCCESS;
    }
}
