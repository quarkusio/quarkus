package io.quarkus.paths;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Provides context for a given path visit
 */
public interface PathVisit {

    /**
     * The root of the path tree the current path belongs to.
     * For a {@link PathTree} created for an archive, this will be the path to the archive file.
     * For a {@link PathTree} created for a directory, this will be the path to the directory.
     *
     * @return root of the path tree the current path belongs to
     */
    Path getRoot();

    /**
     * The path being visited. The {@link java.nio.file.FileSystem} the path belongs to
     * will depend on the implementation of the {@link PathTree} being visited. For example,
     * for an archive it will be a ZIP {@link java.nio.file.FileSystem} implementation.
     *
     * The returned path is granted to exist in the underlying file system (unless it was deleted by a parallel process or
     * thread) and therefore it can be used for I/O operations straight away without further resolving, e.g. against
     * {@link #getRoot()}
     *
     * @return path being visited
     */
    Path getPath();

    /**
     * {@link java.net.URL} that can be used to read the content of the path.
     *
     * @return URL that can be used to read the content of the path
     */
    default URL getUrl() {
        try {
            return getPath().toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to translate " + getPath().toUri() + " to " + URL.class.getName(), e);
        }
    }

    /**
     * Path relative to the root of the tree as a string with {@code /} as a path element separator.
     * This method calls {@link #getRelativePath(String)} passing {@code /} as an argument.
     *
     * @return path relative to the root of the tree as a string with {@code /} as a path element separator
     */
    default String getRelativePath() {
        return getRelativePath("/");
    }

    /**
     * Path relative to the root of the tree as a string with a provided path element separator.
     * For a {@link PathTree} created for an archive, the returned path will be relative to the root
     * of the corresponding {@link java.nio.file.FileSystem} implementation.
     * For a {@link PathTree} created for a directory, the returned path will be relative to the directory
     * used as the root of the path tree.
     *
     * @param separator path element separator
     * @return path relative to the root of the tree as a string with a provided path element separator
     */
    String getRelativePath(String separator);

    /**
     * Terminates walking over a {@link PathTree} after this visit.
     */
    void stopWalking();
}
