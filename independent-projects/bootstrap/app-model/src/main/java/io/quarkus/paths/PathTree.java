package io.quarkus.paths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.Manifest;

public interface PathTree {

    static PathTree ofDirectoryOrFile(Path p) {
        try {
            final BasicFileAttributes fileAttributes = Files.readAttributes(p, BasicFileAttributes.class);
            return fileAttributes.isDirectory() ? new DirectoryPathTree(p) : new FilePathTree(p);
        } catch (IOException e) {
            throw new IllegalArgumentException(p + " does not exist", e);
        }
    }

    static PathTree ofDirectoryOrArchive(Path p) {
        try {
            final BasicFileAttributes fileAttributes = Files.readAttributes(p, BasicFileAttributes.class);
            return fileAttributes.isDirectory() ? new DirectoryPathTree(p) : new ArchivePathTree(p);
        } catch (IOException e) {
            throw new IllegalArgumentException(p + " does not exist", e);
        }
    }

    static PathTree ofArchive(Path archive) {
        if (!Files.exists(archive)) {
            throw new IllegalArgumentException(archive + " does not exist");
        }
        return new ArchivePathTree(archive);
    }

    Collection<Path> getRoots();

    default boolean isEmpty() {
        return getRoots().isEmpty();
    }

    Manifest getManifest();

    void walk(PathVisitor visitor);

    <T> T apply(String relativePath, Function<PathVisit, T> func);

    void accept(String relativePath, Consumer<PathVisit> consumer);

    boolean contains(String relativePath);

    OpenPathTree open();
}
