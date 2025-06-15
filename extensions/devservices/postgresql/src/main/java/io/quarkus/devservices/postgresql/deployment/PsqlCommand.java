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

    private static final String JDBC_DATASOURCE__URL_KEY = "quarkus.datasource.jdbc.url";
    private static final String REACTIVE_DATASOURCE__URL_KEY = "quarkus.datasource.reactive.url";
    private final DevServicesLauncherConfigResultBuildItem devServicesLauncherConfigResultBuildItem;

    public PsqlCommand(DevServicesLauncherConfigResultBuildItem devServicesLauncherConfigResultBuildItem) {
        this.devServicesLauncherConfigResultBuildItem = devServicesLauncherConfigResultBuildItem;
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
        // todo: should this work for non-dev services stuff as well? What about non-default datasources
        try {
            URI uri;
            if (devServicesLauncherConfigResultBuildItem.getConfig().containsKey(JDBC_DATASOURCE__URL_KEY)) {
                uri = new URI(devServicesLauncherConfigResultBuildItem.getConfig().get(JDBC_DATASOURCE__URL_KEY)
                        .substring("jdbc:".length()));
            } else if (devServicesLauncherConfigResultBuildItem.getConfig().containsKey(REACTIVE_DATASOURCE__URL_KEY)) {
                uri = new URI(devServicesLauncherConfigResultBuildItem.getConfig().get(REACTIVE_DATASOURCE__URL_KEY)
                        .substring("vertx-reactive:".length()));
            } else {
                throw new RuntimeException("Unable to determine datasource URL");
            }

            int port = uri.getPort();
            String host = uri.getHost();
            String pw = devServicesLauncherConfigResultBuildItem.getConfig().get("quarkus.datasource.password");
            commandInvocation.println("PGPASSWORD=" + pw + " psql --host=" + host + " --port=" + port + " --username="
                    + devServicesLauncherConfigResultBuildItem.getConfig().get("quarkus.datasource.username") + " "
                    + uri.getPath().substring(1));
            return CommandResult.SUCCESS;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
