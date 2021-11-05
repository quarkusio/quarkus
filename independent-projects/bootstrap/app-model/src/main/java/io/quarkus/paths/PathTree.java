package io.quarkus.paths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Function;
import java.util.jar.Manifest;

public interface PathTree {

    static PathTree of(Path p) {
        if (Files.isDirectory(p)) {
            return new DirectoryPathTree(p);
        }
        if (Files.exists(p)) {
            return new FilePathTree(p);
        }
        throw new IllegalArgumentException(p + " does not exist");
    }

    static PathTree ofArchive(Path archive) {
        if (!Files.exists(archive)) {
            throw new IllegalArgumentException(archive + " does not exist");
        }
        return new ArchivePathTree(archive);
    }

    static PathTreeBuilder builder() {
        return new PathTreeBuilder(false);
    }

    static PathTreeBuilder archiveBuilder() {
        return new PathTreeBuilder(true);
    }

    Collection<Path> getRoots();

    default boolean isEmpty() {
        return getRoots().isEmpty();
    }

    Manifest getManifest();

    void walk(PathVisitor visitor);

    default <T> T processPath(String relativePath, Function<PathVisit, T> func) {
        return processPath(relativePath, func, true);
    }

    <T> T processPath(String relativePath, Function<PathVisit, T> func, boolean multiReleaseSupport);

    OpenPathTree openTree();
}
