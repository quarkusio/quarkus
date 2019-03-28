package io.quarkus.cli.commands;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;
import org.aesh.selector.MultiSelect;

import io.quarkus.dependencies.Extension;
import io.quarkus.devtools.commands.AddExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;

@CommandDefinition(name = "add", description = "Install extensions to a project")
public class AddExtensionsCommand implements Command<CommandInvocation> {

    @Option(shortName = 'p', description = "Path to the project, if not set it will use the current working directory")
    private Resource path;

    @Arguments(completer = ExtensionCompleter.class, description = "Name of the extension that will be added to the project")
    private Set<String> extensions;

    @Override
    public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {

        try {
            Path projectDirectory = path != null ? Paths.get(path.getAbsolutePath())
                    : Paths.get(System.getProperty("user.dir"));

            QuarkusProject quarkusProject = QuarkusProject.resolveExistingProject(projectDirectory,
                    QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor());

            AddExtensions project = new AddExtensions(quarkusProject);
            //if extensions is not set, create a selector
            if (extensions == null || extensions.isEmpty()) {
                MultiSelect selector = new MultiSelect(invocation.getShell(),
                        getAllExtensions(quarkusProject).stream().map(Extension::getSimplifiedArtifactId)
                                .collect(Collectors.toList()),
                        "Select the extensions that will be added to your project");

                extensions = new HashSet<>(selector.doSelect());
            }
            project.extensions(extensions);

            QuarkusCommandOutcome result = project.execute();
            if (result.isSuccess()) {
                invocation.println("Added " + extensions + " to the project.");
            } else {
                invocation.println("Unable to add an extension matching " + extensions);
            }
        } catch (QuarkusCommandException | IOException e) {
            invocation.println("Unable to add an extension matching " + extensions + ": " + e.getMessage());
        } catch (IllegalStateException e) {
            invocation.println("No build file in " + path + " found. Will not attempt to add any extensions.");
        }

        return CommandResult.SUCCESS;
    }

    private List<Extension> getAllExtensions(QuarkusProject quarkusProject) throws IOException {
        return quarkusProject.getPlatformDescriptor().getExtensions();
    }

}
