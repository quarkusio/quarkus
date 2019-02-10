package org.jboss.shamrock.cli.commands;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.jboss.shamrock.dependencies.Extension;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@CommandDefinition(name = "list-extensions", description = "List extensions for a project")
public class ListExtensionsCommand implements Command <CommandInvocation>{

    @Option(shortName = 'h', hasValue = false)
    private boolean help;

    @Option(shortName = 'n', hasValue = false, description = "Only display the extension names")
    private boolean name;

    @Option(shortName = 'a', hasValue = false, description = "Display name, group-id, artifact-id and version (default behaviour)")
    private boolean all;

    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if(help) {
            commandInvocation.println(commandInvocation.getHelpInfo("protean list-extensions"));
        }
        else if(name) {
            for(Extension ext : AddExtensions.get()) {
                commandInvocation.println(ext.getName());
            }

        }
        else {
            for(Extension ext : AddExtensions.get()) {
                commandInvocation.println(
                        ext.getName()+ " ("+ext.getGroupId()+":"+ext.getArtifactId()+":"+ext.getVersion()+")");
            }
        }
        return CommandResult.SUCCESS;
    }

}
