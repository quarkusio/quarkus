package io.quarkus.vertx.http.deployment.spi;

import java.nio.file.Path;
import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Produced by extensions that ship web assets (JS modules, CSS, etc.)
 * following the mvnpm layout convention in their runtime JAR.
 * <p>
 * UI framework extensions (web-dependency-locator, web-bundler, etc.)
 * consume these to discover web dependencies without classpath scanning
 * or group-ID matching.
 * <p>
 * Extensions that generate their import mappings at build time can pass them
 * directly via {@link #WebDependencyJarBuildItem(ArtifactKey, Path, Map)},
 * avoiding the need for a static {@code META-INF/importmap.json} in the JAR.
 */
public final class WebDependencyJarBuildItem extends MultiBuildItem {

    private final ArtifactKey artifactKey;
    private final Path jarPath;
    private final Map<String, String> importMappings;

    public WebDependencyJarBuildItem(ArtifactKey artifactKey, Path jarPath) {
        this(artifactKey, jarPath, Map.of());
    }

    public WebDependencyJarBuildItem(ArtifactKey artifactKey, Path jarPath, Map<String, String> importMappings) {
        this.artifactKey = artifactKey;
        this.jarPath = jarPath;
        this.importMappings = importMappings;
    }

    public ArtifactKey getArtifactKey() {
        return artifactKey;
    }

    public Path getJarPath() {
        return jarPath;
    }

    /**
     * Returns the import mappings provided at build time, or an empty map
     * if the import map should be read from the JAR's {@code META-INF/importmap.json}.
     */
    public Map<String, String> getImportMappings() {
        return importMappings;
    }
}
