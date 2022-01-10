package io.quarkus.devservices.deployment;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.aesh.command.Command;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommand;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;

import io.quarkus.deployment.console.SetCompleter;
import io.quarkus.devservices.runtime.devmode.DevServiceDescription;

@GroupCommandDefinition(name = "devservices", description = "Dev Service Commands")
public class DevServicesCommand implements GroupCommand {
    static List<DevServiceDescription> serviceDescriptions;

    public DevServicesCommand(List<DevServiceDescription> serviceDescriptions) {
        DevServicesCommand.serviceDescriptions = serviceDescriptions;
    }

    @Override
    public List<Command> getCommands() {
        return List.of(new DevServicesListCommand(), new DevServicesLogsCommand());
    }

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) {
        commandInvocation.println(commandInvocation.getHelpInfo());
        return CommandResult.SUCCESS;
    }

    static Optional<DevServiceDescription> findDevService(String devServiceName) {
        return serviceDescriptions.stream()
                .filter(d -> d.getName().equals(devServiceName))
                .findFirst();
    }

    public static class DevServiceCompleter extends SetCompleter {

        @Override
        protected Set<String> allOptions(String soFar) {
            return serviceDescriptions.stream()
                    .map(DevServiceDescription::getName)
                    .collect(Collectors.toSet());
        }
    }
}
