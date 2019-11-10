package io.quarkus.cli.commands;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;

import io.quarkus.cli.commands.legacy.LegacyQuarkusCommandInvocation;
import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.dependencies.Extension;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@CommandDefinition(name = "add-extension", description = "Adds extensions to a project")
public class AddExtensionCommand implements Command<CommandInvocation> {

    @Option(shortName = 'h', hasValue = false, overrideRequired = true)
    private boolean help;

    @Option(shortName = 'e', required = true, description = "Name of the extension that will be added to the project")
    private String extension;

    @Argument(required = true, description = "Path to the project pom the extension will be added")
    private Resource pom;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("quarkus add-extension"));
            return CommandResult.SUCCESS;
        } else {
            final QuarkusCommandInvocation quarkusInvocation = new LegacyQuarkusCommandInvocation();
            if (!findExtension(extension, quarkusInvocation.getPlatformDescriptor().getExtensions())) {
                commandInvocation.println("Can not find any extension named: " + extension);
                return CommandResult.SUCCESS;
            }
            if (pom.isLeaf()) {
                try {
                    quarkusInvocation.setValue(AddExtensions.EXTENSIONS, Collections.singleton(extension));
                    final File pomFile = new File(pom.getAbsolutePath());
                    AddExtensions project = new AddExtensions(new FileProjectWriter(pomFile.getParentFile()));
                    QuarkusCommandOutcome result = project.execute(quarkusInvocation);
                    if (!result.isSuccess()) {
                        throw new CommandException("Unable to add an extension matching " + extension);
                    }
                } catch (Exception e) {
                    throw new CommandException("Unable to add an extension matching " + extension, e);
                }
            }

        }

        return CommandResult.SUCCESS;
    }

    private boolean findExtension(String name, List<Extension> extensions) {
        for (Extension ext : extensions) {
            if (ext.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
