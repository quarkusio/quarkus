package io.quarkus.cli;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.cli.registry.BaseRegistryCommand;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistryConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "remove", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Remove a Quarkus extension registry", description = "%n"
        + "This command will remove a Quarkus extension registry from the registry client configuration.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class RegistryRemoveCommand extends BaseRegistryCommand {

    @CommandLine.Parameters(arity = "0..1", paramLabel = "REGISTRY-ID[,REGISTRY-ID]", description = "Registry ID to remove from the registry client configuration%n"
            + "  Example:%n"
            + "    registry.quarkus.io%n"
            + "    registry.quarkus.acme.com,registry.quarkus.io%n")
    String registryIds;

    @Override
    public Integer call() throws Exception {
        final RegistriesConfig config = registryClient.getConfig();

        final List<RegistryConfig> registries = config.getRegistries();
        final Map<String, RegistryConfig> registryMap = new LinkedHashMap<>(registries.size());
        registries.forEach(r -> registryMap.put(r.getId(), r));

        boolean persist = false;
        for (String registryId : registryIds.split(",")) {
            if (registryMap.remove(registryId) == null) {
                output.info("Registry " + registryId + " was not previously configured");
            } else {
                output.info("Registry " + registryId + " was removed");
                persist = true;
            }
        }

        if (persist) {
            registries.clear();
            registries.addAll(registryMap.values());
            registryClient.saveConfig();
        }

        return CommandLine.ExitCode.OK;
    }
}
