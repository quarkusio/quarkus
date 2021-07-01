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

    @CommandLine.Option(names = { "--registry-client" }, description = "Use the Quarkus extension catalog", negatable = true)
    boolean enableRegistryClient = false;

    public boolean enabled() {
        return enableRegistryClient;
    }

    public QuarkusProject createQuarkusProject(Path projectRoot, TargetQuarkusVersionGroup targetVersion, BuildTool buildTool,
            OutputOptionMixin log) {
        ExtensionCatalog catalog = getExtensionCatalog(targetVersion, log);
        if (catalog.getQuarkusCoreVersion().startsWith("1.")) {
            throw new UnsupportedOperationException("The version 2 CLI can not be used with Quarkus 1.x projects.\n"
                    + "Use the maven/gradle plugins when working with Quarkus 1.x projects.");
        }
        return QuarkusProjectHelper.getProject(projectRoot, catalog, buildTool, log);
    }

    ExtensionCatalog getExtensionCatalog(TargetQuarkusVersionGroup targetVersion, OutputOptionMixin log) {
        log.debug("Resolving Quarkus extension catalog for " + targetVersion);
        QuarkusProjectHelper.setMessageWriter(log);

        if (targetVersion.isStreamSpecified() && !enableRegistryClient) {
            throw new UnsupportedOperationException(
                    "Specifying a stream (--stream) requires the registry client to resolve resources. " +
                            "Please try again with the registry client enabled (--registry-client)");
        }

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

            if (targetVersion.isStreamSpecified()) {
                return catalogResolver.resolveExtensionCatalog(targetVersion.getStream());
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
