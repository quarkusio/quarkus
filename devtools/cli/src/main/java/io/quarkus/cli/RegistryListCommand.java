package io.quarkus.cli;

import java.nio.file.Path;

import io.quarkus.cli.registry.BaseRegistryCommand;
import io.quarkus.cli.registry.RegistryClientMixin;
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
        final RegistriesConfig config = RegistriesConfigLocator.resolveConfig();

        output.info("Configured Quarkus extension registries:");
        for (RegistryConfig r : config.getRegistries()) {
            if (r.isEnabled()) {
                output.info("- " + r.getId());
            } else {
                output.info("- " + r.getId() + " (disabled)");
            }
        }

        final Path configYaml = RegistriesConfigLocator.locateConfigYaml();
        if (configYaml != null) {
            output.info("(Read from " + configYaml + ")");
        }

        return CommandLine.ExitCode.OK;
    }
}
