package io.quarkus.cli;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.cli.registry.BaseRegistryCommand;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigLocator;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.json.JsonRegistriesConfig;
import io.quarkus.registry.config.json.RegistriesConfigMapperHelper;
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

        registryClient.refreshRegistryCache(output);

        Path configYaml;
        if (registryClient.getConfigArg() == null) {
            configYaml = RegistriesConfigLocator.locateConfigYaml();
            if (configYaml == null) {
                output.error("Failed to locate the registry client configuration file");
                return CommandLine.ExitCode.SOFTWARE;
            }
        } else {
            configYaml = Paths.get(registryClient.getConfigArg());
        }

        final RegistriesConfig config = RegistriesConfigMapperHelper.deserialize(configYaml, JsonRegistriesConfig.class);

        final Map<String, RegistryConfig> registries = new LinkedHashMap<>(config.getRegistries().size());
        config.getRegistries().forEach(r -> registries.put(r.getId(), r));
        boolean persist = false;
        for (String registryId : registryIds.split(",")) {
            if (registries.remove(registryId) == null) {
                output.info("Registry " + registryId + " was not previously configured");
            } else {
                output.info("Registry " + registryId + " was removed");
                persist = true;
            }
        }

        if (persist) {
            final JsonRegistriesConfig jsonConfig = new JsonRegistriesConfig();
            jsonConfig.setRegistries(new ArrayList<>(registries.values()));
            RegistriesConfigMapperHelper.serialize(jsonConfig, configYaml);
        }

        return CommandLine.ExitCode.OK;
    }
}
