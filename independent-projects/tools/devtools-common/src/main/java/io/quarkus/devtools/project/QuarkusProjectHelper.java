package io.quarkus.devtools.project;

import static io.quarkus.devtools.project.CodestartResourceLoadersBuilder.getCodestartResourceLoaders;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigLocator;
import java.nio.file.Path;

public class QuarkusProjectHelper {

    private static RegistriesConfig toolsConfig;
    private static MessageWriter log;
    private static MavenArtifactResolver artifactResolver;
    private static ExtensionCatalogResolver catalogResolver;

    private static final boolean registryClientEnabled;
    static {
        String value = System.getProperty("quarkusRegistryClient");
        if (value == null) {
            value = System.getenv("QUARKUS_REGISTRY_CLIENT");
        }
        registryClientEnabled = Boolean.parseBoolean(value);
    }

    public static boolean isRegistryClientEnabled() {
        return registryClientEnabled;
    }

    public static QuarkusProject getProject(Path projectDir) {
        BuildTool buildTool = QuarkusProject.resolveExistingProjectBuildTool(projectDir);
        if (buildTool == null) {
            buildTool = BuildTool.MAVEN;
        }
        return getProject(projectDir, buildTool);
    }

    @Deprecated
    public static QuarkusProject getProject(Path projectDir, String quarkusVersion) {
        // TODO remove this method once the default registry becomes available
        BuildTool buildTool = QuarkusProject.resolveExistingProjectBuildTool(projectDir);
        if (buildTool == null) {
            buildTool = BuildTool.MAVEN;
        }
        return getProject(projectDir, buildTool, quarkusVersion);
    }

    @Deprecated
    public static QuarkusProject getProject(Path projectDir, BuildTool buildTool, String quarkusVersion) {
        // TODO remove this method once the default registry becomes available
        return QuarkusProjectHelper.getProject(projectDir,
                getExtensionCatalog(quarkusVersion),
                buildTool);
    }

    @Deprecated
    public static ExtensionCatalog getExtensionCatalog(String quarkusVersion) {
        // TODO remove this method once the default registry becomes available
        try {
            if (registryClientEnabled && getCatalogResolver().hasRegistries()) {
                return quarkusVersion == null ? catalogResolver.resolveExtensionCatalog()
                        : catalogResolver.resolveExtensionCatalog(quarkusVersion);
            } else {
                return ToolsUtils.resolvePlatformDescriptorDirectly(null, null, quarkusVersion, artifactResolver(),
                        messageWriter());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve the Quarkus extension catalog", e);
        }
    }

    public static QuarkusProject getProject(Path projectDir, BuildTool buildTool) {
        final ExtensionCatalog catalog;
        try {
            catalog = resolveExtensionCatalog();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve the Quarkus extension catalog", e);
        }

        return getProject(projectDir, catalog, buildTool, messageWriter());
    }

    public static QuarkusProject getProject(Path projectDir, ExtensionCatalog catalog, BuildTool buildTool) {
        return getProject(projectDir, catalog, buildTool, messageWriter());
    }

    public static QuarkusProject getProject(Path projectDir, ExtensionCatalog catalog, BuildTool buildTool,
            MessageWriter log) {
        return QuarkusProject.of(projectDir, catalog, getCodestartResourceLoaders(catalog),
                log, buildTool);
    }

    public static QuarkusProject getProject(Path projectDir, ExtensionManager extManager) throws RegistryResolutionException {
        return getProject(projectDir, resolveExtensionCatalog(), extManager, messageWriter());
    }

    public static ExtensionCatalog resolveExtensionCatalog() throws RegistryResolutionException {
        return getCatalogResolver().resolveExtensionCatalog();
    }

    public static QuarkusProject getProject(Path projectDir, ExtensionCatalog catalog, ExtensionManager extManager,
            MessageWriter log) {
        return QuarkusProject.of(projectDir, catalog, getCodestartResourceLoaders(catalog),
                log, extManager);
    }

    public static ExtensionCatalogResolver getCatalogResolver() {
        return catalogResolver == null ? catalogResolver = getCatalogResolver(artifactResolver(), messageWriter())
                : catalogResolver;
    }

    public static ExtensionCatalogResolver getCatalogResolver(MessageWriter log) {
        return catalogResolver == null ? catalogResolver = getCatalogResolver(artifactResolver(), log)
                : catalogResolver;
    }

    public static ExtensionCatalogResolver getCatalogResolver(MavenArtifactResolver resolver, MessageWriter log) {
        return ExtensionCatalogResolver.builder()
                .artifactResolver(resolver)
                .config(toolsConfig())
                .messageWriter(log)
                .build();
    }

    public static RegistriesConfig toolsConfig() {
        return toolsConfig == null ? toolsConfig = RegistriesConfigLocator.resolveConfig() : toolsConfig;
    }

    public static MessageWriter messageWriter() {
        return log == null ? log = toolsConfig().isDebug() ? MessageWriter.debug() : MessageWriter.info() : log;
    }

    public static MavenArtifactResolver artifactResolver() {
        if (artifactResolver == null) {
            try {
                artifactResolver = MavenArtifactResolver.builder()
                        .setArtifactTransferLogging(toolsConfig().isDebug())
                        .setWorkspaceDiscovery(false)
                        .build();
            } catch (BootstrapMavenException e) {
                throw new IllegalStateException("Failed to initialize the Maven artifact resolver", e);
            }
        }
        return artifactResolver;
    }
}
