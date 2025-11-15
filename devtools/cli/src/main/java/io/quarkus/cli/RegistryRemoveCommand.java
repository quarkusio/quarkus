package io.quarkus.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import io.quarkus.cli.registry.BaseRegistryCommand;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistryConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "remove", aliases = "rm", header = "Remove a Quarkus extension registry", description = "%n"
        + "This command will remove a Quarkus extension registry from the registry client configuration.")
public class RegistryRemoveCommand extends BaseRegistryCommand {

    @CommandLine.Parameters(arity = "1..*", split = ",", paramLabel = "REGISTRY-ID[,REGISTRY-ID]", description = "Registry ID to remove from the registry client configuration%n"
            + "  Example:%n"
            + "    registry.quarkus.io%n"
            + "    registry.quarkus.acme.com,registry.quarkus.io%n")
    List<String> registryIds;

    @Override
    public Integer call() throws Exception {
        boolean existingConfig = false;
        Path configYaml = null;

        // If a configuration was specified, check if it exists
        if (registryClient.getConfigArg() != null) {
            configYaml = Paths.get(registryClient.getConfigArg());
            existingConfig = Files.exists(configYaml);
        }

        final RegistriesConfig.Mutable config;
        if (configYaml != null && !existingConfig) {
            // we're creating a new configuration for a new file
            config = RegistriesConfig.builder();
        } else {
            config = registryClient.resolveConfig().mutable();
        }
        registryClient.refreshRegistryCache(output);

        boolean persist = false;
        for (String registryId : registryIds) {
            persist |= config.removeRegistry(registryId);
        }

        if (persist) {
            output.printText("Configured registries:");
            for (RegistryConfig rc : config.getRegistries()) {
                output.printText("- " + rc.getId());
            }
            if (configYaml != null) {
                config.persist(configYaml);
            } else {
                config.persist();
            }
        }
        return CommandLine.ExitCode.OK;
    }
}
