package io.quarkus.devtools.project;

import static io.quarkus.devtools.project.CodestartResourceLoadersBuilder.getCodestartResourceLoaders;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.buildfile.MavenProjectBuildFile;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigLocator;
import java.nio.file.Path;
import java.util.Arrays;

public class QuarkusProjectHelper {

    private static RegistriesConfig toolsConfig;
    private static MessageWriter log;
    private static MavenArtifactResolver artifactResolver;
    private static ExtensionCatalogResolver catalogResolver;

    private static boolean registryClientEnabled;
    static {
        initRegistryClientEnabled();
    }

    private static void initRegistryClientEnabled() {
        String value = System.getProperty("quarkusRegistryClient");
        if (value == null) {
            value = System.getenv("QUARKUS_REGISTRY_CLIENT");
        }
        registryClientEnabled = value == null || value.isBlank() || Boolean.parseBoolean(value);
    }

    public static boolean isRegistryClientEnabled() {
        return registryClientEnabled;
    }

    public static BuildTool detectExistingBuildTool(Path projectDirPath) {
        if (projectDirPath.resolve("pom.xml").toFile().exists()) {
            return BuildTool.MAVEN;
        } else if (projectDirPath.resolve("build.gradle").toFile().exists()) {
            return BuildTool.GRADLE;
        } else if (projectDirPath.resolve("build.gradle.kts").toFile().exists()) {
            return BuildTool.GRADLE_KOTLIN_DSL;
        } else if (projectDirPath.resolve("jbang").toFile().exists()) {
            return BuildTool.JBANG;
        } else if (projectDirPath.resolve("src").toFile().isDirectory()) {
            String[] files = projectDirPath.resolve("src").toFile().list();
            if (files != null && Arrays.asList(files).stream().anyMatch(x -> x.contains(".java"))) {
                return BuildTool.JBANG;
            }
        }
        return null;
    }

    public static QuarkusProject getProject(Path projectDir) {
        BuildTool buildTool = detectExistingBuildTool(projectDir);
        if (buildTool == null) {
            buildTool = BuildTool.MAVEN;
        }
        return getProject(projectDir, buildTool);
    }

    @Deprecated
    public static QuarkusProject getProject(Path projectDir, String quarkusVersion) {
        // TODO remove this method once the default registry becomes available
        BuildTool buildTool = detectExistingBuildTool(projectDir);
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
        if (BuildTool.MAVEN.equals(buildTool)) {
            try {
                return MavenProjectBuildFile.getProject(projectDir, messageWriter(), null);
            } catch (RegistryResolutionException e) {
                throw new RuntimeException("Failed to initialize the Quarkus Maven extension manager", e);
            }
        }
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

    public static ExtensionCatalogResolver getCatalogResolver() throws RegistryResolutionException {
        return catalogResolver == null ? catalogResolver = getCatalogResolver(true, messageWriter())
                : catalogResolver;
    }

    public static ExtensionCatalogResolver getCatalogResolver(MessageWriter log) throws RegistryResolutionException {
        return getCatalogResolver(true, log);
    }

    public static ExtensionCatalogResolver getCatalogResolver(boolean enableRegistryClient, MessageWriter log)
            throws RegistryResolutionException {
        if (catalogResolver == null) {
            if (enableRegistryClient) {
                catalogResolver = getCatalogResolver(artifactResolver(), log);
            } else {
                catalogResolver = ExtensionCatalogResolver.empty();
            }
        }
        return catalogResolver;
    }

    public static ExtensionCatalogResolver getCatalogResolver(MavenArtifactResolver resolver, MessageWriter log)
            throws RegistryResolutionException {
        return ExtensionCatalogResolver.builder()
                .artifactResolver(resolver)
                .config(toolsConfig())
                .messageWriter(log)
                .build();
    }

    public static RegistriesConfig toolsConfig() {
        return toolsConfig == null ? toolsConfig = RegistriesConfigLocator.resolveConfig() : toolsConfig;
    }

    public static void reset() {
        initRegistryClientEnabled();
        toolsConfig = null;
        artifactResolver = null;
        catalogResolver = null;
        log = null;
    }

    public static void setMessageWriter(MessageWriter newLog) {
        if (log == null) {
            log = newLog;
        }
    }

    public static MessageWriter messageWriter() {
        return log == null ? log = toolsConfig().isDebug() ? MessageWriter.debug() : MessageWriter.info() : log;
    }

    public static void setArtifactResolver(MavenArtifactResolver resolver) {
        if (artifactResolver == null) {
            artifactResolver = resolver;
        }
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
