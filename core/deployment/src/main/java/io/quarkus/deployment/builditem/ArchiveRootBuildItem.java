package io.quarkus.deployment.builditem;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.builder.item.SimpleBuildItem;

public final class ArchiveRootBuildItem extends SimpleBuildItem {

    public static class Builder {

        private List<Path> archiveRoots = new ArrayList<>();
        private Collection<Path> excludedFromIndexing;

        private Builder() {
        }

        public Builder addArchiveRoot(Path root) {
            this.archiveRoots.add(root);
            return this;
        }

        public Builder addArchiveRoots(PathsCollection paths) {
            paths.forEach(archiveRoots::add);
            return this;
        }

        public Builder setExcludedFromIndexing(Collection<Path> excludedFromIndexing) {
            this.excludedFromIndexing = excludedFromIndexing;
            return this;
        }

        @Deprecated
        public Builder setArchiveLocation(Path archiveLocation) {
            this.archiveRoots.clear();
            this.archiveRoots.add(archiveLocation);
            return this;
        }

        public ArchiveRootBuildItem build(QuarkusBuildCloseablesBuildItem buildCloseables) throws IOException {
            return new ArchiveRootBuildItem(this, buildCloseables);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final Path archiveRoot;
    private final Collection<Path> excludedFromIndexing;
    private final PathsCollection rootDirs;
    private final PathsCollection paths;

    public ArchiveRootBuildItem(Path appClassesDir) {
        this(appClassesDir, appClassesDir);
    }

    @Deprecated
    public ArchiveRootBuildItem(Path archiveLocation, Path archiveRoot) {
        this(archiveLocation, archiveRoot, Collections.emptySet());
    }

    private ArchiveRootBuildItem(Path archiveLocation, Path archiveRoot, Collection<Path> excludedFromIndexing) {
        if (!Files.isDirectory(archiveRoot)) {
            throw new IllegalArgumentException(archiveRoot + " does not point to the application output directory");
        }
        this.rootDirs = PathsCollection.of(archiveRoot);
        this.paths = PathsCollection.of(archiveLocation);
        this.archiveRoot = archiveRoot;
        this.excludedFromIndexing = excludedFromIndexing;
    }

    private ArchiveRootBuildItem(Builder builder, QuarkusBuildCloseablesBuildItem buildCloseables) throws IOException {
        this.excludedFromIndexing = builder.excludedFromIndexing;
        if (!builder.archiveRoots.isEmpty()) {
            final PathsCollection.Builder rootDirs = PathsCollection.builder();
            final PathsCollection.Builder paths = PathsCollection.builder();
            for (Path root : builder.archiveRoots) {
                paths.add(root);
                if (Files.isDirectory(root)) {
                    rootDirs.add(root);
                } else {
                    final FileSystem fs = buildCloseables.add(FileSystems.newFileSystem(root, (ClassLoader) null));
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
     * @deprecated in favor of {@link #getPaths()}
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
     *
     * Returns a path representing the archive root. Note that if this is a jar archive this is not the path to the
     * jar, but rather a path to the root of the mounted {@link com.sun.nio.zipfs.ZipFileSystem}
     *
     * @return The archive root.
     * @deprecated in favor of {@link #getRootDirs()}
     */
    @Deprecated
    public Path getArchiveRoot() {
        return archiveRoot;
    }

    /**
     * Collection of path representing the archive's root directories. If there is a JAR among the paths
     * (returned by {@link #getPaths()} this method will return the path to the root of the mounted
     * {@link java.nio.file.ZipFileSystem}
     * instead.
     *
     * @return Collection of path representing the archive's root directories.
     */
    public PathsCollection getRootDirs() {
        return rootDirs;
    }

    /**
     * Collection of paths that collectively constitute the application archive's content.
     *
     * @return collection of paths that collectively constitute the application archive content.
     */
    public PathsCollection getPaths() {
        return paths;
    }

    public boolean isExcludedFromIndexing(Path p) {
        return excludedFromIndexing.contains(p);
    }
}
