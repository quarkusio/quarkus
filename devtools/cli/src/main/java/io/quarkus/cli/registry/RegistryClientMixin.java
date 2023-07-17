package io.quarkus.cli.registry;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import io.quarkus.cli.Version;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.TargetQuarkusPlatformGroup;
import io.quarkus.devtools.commands.CreateProjectHelper;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.config.RegistriesConfig;
import picocli.CommandLine;

public class RegistryClientMixin {
    static final boolean VALIDATE = !Boolean.parseBoolean(System.getenv("REGISTRY_CLIENT_TEST"));

    /** @see io.quarkus.cli.registry.ToggleRegistryClientMixin#setRegistryClient */
    public final String getRegistryClientProperty() {
        return "-DquarkusRegistryClient=" + enabled();
    }

    @CommandLine.Option(names = {
            "--refresh" }, description = "Refresh the local Quarkus extension registry cache", defaultValue = "false")
    boolean refresh = false;

    @CommandLine.Option(paramLabel = "CONFIG", names = { "--config" }, description = "Configuration file")
    String config;

    public boolean enabled() {
        return true;
    }

    public String getConfigArg() {
        return config;
    }

    public RegistriesConfig resolveConfig() throws RegistryResolutionException {
        return config == null
                ? RegistriesConfig.resolveConfig()
                : RegistriesConfig.resolveFromFile(Path.of(config));
    }

    public QuarkusProject createQuarkusProject(Path projectRoot, TargetQuarkusPlatformGroup targetVersion, BuildTool buildTool,
            OutputOptionMixin log) throws RegistryResolutionException {
        return createQuarkusProject(projectRoot, targetVersion, buildTool, log, List.of());
    }

    public QuarkusProject createQuarkusProject(Path projectRoot, TargetQuarkusPlatformGroup targetVersion, BuildTool buildTool,
            OutputOptionMixin log, Collection<String> extensions) throws RegistryResolutionException {
        ExtensionCatalog catalog = getExtensionCatalog(targetVersion, log);
        if (VALIDATE && catalog.getQuarkusCoreVersion().startsWith("1.")) {
            throw new UnsupportedOperationException("The version 2 CLI can not be used with Quarkus 1.x projects.\n"
                    + "Use the maven/gradle plugins when working with Quarkus 1.x projects.");
        }
        catalog = CreateProjectHelper.completeCatalog(catalog, extensions, QuarkusProjectHelper.artifactResolver());
        return QuarkusProjectHelper.getProject(projectRoot, catalog, buildTool, log);
    }

    ExtensionCatalog getExtensionCatalog(TargetQuarkusPlatformGroup targetVersion, OutputOptionMixin log)
            throws RegistryResolutionException {
        log.debug("Resolving Quarkus extension catalog for " + targetVersion);
        QuarkusProjectHelper.setMessageWriter(log);
        if (enabled()) {
            QuarkusProjectHelper.setToolsConfig(resolveConfig());
        }

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

        final ExtensionCatalogResolver catalogResolver;
        try {
            catalogResolver = getExtensionCatalogResolver(log);
        } catch (RegistryResolutionException e) {
            log.warn(
                    "Configured Quarkus extension registries appear to be unavailable at the moment. "
                            + "It should still be possible to create a project by providing the groupId:artifactId:version of the desired Quarkus platform BOM, "
                            + "e.g. 'quarkus create -P "
                            + ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID + ":"
                            + ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID + ":" + Version.clientVersion() + "'");
            throw e;
        }

        if (!catalogResolver.hasRegistries()) {
            log.debug("Falling back to direct resolution of the platform bom");
            // Fall back to previous methods of finding registries (e.g. client has been disabled)
            return ToolsUtils.resolvePlatformDescriptorDirectly(null, null, Version.clientVersion(),
                    QuarkusProjectHelper.artifactResolver(), log);
        }

        refreshRegistryCache(log);

        if (targetVersion.isStreamSpecified()) {
            return catalogResolver.resolveExtensionCatalog(targetVersion.getStream());
        }
        return catalogResolver.resolveExtensionCatalog();
    }

    public ExtensionCatalogResolver getExtensionCatalogResolver(OutputOptionMixin log) throws RegistryResolutionException {
        return QuarkusProjectHelper.getCatalogResolver(enabled(), log);
    }

    public void refreshRegistryCache(OutputOptionMixin log) throws RegistryResolutionException {
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
