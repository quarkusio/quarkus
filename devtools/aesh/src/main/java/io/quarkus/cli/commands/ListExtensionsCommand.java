package io.quarkus.cli.commands;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;
import org.aesh.selector.SelectorType;

import io.quarkus.devtools.commands.ListExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;

@CommandDefinition(name = "list", generateHelp = true, description = "List extensions for a project")
public class ListExtensionsCommand implements Command<CommandInvocation> {

    @Option(shortName = 'a', hasValue = false, description = "Display all or just the installable extensions.")
    private boolean all = false;

    @Option(shortName = 'f', selector = SelectorType.SELECT, completer = FormatCompleter.class, converter = FormatConverter.class, description = "Select the output format among:\n"
            +
            "'name' - display the name only\n" +
            "'concise' - (display name and description\n" +
            "'full' - (concise format and version related columns.\n")
    private ExtensionFormat format;

    @Option(shortName = 's', hasValue = true, defaultValue = {
            "*" }, description = "Search filter on extension list. The format is based on Java Pattern.")
    private String searchPattern;

    @Option(shortName = 'p', description = "Path to the project, if not set it will use the current working directory")
    private Resource path;

    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        try {
            Path projectDirectory = path != null ? Paths.get(path.getAbsolutePath())
                    : Paths.get(System.getProperty("user.dir"));

            new ListExtensions(QuarkusProject.resolveExistingProject(projectDirectory,
                    QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor()))
                            .all(all)
                            .format(format.formatValue())
                            .search(searchPattern)
                            .execute();
        } catch (QuarkusCommandException e) {
            e.printStackTrace();
        }
        return CommandResult.SUCCESS;
    }

}
