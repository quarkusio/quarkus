package io.quarkus.bootstrap.app;

import java.nio.file.Path;
import java.util.Map;

public class ArtifactResult {

    private final Path path;
    private final String type;
    private final Map<String, Path> additionalPaths;

    public ArtifactResult(Path path, String type, Map<String, Path> additionalPaths) {
        this.path = path;
        this.type = type;
        this.additionalPaths = additionalPaths;
    }

    public Path getPath() {
        return path;
    }

    public String getType() {
        return type;
    }

    public Map<String, Path> getAdditionalPaths() {
        return additionalPaths;
    }
}
