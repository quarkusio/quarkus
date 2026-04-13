package io.quarkus.vertx.http.deployment.spi;

import java.nio.file.Path;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Produced by extensions that ship web assets (JS modules, CSS, etc.)
 * following the mvnpm layout convention in their runtime JAR.
 * <p>
 * UI framework extensions (web-dependency-locator, web-bundler, etc.)
 * consume these to discover web dependencies without classpath scanning
 * or group-ID matching.
 */
public final class WebDependencyJarBuildItem extends MultiBuildItem {

    private final ArtifactKey artifactKey;
    private final Path jarPath;

    public WebDependencyJarBuildItem(ArtifactKey artifactKey, Path jarPath) {
        this.artifactKey = artifactKey;
        this.jarPath = jarPath;
    }

    public ArtifactKey getArtifactKey() {
        return artifactKey;
    }

    public Path getJarPath() {
        return jarPath;
    }
}
