package io.quarkus.devtools.project;

import static io.quarkus.platform.catalog.processor.CatalogProcessor.getCodestartArtifacts;
import static java.util.Objects.requireNonNull;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.DependencyNodeUtils;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.platform.catalog.processor.ExtensionProcessor;
import io.quarkus.platform.descriptor.loader.json.ClassPathResourceLoader;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.config.PropertiesUtil;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigLocator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.eclipse.aether.artifact.Artifact;

public class QuarkusProjectHelper {

    private static final String BASE_CODESTARTS_ARTIFACT_PROPERTY = "quarkus-base-codestart-artifact";
    private static final String BASE_CODESTARTS_ARTIFACT_PROPERTIES_NAME = "/quarkus-devtools-base-codestarts.properties";

    private static RegistriesConfig toolsConfig;
    private static MessageWriter log;
    private static MavenArtifactResolver artifactResolver;
    private static ExtensionCatalogResolver catalogResolver;
    private static final String BASE_CODESTARTS_ARTIFACT_COORDS = retrieveBaseCodestartsArtifactCoords();

    private static String retrieveBaseCodestartsArtifactCoords() {
        final String artifact = PropertiesUtil.getProperty(BASE_CODESTARTS_ARTIFACT_PROPERTY);
        if (artifact != null) {
            return artifact;
        }
        try {
            final Properties properties = new Properties();
            final InputStream resource = requireNonNull(
                    QuarkusProjectHelper.class.getResourceAsStream(BASE_CODESTARTS_ARTIFACT_PROPERTIES_NAME),
                    BASE_CODESTARTS_ARTIFACT_PROPERTIES_NAME + " resource not found.");
            properties.load(resource);
            return requireNonNull(properties.getProperty("artifact"),
                    "base codestarts 'artifact' property not found");
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't load the base codestarts artifact properties", e);
        }
    }

    public static QuarkusProject getProject(Path projectDir) {
        BuildTool buildTool = QuarkusProject.resolveExistingProjectBuildTool(projectDir);
        if (buildTool == null) {
            buildTool = BuildTool.MAVEN;
        }
        return getProject(projectDir, buildTool);
    }

    public static QuarkusProject getProject(Path projectDir, String quarkusVersion) {
        // TODO remove this method once the default registry becomes available
        BuildTool buildTool = QuarkusProject.resolveExistingProjectBuildTool(projectDir);
        if (buildTool == null) {
            buildTool = BuildTool.MAVEN;
        }
        return getProject(projectDir, buildTool, quarkusVersion);
    }

    public static QuarkusProject getProject(Path projectDir, BuildTool buildTool, String quarkusVersion) {
        // TODO remove this method once the default registry becomes available
        final ExtensionCatalogResolver catalogResolver = getCatalogResolver();
        if (catalogResolver.hasRegistries()) {
            return getProject(projectDir, buildTool);
        }
        return QuarkusProjectHelper.getProject(projectDir,
                ToolsUtils.resolvePlatformDescriptorDirectly(null, null, quarkusVersion, artifactResolver(), messageWriter()),
                buildTool);
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

    public static List<ResourceLoader> getCodestartResourceLoaders(ExtensionCatalog catalog) {
        return getCodestartResourceLoaders(catalog, artifactResolver());
    }

    public static List<ResourceLoader> getBaseCodestartResourceLoaders() {
        return getCodestartResourceLoaders(null, artifactResolver());
    }

    public static List<ResourceLoader> getCodestartResourceLoaders(ExtensionCatalog catalog,
            MavenArtifactResolver mvn) {
        return getCodestartResourceLoaders(BASE_CODESTARTS_ARTIFACT_COORDS, catalog, mvn);
    }

    public static List<ResourceLoader> getCodestartResourceLoaders(String baseCodestartsArtifactCoords,
            ExtensionCatalog catalog,
            MavenArtifactResolver mvn) {
        final Map<String, Artifact> codestartsArtifacts = new LinkedHashMap<>();
        if (catalog != null) {
            // Load codestarts from each extensions codestart artifacts
            for (Extension e : catalog.getExtensions()) {
                final String artifactCoords = ExtensionProcessor.of(e).getCodestartArtifact();
                if (artifactCoords == null || codestartsArtifacts.containsKey(artifactCoords)) {
                    continue;
                }
                codestartsArtifacts.put(artifactCoords, DependencyNodeUtils.toArtifact(artifactCoords));
            }

            // Load codestarts from catalog codestart artifacts
            final List<String> catalogCodestartArtifacts = getCodestartArtifacts(catalog);
            for (String artifactCoords : catalogCodestartArtifacts) {
                if (codestartsArtifacts.containsKey(artifactCoords)) {
                    continue;
                }
                codestartsArtifacts.put(artifactCoords, DependencyNodeUtils.toArtifact(artifactCoords));
            }
        }
        // Load codestarts from the base artifact
        codestartsArtifacts.put(baseCodestartsArtifactCoords, DependencyNodeUtils.toArtifact(baseCodestartsArtifactCoords));

        final List<ResourceLoader> codestartResourceLoaders = new ArrayList<>(codestartsArtifacts.size());
        for (Artifact a : codestartsArtifacts.values()) {
            try {
                final URL artifactUrl = mvn.resolve(a).getArtifact().getFile().toURI().toURL();
                final ClassPathResourceLoader resourceLoader = new ClassPathResourceLoader(
                        new URLClassLoader(new URL[] { artifactUrl }, null));
                codestartResourceLoaders.add(resourceLoader);
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve codestart artifact " + a, e);
            }
        }
        return codestartResourceLoaders;
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

    private static RegistriesConfig toolsConfig() {
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
