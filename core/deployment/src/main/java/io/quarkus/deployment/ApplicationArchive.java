package io.quarkus.deployment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.BiConsumer;

import org.jboss.jandex.IndexView;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.paths.PathCollection;

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
     * If this archive is a jar file it will return the path to the jar file on the file system,
     * otherwise it will return the directory that this corresponds to.
     *
     * @deprecated in favor of {@link #getResolvedPaths()}
     */
    @Deprecated
    Path getArchiveLocation();

    /**
     * @deprecated in favor of {@link #getRootDirectories()}
     *
     *             Returns paths representing the archive root directories. Note that every path in this collection
     *             is guaranteed to be a directory. If the actual application archive appears to be a JAR,
     *             this collection will include a path to the root of the mounted {@link java.nio.file.FileSystem}
     *             created from the JAR.
     *
     * @return The archive root directories.
     */
    @Deprecated
    PathsCollection getRootDirs();

    /**
     * Returns paths representing the archive root directories. Note that every path in this collection
     * is guaranteed to be a directory. If the actual application archive appears to be a JAR,
     * this collection will include a path to the root of the mounted {@link java.nio.file.FileSystem}
     * created from the JAR.
     *
     * @return The archive root directories.
     */
    PathCollection getRootDirectories();

    /**
     * @deprecated in favor of {@link #getResolvedPaths()}
     * @return The paths representing the application root paths.
     */
    @Deprecated
    PathsCollection getPaths();

    /**
     * 
     * @return The paths representing the application root paths.
     */
    PathCollection getResolvedPaths();

    /**
     * @deprecated in favor of {@link #getKey()}
     * @return the artifact key or null if not available
     */
    AppArtifactKey getArtifactKey();

    /**
     *
     * @return the artifact key or null if not available
     */
    ArtifactKey getKey();

    /**
     * Convenience method, returns the child path if it exists, otherwise null.
     *
     * @param path The child path
     * @return The child path, or null if it does not exist.
     */
    default Path getChildPath(String path) {
        return getRootDirectories().resolveExistingOrNull(path);
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
        final Iterator<Path> dirs = getRootDirectories().iterator();
        final Iterator<Path> paths = getResolvedPaths().iterator();
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
