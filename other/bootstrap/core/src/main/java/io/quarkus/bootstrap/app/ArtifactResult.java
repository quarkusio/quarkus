package io.quarkus.bootstrap.app;

import java.nio.file.Path;
import java.util.Map;

public class ArtifactResult {

    private final Path path;
    private final String type;
    private final Map<String, Object> metadata;

    public ArtifactResult(Path path, String type, Map<String, Object> metadata) {
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
