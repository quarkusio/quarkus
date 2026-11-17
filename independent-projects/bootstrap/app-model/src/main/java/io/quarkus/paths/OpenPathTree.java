package io.quarkus.paths;

import java.io.Closeable;
import java.nio.file.Path;

public interface OpenPathTree extends PathTree, Closeable {

    PathTree getOriginalTree();

    boolean isOpen();

    /**
     * Returns the {@link Path} for a given resource name in this open tree, or {@code null}
     * if the tree does not contain the resource.
     * <p>
     * For directory-backed trees, an empty {@code relativePath} ({@code ""}) resolves to
     * the root directory {@link Path}. For archive-backed trees, {@code ""} resolves to {@code null}.
     *
     * @param relativePath resource name to resolve
     * @return the {@link Path} for the resource, or {@code null} if not found
     */
    Path getPath(String relativePath);
}
