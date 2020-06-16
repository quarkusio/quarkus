package io.quarkus.cli.commands;

import java.io.File;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.io.Resource;

import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;

@CommandDefinition(name = "build", description = "Compiles the targeted project")
public class CompileProjectCommand implements Command<CommandInvocation> {
    @Option(name = "clean", hasValue = false, shortName = 'c', description = "Clean the project before compiling")
    private boolean clean;

    @Argument(description = "Path to the project, if not set it will use the current working directory")
    private Resource path;

    @Override
    public CommandResult execute(CommandInvocation invocation) throws CommandException, InterruptedException {

        File projectPath = path != null ? new File(path.getAbsolutePath()) : new File(System.getProperty("user.dir"));

        BuildTool buildTool = QuarkusProject.resolveExistingProjectBuildTool(projectPath.toPath());

        if (buildTool.getBuildFiles() != null && buildTool.getBuildFiles().length > 0) {
            File buildFile = new File(buildTool.getBuildFiles()[0]);

            if (!buildFile.isFile()) {
                invocation.println("Was not able to find a build file in: " + projectPath);
                return CommandResult.FAILURE;
            }

            try {
                if (buildTool.equals(BuildTool.MAVEN)) {
                    File wrapper = ExecuteUtil.getMavenWrapper(projectPath.getAbsolutePath());
                    if (wrapper != null) {
                        ExecuteUtil.executeWrapper(invocation, wrapper, "package");
                    } else {
                        ExecuteUtil.executeMaven(projectPath, invocation, "package");
                    }

                }
                //do gradle
                else {
                    File wrapper = ExecuteUtil.getGradleWrapper(projectPath.getAbsolutePath());
                    if (wrapper != null) {
                        ExecuteUtil.executeWrapper(invocation, wrapper, "build");
                    } else {
                        ExecuteUtil.executeGradle(projectPath, invocation, "build");
                    }
                }
            } catch (InterruptedException i) {
                invocation.println("Build was interrupted.");
                return CommandResult.FAILURE;
            }

            return CommandResult.SUCCESS;
        } else {
            invocation.println("Was not able to find a build file in: " + projectPath);
            return CommandResult.FAILURE;
        }
    }

}
