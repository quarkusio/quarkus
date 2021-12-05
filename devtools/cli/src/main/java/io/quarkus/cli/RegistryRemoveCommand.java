package io.quarkus.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import io.quarkus.cli.registry.BaseRegistryCommand;
import io.quarkus.registry.config.RegistriesConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "remove", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "Remove a Quarkus extension registry", description = "%n"
        + "This command will remove a Quarkus extension registry from the registry client configuration.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
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
        if (existingConfig) {
            registryClient.refreshRegistryCache(output);
            config = registryClient.resolveConfig().mutable();
            if (config.getSource().getFilePath() == null) {
                output.error("Can only modify file-based configuration. Config source is " + config.getSource().describe());
                return CommandLine.ExitCode.SOFTWARE;
            }
        } else {
            output.error("Can only remove registries from an existing configuration. The specified config file does not exist: "
                    + configYaml);
            return CommandLine.ExitCode.SOFTWARE;
        }

        boolean persist = false;
        for (String registryId : registryIds) {
            persist |= config.removeRegistry(registryId);
        }

        if (persist) {
            config.persist();
        }
        return CommandLine.ExitCode.OK;
    }
}
