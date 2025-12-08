package io.quarkus.bootstrap.workspace;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.Mappable;
import io.quarkus.bootstrap.model.MappableCollectionFactory;
import io.quarkus.paths.PathTree;

public interface SourceDir extends Mappable {

    static SourceDir of(Path src, Path dest) {
        return of(src, dest, null);
    }

    static SourceDir of(Path src, Path dest, Path generatedSources) {
        return new LazySourceDir(src, dest, generatedSources);
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

    @Override
    default Map<String, Object> asMap(MappableCollectionFactory factory) {
        final Map<String, Object> map = factory.newMap(3);
        final Path dir = getDir();
        if (dir != null) {
            map.put(BootstrapConstants.MAPPABLE_SRC_DIR, dir.toString());
        }
        final Path outputDir = getOutputDir();
        if (outputDir != null) {
            map.put(BootstrapConstants.MAPPABLE_DEST_DIR, outputDir.toString());
        }
        final Path aptSourcesDir = getAptSourcesDir();
        if (aptSourcesDir != null) {
            map.put(BootstrapConstants.MAPPABLE_APT_SOURCES_DIR, aptSourcesDir.toString());
        }
        return map;
    }
}
