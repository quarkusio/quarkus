package io.quarkus.deployment.builditem;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.fs.util.ZipUtils;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;

/**
 * Represents a build item for an archive root, typically used in Quarkus build steps to
 * reference application classes directories or archives (like JARs) for indexing and processing.
 *
 * <p>
 * This class contains the paths of directories or archives to be used as archive roots,
 * as well as paths that should be excluded from indexing.
 * </p>
 */
public final class ArchiveRootBuildItem extends SimpleBuildItem {

    /**
     * Builder for constructing {@link ArchiveRootBuildItem} instances.
     */
    public static class Builder {

        private List<Path> archiveRoots = new ArrayList<>();
        private Collection<Path> excludedFromIndexing;

        private Builder() {
        }

        /**
         * Adds a single archive root path to the builder.
         *
         * @param root the archive root path to add
         * @return this builder instance
         */
        public Builder addArchiveRoot(Path root) {
            this.archiveRoots.add(root);
            return this;
        }

        /**
         * Adds multiple archive root paths to the builder.
         *
         * @param paths a {@link PathCollection} of archive root paths to add
         * @return this builder instance
         */
        public Builder addArchiveRoots(PathCollection paths) {
            paths.forEach(archiveRoots::add);
            return this;
        }

        /**
         * Sets the collection of paths to exclude from indexing.
         *
         * @param excludedFromIndexing a collection of paths to be excluded
         * @return this builder instance
         */
        public Builder setExcludedFromIndexing(Collection<Path> excludedFromIndexing) {
            this.excludedFromIndexing = excludedFromIndexing;
            return this;
        }

        /**
         * @deprecated Use {@link #addArchiveRoot(Path)} instead to add archive roots.
         *             This method clears previous archive roots before setting the new one.
         *
         * @param archiveLocation the archive location to set
         * @return this builder instance
         */
        @Deprecated
        public Builder setArchiveLocation(Path archiveLocation) {
            this.archiveRoots.clear();
            this.archiveRoots.add(archiveLocation);
            return this;
        }

        /**
         * Builds the {@link ArchiveRootBuildItem} using the configured properties.
         *
         * @param buildCloseables a {@link QuarkusBuildCloseablesBuildItem} to manage opened resources (e.g., zip file systems)
         * @return a new {@link ArchiveRootBuildItem} instance
         * @throws IOException if an I/O error occurs when accessing the archive roots
         */
        public ArchiveRootBuildItem build(QuarkusBuildCloseablesBuildItem buildCloseables) throws IOException {
            return new ArchiveRootBuildItem(this, buildCloseables);
        }
    }

    /**
     * Creates a new {@link Builder} instance for building an {@link ArchiveRootBuildItem}.
     *
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    private final Path archiveRoot;
    private final Collection<Path> excludedFromIndexing;
    private final PathCollection rootDirs;
    private final PathCollection paths;

    /**
     * Constructs an {@link ArchiveRootBuildItem} with a single application classes directory.
     *
     * @param appClassesDir the path to the application classes directory
     */
    public ArchiveRootBuildItem(Path appClassesDir) {
        this(appClassesDir, appClassesDir);
    }

    /**
     * @deprecated Use {@link Builder} instead.
     *             Constructs an {@link ArchiveRootBuildItem} with a given archive location and root directory.
     *
     * @param archiveLocation the archive location (e.g., JAR file path)
     * @param archiveRoot the root directory of the archive
     */
    @Deprecated
    public ArchiveRootBuildItem(Path archiveLocation, Path archiveRoot) {
        this(archiveLocation, archiveRoot, Collections.emptySet());
    }

    private ArchiveRootBuildItem(Path archiveLocation, Path archiveRoot, Collection<Path> excludedFromIndexing) {
        if (!Files.isDirectory(archiveRoot)) {
            throw new IllegalArgumentException(archiveRoot + " does not point to the application output directory");
        }
        this.rootDirs = PathList.of(archiveRoot);
        this.paths = PathList.of(archiveLocation);
        this.archiveRoot = archiveRoot;
        this.excludedFromIndexing = excludedFromIndexing;
    }

    private ArchiveRootBuildItem(Builder builder, QuarkusBuildCloseablesBuildItem buildCloseables) throws IOException {
        this.excludedFromIndexing = builder.excludedFromIndexing;
        if (!builder.archiveRoots.isEmpty()) {
            final PathList.Builder rootDirs = PathList.builder();
            final PathList.Builder paths = PathList.builder();
            for (Path root : builder.archiveRoots) {
                paths.add(root);
                if (Files.isDirectory(root)) {
                    rootDirs.add(root);
                } else {
                    final FileSystem fs = buildCloseables.add(ZipUtils.newFileSystem(root));
                    fs.getRootDirectories().forEach(rootDirs::add);
                }
            }
            this.rootDirs = rootDirs.build();
            this.paths = paths.build();
            this.archiveRoot = this.rootDirs.iterator().next();
        } else {
            this.paths = this.rootDirs = PathsCollection.of();
            this.archiveRoot = null;
        }
    }

    /**
     * If this archive is a jar file it will return the path to the jar file on the file system,
     * otherwise it will return the directory that this corresponds to.
     *
     * @deprecated in favor of {@link #getResolvedPaths()}
     */
    @Deprecated
    public Path getArchiveLocation() {
        final Iterator<Path> i = paths.iterator();
        Path last = i.next();
        while (i.hasNext()) {
            last = i.next();
        }
        return last;
    }

    /**
     * Returns a path representing the archive root. Note that if this is a jar archive this is not the path to the
     * jar, but rather a path to the root of the mounted {@link com.sun.nio.zipfs.ZipFileSystem}.
     *
     * @return The archive root.
     * @deprecated in favor of {@link #getRootDirectories()}
     */
    @Deprecated
    public Path getArchiveRoot() {
        return archiveRoot;
    }

    /**
     * Collection of paths representing the archive's root directories. If there is a JAR among the paths
     * (returned by {@link #getResolvedPaths()}) this method will return the path to the root of the mounted
     * {@link java.nio.file.ZipFileSystem}
     * instead.
     *
     * @deprecated in favor of {@link #getRootDirectories()}
     * @return Collection of paths representing the archive's root directories
     */
    @Deprecated
    public PathsCollection getRootDirs() {
        return PathsCollection.from(rootDirs);
    }

    /**
     * Collection of paths representing the archive's root directories. If there is a JAR among the paths
     * (returned by {@link #getResolvedPaths()}) this method will return the path to the root of the mounted
     * {@link java.nio.file.ZipFileSystem}
     * instead.
     *
     * @return Collection of paths representing the archive's root directories
     */
    public PathCollection getRootDirectories() {
        return rootDirs;
    }

    /**
     * Collection of paths that collectively constitute the application archive's content.
     *
     * @deprecated in favor of {@link #getResolvedPaths()}
     * @return collection of paths that collectively constitute the application archive content
     */
    @Deprecated
    public PathsCollection getPaths() {
        return PathsCollection.from(paths);
    }

    /**
     * Collection of paths that collectively constitute the application archive's content.
     *
     * @return collection of paths that collectively constitute the application archive content
     */
    public PathCollection getResolvedPaths() {
        return paths;
    }

    public boolean isExcludedFromIndexing(Path p) {
        return excludedFromIndexing.contains(p);
    }
}
