package io.quarkus.bootstrap.workspace;

import io.quarkus.paths.PathTree;
import java.nio.file.Files;
import java.nio.file.Path;

public interface SourceDir {

    static SourceDir of(Path src, Path dest) {
        return new DefaultSourceDir(src, dest);
    }

    Path getDir();

    PathTree getSourceTree();

    default boolean isOutputAvailable() {
        final Path outputDir = getOutputDir();
        return outputDir != null && Files.exists(outputDir);
    }

    Path getOutputDir();

    PathTree getOutputTree();

    default <T> T getValue(Object key, Class<T> type) {
        return null;
    }
}
