package io.quarkus.cli.commands;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;

import io.quarkus.cli.commands.writer.FileWriter;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
@CommandDefinition(name = "create-project", description = "Creates a base Quarkus maven project")
public class CreateProjectCommand implements Command<CommandInvocation> {

    @Option(shortName = 'h', hasValue = false)
    private boolean help;

    @Option(shortName = 'g', defaultValue = "org.acme")
    private String groupid;

    @Option(shortName = 'a', defaultValue = "quarkus")
    private String artifactid;

    @Option(shortName = 'v', defaultValue = "1.0.0-SNAPSHOT")
    private String version;

    @Option(shortName = 'p', description = "path for the project")
    private Resource path;

    public CommandResult execute(CommandInvocation commandInvocation) {
        if (help) {
            commandInvocation.println(commandInvocation.getHelpInfo("quarkus create-project"));
            return CommandResult.SUCCESS;
        }

        if (path != null) {
            try {
                boolean status = new CreateProject(new FileWriter(new File(path.getAbsolutePath())))
                        .groupId(groupid)
                        .artifactId(artifactid)
                        .version(this.version)
                        .doCreateProject(new HashMap<>());
                if (status) {
                    commandInvocation.println("Project " + artifactid + " created successfully.");
                } else {
                    commandInvocation.println("Failed to create project");
                }
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        } else {
            commandInvocation.println("You need to set the path for the project");
        }

        return CommandResult.SUCCESS;
    }
}
