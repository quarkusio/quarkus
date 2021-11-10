package io.quarkus.cli;

import java.util.List;

import io.quarkus.cli.registry.BaseRegistryCommand;
import io.quarkus.registry.config.MutableRegistriesConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "add", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Add a Quarkus extension registry", description = "%n"
        + "This command will add a Quarkus extension registry to the registry client configuration unless it's already present.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class RegistryAddCommand extends BaseRegistryCommand {

    @CommandLine.Parameters(arity = "0..1", paramLabel = "REGISTRY-ID[,REGISTRY-ID]", description = "Registry ID to add to the registry client configuration%n"
            + "  Example:%n"
            + "    registry.quarkus.io%n"
            + "    registry.quarkus.acme.com,registry.quarkus.io%n")
    List<String> registryIds;

    @Override
    public Integer call() throws Exception {
        boolean persist = false;
        final MutableRegistriesConfig config = registryClient.getExtensionCatalogResolver().getConfig().mutable();

        for (String registryId : registryIds) {
            if (config.addRegistry(registryId)) {
                persist = true;
                output.info("Registry " + registryId + " was added");
            } else {
                output.info("Registry " + registryId + " was skipped since it is already present");
            }
        }

        if (persist) {
            config.persist();
        }
        return CommandLine.ExitCode.OK;
    }
}
