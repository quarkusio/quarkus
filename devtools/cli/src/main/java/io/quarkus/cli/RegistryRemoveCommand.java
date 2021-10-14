package io.quarkus.cli;

import java.util.List;

import io.quarkus.cli.registry.BaseRegistryCommand;
import io.quarkus.registry.config.MutableRegistriesConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "remove", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Remove a Quarkus extension registry", description = "%n"
        + "This command will remove a Quarkus extension registry from the registry client configuration.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class RegistryRemoveCommand extends BaseRegistryCommand {

    @CommandLine.Parameters(arity = "0..1", paramLabel = "REGISTRY-ID[,REGISTRY-ID]", description = "Registry ID to remove from the registry client configuration%n"
            + "  Example:%n"
            + "    registry.quarkus.io%n"
            + "    registry.quarkus.acme.com,registry.quarkus.io%n")
    List<String> registryIds;

    @Override
    public Integer call() throws Exception {
        boolean persist = false;
        final MutableRegistriesConfig config = registryClient.getExtensionCatalogResolver().getConfig().mutable();

        for (String registryId : registryIds) {
            if (config.removeRegistry(registryId)) {
                persist = true;
                output.info("Registry " + registryId + " was removed");
            } else {
                output.info("Registry " + registryId + " was not removed; it was not configured.");
            }
        }

        if (persist) {
            config.persist();
        }

        return CommandLine.ExitCode.OK;
    }
}
