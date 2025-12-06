package io.quarkus.bootstrap.workspace;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.quarkus.paths.PathTree;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", defaultImpl = Void.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultSourceDir.class, name = "default"),
        @JsonSubTypes.Type(value = LazySourceDir.class, name = "lazy"),
})
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
