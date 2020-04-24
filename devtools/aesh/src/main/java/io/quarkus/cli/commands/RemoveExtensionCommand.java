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

import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.dependencies.Extension;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;

@CommandDefinition(name = "remove-extension", description = "Remove extension from a project")
public class RemoveExtensionCommand implements Command<CommandInvocation> {

    @Option(shortName = 'h', hasValue = false, overrideRequired = true)
    private boolean help;

    @Option(shortName = 'e', required = true, description = "Name of the extension that will be removed from the project")
    private String extension;

    @Argument(required = true, description = "Path to the project pom the extension will be removed from")
    private Resource pom;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("quarkus remove-extension"));
            return CommandResult.SUCCESS;
        } else {

            final QuarkusPlatformDescriptor platformDescr = QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor();
            if (!findExtension(extension, platformDescr.getExtensions())) {
                commandInvocation.println("Can not find any extension named: " + extension);
                return CommandResult.SUCCESS;
            }
            if (pom.isLeaf()) {
                try {
                    final File pomFile = new File(pom.getAbsolutePath());
                    RemoveExtensions project = new RemoveExtensions(new FileProjectWriter(pomFile.getParentFile()),
                            platformDescr)
                                    .extensions(Collections.singleton(extension));
                    QuarkusCommandOutcome result = project.execute();
                    if (!result.isSuccess()) {
                        throw new CommandException("Unable to remove an extension matching " + extension);
                    }
                } catch (Exception e) {
                    throw new CommandException("Unable to remove an extension matching " + extension, e);
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
