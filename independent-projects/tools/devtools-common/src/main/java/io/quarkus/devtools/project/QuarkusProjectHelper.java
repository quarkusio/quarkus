package io.quarkus.devtools.project;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.platform.descriptor.loader.json.ClassPathResourceLoader;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigLocator;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

public class QuarkusProjectHelper {

    private static final String CODESTARTS_ARTIFACTS = "codestarts-artifacts";

    private static RegistriesConfig toolsConfig;
    private static MessageWriter log;
    private static MavenArtifactResolver artifactResolver;
    private static ExtensionCatalogResolver catalogResolver;

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
        return QuarkusProject.of(projectDir, catalog, getResourceLoader(catalog, artifactResolver()),
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
        return QuarkusProject.of(projectDir, catalog, getResourceLoader(catalog, artifactResolver()),
                log, extManager);
    }

    public static ClassPathResourceLoader getResourceLoader(ExtensionCatalog catalog) {
        return getResourceLoader(catalog, artifactResolver());
    }

    public static ClassPathResourceLoader getResourceLoader(ExtensionCatalog catalog, MavenArtifactResolver mvn) {
        Object o = catalog.getMetadata().get(CODESTARTS_ARTIFACTS);
        final List<Artifact> codestartsArtifacts;
        if (o == null) {
            // This is hardcoded temporarily
            codestartsArtifacts = Arrays
                    .asList(new DefaultArtifact(ToolsConstants.IO_QUARKUS, "quarkus-platform-descriptor-json", "", "jar",
                            catalog.getQuarkusCoreVersion()));
        } else {
            @SuppressWarnings({ "unchecked" })
            final List<Object> list = o instanceof List ? (List<Object>) o : Arrays.asList(o);
            codestartsArtifacts = new ArrayList<>(list.size());
            for (Object i : list) {
                AppArtifactCoords coords = AppArtifactCoords.fromString(i.toString());
                codestartsArtifacts.add(new DefaultArtifact(coords.getGroupId(), coords.getArtifactId(), coords.getClassifier(),
                        coords.getType(), coords.getVersion()));
            }
        }

        final URL[] urls = new URL[codestartsArtifacts.size()];
        for (int i = 0; i < codestartsArtifacts.size(); ++i) {
            try {
                urls[i] = mvn.resolve(codestartsArtifacts.get(i)).getArtifact().getFile().toURI().toURL();
            } catch (Exception e) {
                throw new RuntimeException("Failed to resolve codestart artifact " + codestartsArtifacts.get(i), e);
            }
        }

        return new ClassPathResourceLoader(new URLClassLoader(urls, null));
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
