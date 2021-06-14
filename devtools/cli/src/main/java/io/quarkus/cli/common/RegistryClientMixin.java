package io.quarkus.cli.common;

import io.quarkus.cli.Version;
import io.quarkus.cli.create.TargetQuarkusVersionGroup;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.catalog.ExtensionCatalog;
import picocli.CommandLine;

public class RegistryClientMixin {

    @CommandLine.Option(names = { "--registry-client" }, description = "Use the Quarkus extension catalog", negatable = true)
    boolean enableRegistryClient = false;

    public boolean enabled() {
        return enableRegistryClient;
    }

    public ExtensionCatalog getExtensionCatalog(TargetQuarkusVersionGroup targetVersion, OutputOptionMixin log) {
        log.debug("Resolving Quarkus extension catalog for " + targetVersion);
        QuarkusProjectHelper.setMessageWriter(log);

        if (targetVersion.isPlatformSpecified()) {
            ArtifactCoords coords = targetVersion.getPlatformBom();
            return ToolsUtils.resolvePlatformDescriptorDirectly(coords.getGroupId(), coords.getArtifactId(),
                    coords.getVersion(), QuarkusProjectHelper.artifactResolver(), log);
        }

        ExtensionCatalogResolver catalogResolver = QuarkusProjectHelper.getCatalogResolver(enableRegistryClient, log);

        try {
            if (!catalogResolver.hasRegistries()) {
                log.debug("Falling back to direct resolution of the platform bom");
                // Fall back to previous methods of finding registries (e.g. client has been disabled)
                return ToolsUtils.resolvePlatformDescriptorDirectly(null, null, Version.clientVersion(),
                        QuarkusProjectHelper.artifactResolver(), log);
            }

            if (targetVersion.isStream()) {
                final String stream = targetVersion.getStream();
                final int colon = stream.indexOf(':');
                final String platformKey = colon <= 0 ? null : stream.substring(0, colon);
                final String streamId = colon < 0 ? stream : stream.substring(colon + 1);
                return catalogResolver.resolveExtensionCatalog(platformKey, streamId);
            }

            return catalogResolver.resolveExtensionCatalog();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve the Quarkus extension catalog", e);
        }
    }

    @Override
    public String toString() {
        return "RegistryClientMixin [enableRegistryClient=" + enableRegistryClient + "]";
    }
}
