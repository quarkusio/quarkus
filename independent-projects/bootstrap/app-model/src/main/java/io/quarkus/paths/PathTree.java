package io.quarkus.paths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public interface PathTree {

    static PathTree ofDirectoryOrFile(Path p) {
        return ofDirectoryOrFile(p, null);
    }

    static PathTree ofDirectoryOrFile(Path p, PathFilter filter) {
        try {
            final BasicFileAttributes fileAttributes = Files.readAttributes(p, BasicFileAttributes.class);
            return fileAttributes.isDirectory() ? new DirectoryPathTree(p, filter) : new FilePathTree(p, filter);
        } catch (IOException e) {
            throw new IllegalArgumentException(p + " does not exist", e);
        }
    }

    /**
     * Creates a new {@link PathTree} for a given existing path which is expected to be
     * either a directory or a ZIP-based archive.
     *
     * @param p path to a directory or an archive
     * @return an instance of {@link PathTree} for a given existing directory or an archive
     */
    static PathTree ofDirectoryOrArchive(Path p) {
        return ofDirectoryOrArchive(p, null);
    }

    /**
     * Creates a new {@link PathTree} for a given existing path which is expected to be
     * either a directory or a ZIP-based archive applying a provided {@link PathFilter}
     * unless it is {@code null}.
     *
     * @param p path to a directory or an archive
     * @param filter path filter to apply, could be {@code null}
     * @return an instance of {@link PathTree} for a given existing directory or an archive
     */
    static PathTree ofDirectoryOrArchive(Path p, PathFilter filter) {
        try {
            final BasicFileAttributes fileAttributes = Files.readAttributes(p, BasicFileAttributes.class);
            return fileAttributes.isDirectory() ? new DirectoryPathTree(p, filter)
                    // invoke ArchivePathTree.forPath directly instead of ofArchive() to avoid an extra Files.exists invocation
                    : ArchivePathTree.forPath(p, filter);
        } catch (IOException e) {
            throw new IllegalArgumentException(p + " does not exist", e);
        }
    }

    /**
     * Creates a new {@link PathTree} for an existing path that is expected to be
     * a ZIP-based archive.
     *
     * @param archive path to an archive
     * @return an instance of {@link PathTree} for a given archive
     */
    static PathTree ofArchive(Path archive) {
        return ofArchive(archive, null);
    }

    /**
     * Creates a new {@link PathTree} for an existing path that is expected to be
     * a ZIP-based archive applying a provided {@link PathFilter} unless it is {@code null}.
     *
     * @param archive path to an archive
     * @param filter path filter to apply, could be {@code null}
     * @return an instance of {@link PathTree} for a given archive
     */
    static PathTree ofArchive(Path archive, PathFilter filter) {
        if (!Files.exists(archive)) {
            throw new IllegalArgumentException(archive + " does not exist");
        }
        return ArchivePathTree.forPath(archive, filter);
    }

    /**
     * Whether the content of this tree comes from an archive or not.
     * <p>
     * This is useful for instance when you want to determine if the resources can be updated in dev mode.
     */
    boolean isArchiveOrigin();

    /**
     * The roots of the path tree.
     * <p>
     * Note that you shouldn't use these roots for browsing except if the PathTree is open.
     *
     * @return roots of the path tree
     */
    Collection<Path> getRoots();

    /**
     * Checks whether the tree is empty
     *
     * @return true, if the tree is empty, otherwise - false
     */
    default boolean isEmpty() {
        return getRoots().isEmpty();
    }

    /**
     * If {@code META-INF/MANIFEST.MF} found, reads it and returns an instance of {@link ManifestAttributes},
     * a trimmed down version of the Manifest, otherwise returns null.
     *
     * @return parsed {@code META-INF/MANIFEST.MF} if it's found, otherwise {@code null}
     */
    ManifestAttributes getManifestAttributes();

    /**
     * Walks the tree.
     *
     * @param visitor path visitor
     */
    void walk(PathVisitor visitor);

    /**
     * Walks a subtree of this tree that begins with a passed in {@code relativePath},
     * if the tree contains {@code relativePath}. If the tree does not contain {@code relativePath}
     * then the method returns without an error.
     * <p>
     * This method does not create a new {@link PathTree} with the root at {@code relativePath}.
     * It simply applies an inclusion filter to this {@link PathTree} instance, keeping the same root.
     *
     * @param relativePath relative path from which the walk should begin
     * @param visitor path visitor
     */
    void walkIfContains(String relativePath, PathVisitor visitor);

    /**
     * Applies a function to a given path relative to the root of the tree.
     * If the path isn't found in the tree, the {@link PathVisit} argument
     * passed to the function will be {@code null}.
     *
     * @param <T> resulting type
     * @param relativePath relative path to process
     * @param func processing function
     * @return result of the function
     */
    <T> T apply(String relativePath, Function<PathVisit, T> func);

    /**
     * Consumes a given path relative to the root of the tree.
     * If the path isn't found in the tree, the {@link PathVisit} argument
     * passed to the consumer will be {@code null}.
     *
     * @param relativePath relative path to consume
     * @param consumer path consumer
     */
    void accept(String relativePath, Consumer<PathVisit> consumer);

    /**
     * Consumes a given path relative to the root of the tree.
     * If the path isn't found in the tree, the {@link PathVisit} argument
     * passed to the consumer will be {@code null}.
     *
     * If multiple items match then the consumer will be called multiple times.
     *
     * @param relativePath relative path to consume
     * @param consumer path consumer
     */
    default void acceptAll(String relativePath, Consumer<PathVisit> consumer) {
        accept(relativePath, consumer);
    }

    /**
     * Checks whether the tree contains a relative path.
     *
     * @param relativePath path relative to the root of the tree
     * @return true, in case the tree contains the path, otherwise - false
     */
    boolean contains(String relativePath);

    /**
     * Returns an {@link OpenPathTree} for this tree, which is supposed to be
     * closed at the end of processing. It is meant to be an optimization when
     * processing multiple paths of path trees that represent archives.
     * If a path tree does not represent an archive but a directory, for example,
     * this method is expected to be a no-op, returning the original tree as an
     * instance of {@link OpenPathTree}.
     *
     * @return an instance of {@link OpenPathTree} for this path tree
     */
    OpenPathTree open();

    /**
     * Creates a new {@link PathTree} instance applying a path filter to this {@link PathTree} instance.
     *
     * @param filter path filter to apply
     * @return a new {@link PathTree} instance applying a path filter to this {@link PathTree} instance
     */
    default PathTree filter(PathFilter filter) {
        return new FilteredPathTree(this, filter);
    }
}
