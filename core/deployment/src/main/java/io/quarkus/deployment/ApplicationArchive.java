package io.quarkus.deployment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.BiConsumer;

import org.jboss.jandex.IndexView;

import io.quarkus.bootstrap.model.PathsCollection;

/**
 * Represents an archive that is part of application code.
 * <p>
 * An application archive is an archive that provides components to the application. As a result it will be indexed
 * via jandex, and any deployment descriptors will be made available to the application.
 */
public interface ApplicationArchive {

    /**
     *
     * @return The index of this application Archive
     */
    IndexView getIndex();

    /**
     *
     * Returns a path representing the archive root. Note that if this is a jar archive this is not the path to the
     * jar, but rather a path to the root of the mounted {@link com.sun.nio.zipfs.ZipFileSystem}
     *
     * @return The archive root.
     * @deprecated in favor of {@link #getRootDirs()}
     */
    @Deprecated
    Path getArchiveRoot();

    /**
     *
     * @return <code>true</code> if this archive is a jar
     * @deprecated does not appear to be used anywhere and now it shouldn't be
     */
    @Deprecated
    boolean isJarArchive();

    /**
     * If this archive is a jar file it will return the path to the jar file on the file system,
     * otherwise it will return the directory that this corresponds to.
     *
     * @deprecated in favor of {@link #getPaths()}
     */
    @Deprecated
    Path getArchiveLocation();

    /**
     *
     * Returns paths representing the archive root directories. Note that every path in this collection
     * is guaranteed to be a directory. If the actual application archive appears to be a JAR,
     * this collection will include a path to the root of the mounted {@link java.nio.file.FileSystem}
     * created from the JAR.
     *
     * @return The archive root directories.
     */
    PathsCollection getRootDirs();

    /**
     * Returns paths representing the application root paths.
     */
    PathsCollection getPaths();

    /**
     * Convenience method, returns the child path if it exists, otherwise null.
     *
     * @param path The child path
     * @return The child path, or null if it does not exist.
     */
    default Path getChildPath(String path) {
        return getRootDirs().resolveExistingOrNull(path);
    }

    /**
     * Searches for the specified entry among the archive paths. If a root path appears to be a JAR,
     * the entry will be searched among its entries. The first matched entry will be passed to the
     * consumer along with its root path.
     *
     * @param path relative entry path
     * @param consumer entry consumer
     */
    default void processEntry(String path, BiConsumer<Path, Path> consumer) {
        final Iterator<Path> dirs = getRootDirs().iterator();
        final Iterator<Path> paths = getPaths().iterator();
        while (dirs.hasNext()) {
            final Path child = dirs.next().resolve(path);
            if (Files.exists(child)) {
                consumer.accept(child, paths.next());
                return;
            }
            paths.next();
        }
    }
}
