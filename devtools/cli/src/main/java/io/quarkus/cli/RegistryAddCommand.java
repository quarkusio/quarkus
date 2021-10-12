package io.quarkus.cli;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.cli.registry.BaseRegistryCommand;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.json.JsonRegistryConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "add", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Add a Quarkus extension registry", description = "%n"
        + "This command will add a Quarkus extension registry to the registry client configuration unless it's already present.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class RegistryAddCommand extends BaseRegistryCommand {

    @CommandLine.Parameters(arity = "0..1", paramLabel = "REGISTRY-ID[,REGISTRY-ID]", description = "Registry ID to add to the registry client configuration%n"
            + "  Example:%n"
            + "    registry.quarkus.io%n"
            + "    registry.quarkus.acme.com,registry.quarkus.io%n")
    String registryIds;

    @Override
    public Integer call() throws Exception {
        final RegistriesConfig config = registryClient.getConfig();

        List<RegistryConfig> registries = config.getRegistries();

        final Set<String> existingIds = config.getRegistries().stream().map(RegistryConfig::getId).collect(Collectors.toSet());
        boolean persist = false;
        for (String registryId : registryIds.split(",")) {
            if (existingIds.add(registryId)) {
                persist = true;
                final JsonRegistryConfig registry = new JsonRegistryConfig();
                registry.setId(registryId);
                registries.add(registry);
                output.info("Registry " + registryId + " was added");
            } else {
                output.info("Registry " + registryId + " was skipped since it is already present");
            }
        }

        if (persist) {
            registryClient.saveConfig();
        }

        return CommandLine.ExitCode.OK;
    }
}
