package io.quarkus.cli;

import io.quarkus.cli.registry.BaseRegistryCommand;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.catalog.Platform;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformStream;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistryConfig;
import picocli.CommandLine;

@CommandLine.Command(name = "list", header = "List enabled Quarkus registries", description = "%n"
        + "This command will list currently enabled Quarkus extension registries.")
public class RegistryListCommand extends BaseRegistryCommand {

    @CommandLine.Option(names = {
            "--streams" }, description = "List currently recommended platform streams", defaultValue = "false")
    boolean streams;

    @Override
    public Integer call() throws Exception {

        registryClient.refreshRegistryCache(output);
        final RegistriesConfig config = registryClient.resolveConfig();

        final ExtensionCatalogResolver catalogResolver = streams ? registryClient.getExtensionCatalogResolver(output)
                : null;

        if (streams) {
            output.info("Available Quarkus platform streams per registry:");
        } else {
            output.info("Configured Quarkus extension registries:");
        }
        for (RegistryConfig r : config.getRegistries()) {
            if (r.isEnabled()) {
                output.info(r.getId());
                if (catalogResolver != null) {
                    final PlatformCatalog platformCatalog = catalogResolver
                            .resolvePlatformCatalogFromRegistry(r.getId());
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

        output.info("(Config source: " + config.getSource().describe() + ")");

        return CommandLine.ExitCode.OK;
    }
}
