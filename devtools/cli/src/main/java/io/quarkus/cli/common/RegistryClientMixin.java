package io.quarkus.cli.common;

import java.nio.file.Path;

import io.quarkus.cli.Version;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.catalog.ExtensionCatalog;
import picocli.CommandLine;

public class RegistryClientMixin {
    final static boolean VALIDATE = !Boolean.parseBoolean(System.getenv("REGISTRY_CLIENT_TEST"));

    @CommandLine.Option(names = { "--refresh" }, description = "Refresh the local Quarkus extension registry cache")
    boolean refresh;

    public boolean enabled() {
        return true;
    }

    public QuarkusProject createQuarkusProject(Path projectRoot, TargetQuarkusVersionGroup targetVersion, BuildTool buildTool,
            OutputOptionMixin log) {
        ExtensionCatalog catalog = getExtensionCatalog(targetVersion, log);
        if (VALIDATE && catalog.getQuarkusCoreVersion().startsWith("1.")) {
            throw new UnsupportedOperationException("The version 2 CLI can not be used with Quarkus 1.x projects.\n"
                    + "Use the maven/gradle plugins when working with Quarkus 1.x projects.");
        }
        return QuarkusProjectHelper.getProject(projectRoot, catalog, buildTool, log);
    }

    ExtensionCatalog getExtensionCatalog(TargetQuarkusVersionGroup targetVersion, OutputOptionMixin log) {
        log.debug("Resolving Quarkus extension catalog for " + targetVersion);
        QuarkusProjectHelper.setMessageWriter(log);

        if (VALIDATE && targetVersion.isStreamSpecified() && !enabled()) {
            throw new UnsupportedOperationException(
                    "Specifying a stream (--stream) requires the registry client to resolve resources. " +
                            "Please try again with the registry client enabled (--registry-client)");
        }

        if (targetVersion.isPlatformSpecified()) {
            ArtifactCoords coords = targetVersion.getPlatformBom();
            return ToolsUtils.resolvePlatformDescriptorDirectly(coords.getGroupId(), coords.getArtifactId(),
                    coords.getVersion(), QuarkusProjectHelper.artifactResolver(), log);
        }

        final ExtensionCatalogResolver catalogResolver = getExtensionCatalogResolver(log);

        try {
            if (!catalogResolver.hasRegistries()) {
                log.debug("Falling back to direct resolution of the platform bom");
                // Fall back to previous methods of finding registries (e.g. client has been disabled)
                return ToolsUtils.resolvePlatformDescriptorDirectly(null, null, Version.clientVersion(),
                        QuarkusProjectHelper.artifactResolver(), log);
            }

            if (targetVersion.isStreamSpecified()) {
                return catalogResolver.resolveExtensionCatalog(targetVersion.getStream());
            }

            refreshRegistryCache(log);
            return catalogResolver.resolveExtensionCatalog();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve the Quarkus extension catalog", e);
        }
    }

    private ExtensionCatalogResolver getExtensionCatalogResolver(OutputOptionMixin log) {
        return QuarkusProjectHelper.getCatalogResolver(enabled(), log);
    }

    public void refreshRegistryCache(OutputOptionMixin log) {
        if (!refresh) {
            return;
        }
        final ExtensionCatalogResolver catalogResolver = getExtensionCatalogResolver(log);
        if (!catalogResolver.hasRegistries()) {
            log.warn("Skipping refresh since no registries are configured");
            return;
        }
        log.debug("Refreshing registry cache");
        try {
            catalogResolver.clearRegistryCache();
        } catch (Exception e) {
            log.warn("Unable to refresh the registry cache: %s", e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "RegistryClientMixin [useRegistryClient=" + enabled() + "]";
    }
}
