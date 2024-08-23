package io.quarkus.devservices.deployment;

import static io.quarkus.devservices.deployment.DevServicesProcessor.printDevService;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;

import io.quarkus.deployment.dev.devservices.DevServiceDescriptionBuildItem;

@CommandDefinition(name = "list", description = "List of dev services")
public class DevServicesListCommand implements Command {

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) {
        commandInvocation.println("");
        StringBuilder builder = new StringBuilder();
        for (DevServiceDescriptionBuildItem serviceDescription : DevServicesCommand.serviceDescriptions) {
            printDevService(builder, serviceDescription, false);
            builder.append("\n");
        }
        commandInvocation.print(builder.toString());
        return CommandResult.SUCCESS;
    }
}
