package io.quarkus.devservices.postgresql.deployment;

import java.util.List;

import org.aesh.command.Command;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommand;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;

import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;

@GroupCommandDefinition(name = "postgres", description = "Postgresql Commands")
public class PostgresCommand implements GroupCommand {

    private final DevServicesLauncherConfigResultBuildItem devServicesLauncherConfigResultBuildItem;

    public PostgresCommand(DevServicesLauncherConfigResultBuildItem devServicesLauncherConfigResultBuildItem) {
        this.devServicesLauncherConfigResultBuildItem = devServicesLauncherConfigResultBuildItem;
    }

    @Override
    public List<Command> getCommands() {
        return List.of(new PsqlCommand(devServicesLauncherConfigResultBuildItem));
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        return null;
    }
}
