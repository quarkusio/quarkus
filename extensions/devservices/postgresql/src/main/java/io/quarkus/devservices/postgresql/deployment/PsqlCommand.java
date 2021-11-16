package io.quarkus.devservices.postgresql.deployment;

import java.net.URI;
import java.net.URISyntaxException;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;

import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;

@CommandDefinition(name = "print-command", description = "Outputs the psql command to connect to the database")
public class PsqlCommand implements Command {

    private final DevServicesLauncherConfigResultBuildItem devServicesLauncherConfigResultBuildItem;

    public PsqlCommand(DevServicesLauncherConfigResultBuildItem devServicesLauncherConfigResultBuildItem) {
        this.devServicesLauncherConfigResultBuildItem = devServicesLauncherConfigResultBuildItem;
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        //todo: should this work for non-dev services stuff as well? What about non-default datasources
        try {
            URI url = new URI(
                    devServicesLauncherConfigResultBuildItem.getConfig().get("quarkus.datasource.jdbc.url").substring(5));
            int port = url.getPort();
            String host = url.getHost();
            String pw = devServicesLauncherConfigResultBuildItem.getConfig().get("quarkus.datasource.password");
            commandInvocation.println("PGPASSWORD=" + pw + " psql --host=" + host + " --port=" + port + " --username="
                    + devServicesLauncherConfigResultBuildItem.getConfig().get("quarkus.datasource.username") + " "
                    + url.getPath().substring(1));
            return CommandResult.SUCCESS;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
