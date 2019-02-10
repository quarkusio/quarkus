package org.jboss.shamrock.cli.commands;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;
import org.jboss.shamrock.dependencies.Extension;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@CommandDefinition(name ="add-extension", description = "Adds extensions to a project")
public class AddExtensionCommand implements Command<CommandInvocation>{

    @Option(shortName = 'h', hasValue = false, overrideRequired = true)
    private boolean help;

    @Option(shortName = 'e', required = true, description = "Name of the extension that will be added to the project")
    private String extension;

    @Argument(required = true, description = "Path to the project pom the extension will be added")
    private Resource pom;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if(help) {
            commandInvocation.println(commandInvocation.getHelpInfo("protean add-extension"));
            return CommandResult.SUCCESS;
        }
        else {
            if(!findExtension(extension)) {
                commandInvocation.println("Can not find any extension named: " + extension);
                return CommandResult.SUCCESS;
            }
            else if (pom.isLeaf()){
                try {
                    AddExtensions project = new AddExtensions(new File(pom.getAbsolutePath()));
                    project.addExtensions(Collections.singleton(extension));
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return CommandResult.SUCCESS;
    }

    private boolean findExtension(String name) {
        for(Extension ext : AddExtensions.get()) {
            if(ext.getName().equalsIgnoreCase(name))
                return true;
        }
        return false;
    }
}
