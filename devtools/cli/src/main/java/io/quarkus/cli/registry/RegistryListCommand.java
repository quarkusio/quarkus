package io.quarkus.cli.registry;

import java.nio.file.Path;

import io.quarkus.cli.common.RegistryClientMixin;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigLocator;
import io.quarkus.registry.config.RegistryConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "list", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "List enabled Quarkus registries", description = "%n"
        + "This command will list currently enabled Quarkus extension registries", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class RegistryListCommand extends BaseRegistryCommand {

    @CommandLine.Mixin
    protected RegistryClientMixin registryClient;

    @Override
    public Integer call() throws Exception {

        registryClient.refreshRegistryCache(output);

        output.info("Available Quarkus extension registries:");
        final RegistriesConfig config = RegistriesConfigLocator.resolveConfig();
        for (RegistryConfig r : config.getRegistries()) {
            if (r.isDisabled()) {
                continue;
            }
            output.info("- " + r.getId());
        }

        final Path configYaml = RegistriesConfigLocator.locateConfigYaml();
        if (configYaml != null) {
            output.info("(Read from " + configYaml + ")");
        }

        return CommandLine.ExitCode.OK;
    }
}
