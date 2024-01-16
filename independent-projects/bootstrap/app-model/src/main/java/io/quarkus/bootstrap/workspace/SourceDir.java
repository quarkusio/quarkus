package io.quarkus.bootstrap.workspace;

import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.paths.PathTree;

public interface SourceDir {

    static SourceDir of(Path src, Path dest) {
        return of(src, dest, null);
    }

    static SourceDir of(Path src, Path dest, Path generatedSources) {
        return new DefaultSourceDir(src, dest, generatedSources);
    }

    Path getDir();

    PathTree getSourceTree();

    default boolean isOutputAvailable() {
        final Path outputDir = getOutputDir();
        return outputDir != null && Files.exists(outputDir);
    }

    Path getOutputDir();

    Path getAptSourcesDir();

    PathTree getOutputTree();

    default <T> T getValue(Object key, Class<T> type) {
        return null;
    }
}
