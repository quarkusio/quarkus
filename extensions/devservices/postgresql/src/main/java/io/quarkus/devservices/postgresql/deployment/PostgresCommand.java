package io.quarkus.devservices.postgresql.deployment;

import java.util.List;

import io.quarkus.deployment.console.QuarkusGroupCommand;
import org.aesh.command.Command;
import org.aesh.command.GroupCommandDefinition;

import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;

@GroupCommandDefinition(name = "postgres", description = "Postgresql Commands")
public class PostgresCommand extends QuarkusGroupCommand {

    private final DevServicesLauncherConfigResultBuildItem devServicesLauncherConfigResultBuildItem;

    public PostgresCommand(DevServicesLauncherConfigResultBuildItem devServicesLauncherConfigResultBuildItem) {
        this.devServicesLauncherConfigResultBuildItem = devServicesLauncherConfigResultBuildItem;
    }

    @Override
    public List<Command> getCommands() {
        return List.of(new PsqlCommand(devServicesLauncherConfigResultBuildItem));
    }

}
