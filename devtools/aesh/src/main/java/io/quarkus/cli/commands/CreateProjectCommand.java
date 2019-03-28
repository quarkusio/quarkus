package io.quarkus.cli.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.OptionList;
import org.aesh.command.shell.Shell;
import org.aesh.io.Resource;
import org.aesh.readline.action.KeyAction;
import org.aesh.readline.terminal.Key;
import org.aesh.selector.MultiSelect;
import org.aesh.selector.SelectorType;
import org.aesh.terminal.utils.Config;
import org.apache.commons.lang3.StringUtils;

import io.quarkus.dependencies.Extension;
import io.quarkus.devtools.commands.AddExtensions;
import io.quarkus.devtools.commands.CreateProject;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;

@CommandDefinition(name = "create", generateHelp = true, description = "Creates a base Quarkus project")
public class CreateProjectCommand implements Command<CommandInvocation> {

    @Option(name = "groupid", shortName = 'g', defaultValue = "org.acme.quarkus.sample", askIfNotSet = true, description = "The groupId of the project")
    private String groupId;

    @Option(name = "artifactid", shortName = 'a', defaultValue = "my-quarkus-project", askIfNotSet = true, description = "The artifactId of the project (also often the name of the project")
    private String artifactId;

    @Option(shortName = 'v', defaultValue = "1.0.0-SNAPSHOT", askIfNotSet = true, description = "Project version number")
    private String version;

    @Option(shortName = 'c', name = "classname", description = "Rest resource name, by default set to: groupId+artifactId+HelloResource")
    private String className;

    @Option(shortName = 'p', name = "resourcepath", description = "Rest resource path, by default set to /hello")
    private String resourcePath;

    @Option(shortName = 'b', selector = SelectorType.SELECT, completer = ProjectTypeCompleter.class, converter = BuildToolConverter.class, description = "Build tool type for the project")
    private BuildTool buildTool;

    @OptionList(shortName = 'e', completer = ExtensionCompleter.class, description = "Extensions that will be added to the build file")
    private Set<String> extensions;

    @Argument(description = "Path to the new project, if not set it will use the current working directory")
    private Resource path;

    public CommandResult execute(CommandInvocation invocation) {
        try {
            Path projectDirectory = path != null ? Paths.get(path.getAbsolutePath())
                    : Paths.get(System.getProperty("user.dir"));

            if (className == null) {
                invocation.print("Do you want to create a REST resource? (y/n) ");
                KeyAction input = invocation.input();
                invocation.print(Config.getLineSeparator());
                if (input == Key.y)
                    processClassName(invocation.getShell());
            }

            Files.createDirectories(projectDirectory);

            final Map<String, Object> context = new HashMap<>();
            context.put("path", resourcePath);
            boolean status = new CreateProject(projectDirectory,
                    QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor())
                            .groupId(groupId)
                            .artifactId(artifactId)
                            .version(version)
                            .buildTool(buildTool)
                            .className(className)
                            .doCreateProject(context);

            if ((extensions == null || extensions.isEmpty()) && status) {
                //ask if the user want to add any extensions
                try {
                    invocation.print("Do you want to add any extensions in addition to rest? (y/n) ");
                    if (invocation.input() == Key.y) {
                        QuarkusProject quarkusProject = QuarkusProject.resolveExistingProject(projectDirectory,
                                QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor());

                        MultiSelect selector = new MultiSelect(invocation.getShell(),
                                getAllExtensions(quarkusProject).stream().map(Extension::getSimplifiedArtifactId)
                                        .collect(Collectors.toList()),
                                "Select the extensions that will be added to your project");

                        extensions = new HashSet<>(selector.doSelect());

                        status = new AddExtensions(QuarkusProject.resolveExistingProject(projectDirectory,
                                QuarkusPlatformConfig.getGlobalDefault().getPlatformDescriptor()))
                                        .extensions(extensions).execute()
                                        .isSuccess();
                    }
                    invocation.print(Config.getLineSeparator());
                } catch (InterruptedException ie) {
                    //ignored
                }
            }

            if (status) {
                invocation.println("Project " + artifactId +
                        " created successfully at " + projectDirectory.toString() + ".");
            } else {
                invocation.println("Failed to create project");
            }
        } catch (InterruptedException e) {
            invocation.println("Project creation was aborted, " + e.getMessage());
            return CommandResult.FAILURE;
        } catch (IOException | QuarkusCommandException e) {
            invocation.println("Project creation failed, " + e.getMessage());
            invocation.println("Is the given path an empty folder?");
            return CommandResult.FAILURE;
        }

        return CommandResult.SUCCESS;
    }

    private void processClassName(Shell shell) throws InterruptedException {
        String defaultResourceName = groupId.replace("-", ".").replace("_", ".");
        className = shell.readLine("Set the resource class name, ( HelloResource ): ");
        if (className == null || className.length() == 0)
            className = defaultResourceName + ".HelloResource";
        else {
            className = defaultResourceName + "." + className;
        }

        if (resourcePath == null || resourcePath.length() == 0) {
            resourcePath = shell.readLine("Set the resource path (" + getDerivedPath(className) + "): ");
            if (resourcePath == null || resourcePath.length() == 0)
                resourcePath = getDerivedPath(className);
            if (!resourcePath.startsWith("/"))
                resourcePath = "/" + resourcePath;
        }

    }

    private static String getDerivedPath(String className) {
        String[] resourceClassName = StringUtils.splitByCharacterTypeCamelCase(
                className.substring(className.lastIndexOf(".") + 1));
        return "/" + resourceClassName[0].toLowerCase();
    }

    private List<Extension> getAllExtensions(QuarkusProject quarkusProject) throws IOException {
        return quarkusProject.getPlatformDescriptor().getExtensions();
    }
}
