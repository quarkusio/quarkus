package io.quarkus.cli;

import io.quarkus.cli.registry.BaseRegistryCommand;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformStream;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistryConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "list", sortOptions = false, showDefaultValues = true, mixinStandardHelpOptions = false, header = "List enabled Quarkus registries", description = "%n"
        + "This command will list currently enabled Quarkus extension registries.", headerHeading = "%n", commandListHeading = "%nCommands:%n", synopsisHeading = "%nUsage: ", parameterListHeading = "%n", optionListHeading = "Options:%n")
public class RegistryListCommand extends BaseRegistryCommand {

    @CommandLine.Option(names = {
            "--streams" }, description = "List currently recommended platform streams", defaultValue = "false")
    boolean showStreams;

    @Override
    public Integer call() throws Exception {
        final RegistriesConfig config = registryClient.getExtensionCatalogResolver().getConfig();

        ExtensionCatalogResolver catalogResolver = null;
        if (showStreams) {
            output.info("Available Quarkus platform streams per registry:");
            catalogResolver = registryClient.getExtensionCatalogResolver();
        } else {
            output.info("Configured Quarkus extension registries:");
        }
        for (RegistryConfig r : config.getRegistries()) {
            if (r.isEnabled()) {
                output.info(r.getId());
                if (showStreams) {
                    final PlatformCatalog platformCatalog = catalogResolver.resolvePlatformCatalogFromRegistry(r.getId());
                    if (platformCatalog != null) {
                        for (Platform p : platformCatalog.getPlatforms()) {
                            for (PlatformStream s : p.getStreams()) {
                                output.info("  " + p.getPlatformKey() + ":" + s.getId());
                            }
                        }
                    }
                }
            } else {
                output.info(r.getId() + " (disabled)");
            }
        }

        output.info("(Read from " + config.getSource().describe() + ")");

        return CommandLine.ExitCode.OK;
    }
}
