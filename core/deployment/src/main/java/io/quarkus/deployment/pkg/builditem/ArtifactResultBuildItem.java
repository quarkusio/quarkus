package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;
import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.sbom.CoreSbomContributionConfig;

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
    private final CoreSbomContributionConfig coreSbomConfig;

    public ArtifactResultBuildItem(Path path, String type, Map<String, String> metadata) {
        this(path, type, metadata, null);
    }

    public ArtifactResultBuildItem(Path path, String type, Map<String, String> metadata,
            CoreSbomContributionConfig manifestConfig) {
        this.path = path;
        this.type = type;
        this.metadata = metadata;
        this.coreSbomConfig = manifestConfig;
    }

    public Path getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public CoreSbomContributionConfig getCoreSbomConfig() {
        return coreSbomConfig;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
