package io.quarkus.cli.commands;

import java.io.File;
import java.io.IOException;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;

import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.generators.BuildTool;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@CommandDefinition(name = "list-extensions", description = "List extensions for a project")
public class ListExtensionsCommand implements Command<CommandInvocation> {

    @Option(shortName = 'h', hasValue = false)
    private boolean help;

    @Option(shortName = 'a', hasValue = false, description = "Display all extensions or just the installable.")
    private boolean all = false;

    @Option(shortName = 'f', hasValue = true, description = "Select the output format among 'name' (display the name only), 'concise' (display name and description) and 'full' (concise format and version related columns).")
    private String format = "concise";

    @Option(shortName = 's', hasValue = true, description = "Search filter on extension list. The format is based on Java Pattern.")
    private String searchPattern;

    @Option(shortName = 'p', description = "path to the project")
    private Resource path;

    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("quarkus list-extensions"));
        } else {
            try {
                BuildTool builtTool = BuildTool.MAVEN;
                FileProjectWriter writer = null;
                if (path != null) {
                    File projectDirectory = new File(path.getAbsolutePath());
                    if (new File(projectDirectory, "build.gradle").exists()
                            || new File(projectDirectory, "build.gradle.kts").exists()) {
                        builtTool = BuildTool.GRADLE;
                    }
                    writer = new FileProjectWriter(projectDirectory);
                }
                new ListExtensions(writer, builtTool).listExtensions(all, format, searchPattern);
            } catch (IOException e) {
                throw new CommandException("Unable to list extensions", e);
            }
        }
        return CommandResult.SUCCESS;
    }

}
