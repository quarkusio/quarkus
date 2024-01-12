package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;
import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a runnable artifact, such as an uberjar or thin jar.
 *
 * Most artifacts will also produce a more specialized build item, to allow them
 * to be consumed by other build steps.
 *
 */
public final class ArtifactResultBuildItem extends MultiBuildItem {

    private final Path path;
    private final String type;
    private final Map<String, String> metadata;

    public ArtifactResultBuildItem(Path path, String type, Map<String, String> metadata) {
        this.path = path;
        this.type = type;
        this.metadata = metadata;
    }

    public Path getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
