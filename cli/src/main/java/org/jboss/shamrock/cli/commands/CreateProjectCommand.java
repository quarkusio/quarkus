package org.jboss.shamrock.cli.commands;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@CommandDefinition(name = "create-project", description = "Creates a base Shamrock maven project")
public class CreateProjectCommand implements Command<CommandInvocation>{

    @Option(shortName = 'h', hasValue = false)
    private boolean help;

    @Option(shortName = 'p', description = "path for the project")
    private Resource path;

    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if(help) {
            commandInvocation.println(commandInvocation.getHelpInfo("protean create-project"));
            return CommandResult.SUCCESS;
        }

        commandInvocation.println("here we'll create a project in: "+path);

        return CommandResult.SUCCESS;
    }
}
