package io.quarkus.cli.registry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;

import io.quarkus.cli.Version;
import io.quarkus.cli.common.OutputOptionMixin;
import io.quarkus.cli.common.TargetQuarkusVersionGroup;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigLocator;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

public class RegistryClientMixin {
    final static boolean VALIDATE = !Boolean.parseBoolean(System.getenv("REGISTRY_CLIENT_TEST"));

    OutputOptionMixin outputOptionMixin;
    ExtensionCatalogResolver projectResolver;

    @CommandLine.Option(names = {
            "--refresh" }, description = "Refresh the local Quarkus extension registry cache", defaultValue = "false")
    boolean refresh = false;

    @CommandLine.Option(paramLabel = "CONFIG", names = { "--config" }, description = "Configuration file")
    String config;

    @CommandLine.Spec(CommandLine.Spec.Target.MIXEE)
    CommandSpec mixee;

    /**
     * @return either the path to the config file specified as an argument, or
     *         the value of a system property (for consistency with other tools), or null.
     */
    public String getToolsConfigSource() {
        return config == null
                ? System.getProperty(RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY)
                : config;
    }

    /**
     * Add properties for configuring the registry client to the command line.
     * 
     * @see #getToolsConfigSource()
     * @see #useRegistryClient()
     * @see io.quarkus.cli.registry.ToggleRegistryClientMixin#setRegistryClient
     */
    public final void addRegistryClientProperties(ArrayDeque<String> args) {
        String configFile = getToolsConfigSource();
        if (configFile != null) {
            args.add("-D" + RegistriesConfigLocator.CONFIG_FILE_PATH_PROPERTY + "=" + configFile);
        }
        args.add("-DquarkusRegistryClient=" + useRegistryClient());
    }

    /**
     * The registry client can be disabled for some commands.
     * 
     * @return true if the registry client should be used.
     * @see ToggleRegistryClientMixin#useRegistryClient()
     */
    public boolean useRegistryClient() {
        return true;
    }

    private MessageWriter log() {
        if (outputOptionMixin == null) {
            outputOptionMixin = OutputOptionMixin.getMixin(mixee);
        }
        return outputOptionMixin;
    }

    public ExtensionCatalogResolver resolver() {
        if (projectResolver == null) {
            try {
                projectResolver = ExtensionCatalogResolver.builder()
                        .messageWriter(log())
                        .useRegistryClient(useRegistryClient())
                        .configFile(getToolsConfigSource())
                        .refreshCache(refresh)
                        .build();
            } catch (RegistryResolutionException e) {
                log().warn("Unable to resolve extension catalog: " + e.getMessage());
                projectResolver = ExtensionCatalogResolver.builder()
                        .messageWriter(log())
                        .useRegistryClient(useRegistryClient())
                        .configFile(getToolsConfigSource())
                        .empty();
            }
        }
        return projectResolver;
    }

    public RegistriesConfig getConfig() {
        return resolver().getConfig();
    }

    public QuarkusProject createQuarkusProject(Path projectRoot, TargetQuarkusVersionGroup targetVersion, BuildTool buildTool)
            throws RegistryResolutionException {
        log().debug("Resolving Quarkus extension catalog for " + targetVersion);

        if (VALIDATE && targetVersion.isStreamSpecified() && !useRegistryClient()) {
            throw new UnsupportedOperationException(
                    "Specifying a stream (--stream) requires the registry client to resolve resources. " +
                            "Please try again with the registry client enabled (--registry-client)");
        }

        final ExtensionCatalog extensionCatalog;

        // the target version is either an explicit bom, or a stream.
        if (targetVersion.isPlatformSpecified()) {
            ArtifactCoords bom = targetVersion.getPlatformBom();
            extensionCatalog = resolver()
                    .resolvePlatformDescriptorDirectly(bom.getGroupId(), bom.getArtifactId(), bom.getVersion());
        } else {
            try {
                extensionCatalog = resolver().resolveExtensionCatalog(targetVersion.getStream());
            } catch (RegistryResolutionException e) {
                log().warn(
                        "Configured Quarkus extension registries appear to be unavailable at the moment. "
                                + "It should still be possible to create a project by providing the groupId:artifactId:version of the desired Quarkus platform BOM, "
                                + "e.g. 'quarkus create -P "
                                + ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID + ":"
                                + ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID + ":" + Version.clientVersion() + "'");
                throw e;
            }
        }

        if (VALIDATE && extensionCatalog.getQuarkusCoreVersion().startsWith("1.")) {
            throw new UnsupportedOperationException("The version 2 CLI can not be used with Quarkus 1.x projects.\n"
                    + "Use the maven/gradle plugins when working with Quarkus 1.x projects.");
        }

        return QuarkusProject.builder()
                .projectDir(projectRoot)
                .extensionCatalog(extensionCatalog)
                .buildTool(buildTool)
                .log(log())
                .build();
    }

    public ExtensionCatalogResolver getExtensionCatalogResolver() {
        return resolver();
    }

    public void saveConfig() throws IOException {
        resolver().saveConfig();
    }

    @Override
    public String toString() {
        return "RegistryClientMixin [useRegistryClient=" + useRegistryClient() + "]";
    }
}
