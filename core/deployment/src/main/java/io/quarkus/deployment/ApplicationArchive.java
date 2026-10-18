package io.quarkus.deployment;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.jandex.IndexView;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.OpenPathTree;
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
     * Returns paths representing the archive root directories. Note that every path in this collection
     * is guaranteed to be a directory. If the actual application archive appears to be a JAR,
     * this collection will include a path to the root of the mounted {@link java.nio.file.FileSystem}
     * created from the JAR.
     *
     * @return The archive root directories.
     */
    PathCollection getRootDirectories();

    /**
     *
     * @return The paths representing the application root paths.
     */
    PathCollection getResolvedPaths();

    /**
     *
     * @return the artifact key or null if not available
     */
    ArtifactKey getKey();

    /**
     *
     * @return the resolved artifact or {@code null} if not available
     */
    ResolvedDependency getResolvedDependency();

    /**
     * Applies a function to the content tree of the archive.
     *
     * @param <T> result type of the function
     * @param func function to apply
     * @return the result of the function
     */
    <T> T apply(Function<OpenPathTree, T> func);

    /**
     * Accepts a consumer for the content tree of the archive.
     *
     * @param func consumer
     */
    void accept(Consumer<OpenPathTree> func);

    /**
     * Convenience method, returns the child path if it exists, otherwise null.
     *
     * @param path The child path
     * @return The child path, or null if it does not exist.
     */
    default Path getChildPath(String path) {
        return apply(tree -> tree.getPath(path));
    }
}
