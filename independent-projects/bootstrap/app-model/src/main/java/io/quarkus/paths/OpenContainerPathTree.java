package io.quarkus.paths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class OpenContainerPathTree extends PathTreeWithManifest implements OpenPathTree {

    protected PathFilter pathFilter;

    /**
     * For deserialization of DirectoryPathTree
     */
    public OpenContainerPathTree() {
    }

    protected OpenContainerPathTree(PathFilter pathFilter) {
        this(pathFilter, false);
    }

    protected OpenContainerPathTree(PathFilter pathFilter, boolean manifestEnabled) {
        super(manifestEnabled);
        this.pathFilter = pathFilter;
    }

    protected OpenContainerPathTree(PathFilter pathFilter, PathTreeWithManifest pathTreeWithManifest) {
        super(pathTreeWithManifest);
        this.pathFilter = pathFilter;
    }

    /**
     * This is the path to the container.
     * <p>
     * In the case of a zip archive, it's the path of the archive.
     * In the case of a directory, it's the directory.
     * <p>
     * Should only be used for equals/hashCode.
     */
    protected abstract Path getContainerPath();

    /**
     * This is the path to the container.
     * <p>
     * In the case of a zip archive, it's the path to the root of the archive (i.e. a ZipPath).
     * In the case of a directory, it's the directory.
     * <p>
     * Should be used for any read operation on the container.
     */
    protected abstract Path getRootPath();

    @Override
    public OpenPathTree open() {
        return this;
    }

    @Override
    public Collection<Path> getRoots() {
        final Path rootPath = getRootPath();
        if (rootPath == null) {
            throw new RuntimeException("rootPath is null for " + getContainerPath());
        }
        return List.of(rootPath);
    }

    @Override
    public void walk(PathVisitor visitor) {
        final Path rootPath = getRootPath();
        if (!Files.exists(rootPath)) {
            return;
        }
        PathTreeVisit.walk(rootPath, rootPath, rootPath, pathFilter, getMultiReleaseMapping(),
                visitor);

    }

    @Override
    public void walkRaw(PathVisitor visitor) {
        final Path rootPath = getRootPath();
        if (!Files.exists(rootPath)) {
            return;
        }
        PathTreeVisit.walk(rootPath, rootPath, rootPath, pathFilter, Map.of(), visitor);

    }

    @Override
    public void walkIfContains(String resourceDirName, PathVisitor visitor) {
        ensureResourcePath(resourceDirName);
        if (!PathFilter.isVisible(pathFilter, resourceDirName)) {
            return;
        }
        final Path walkDir = resolveResource(resourceDirName, manifestEnabled);
        if (!Files.exists(walkDir)) {
            return;
        }
        final Path root = getRootPath();
        PathTreeVisit.walk(root, root, walkDir, pathFilter, getMultiReleaseMapping(), visitor);
    }

    private void ensureResourcePath(String path) {
        PathTreeVisit.ensureResourcePath(getRootPath().getFileSystem(), path);
    }

    private Path resolveResource(String resourceName, boolean manifestEnabled) {
        if (manifestEnabled) {
            resourceName = toMultiReleaseResourceName(resourceName);
        }
        final Path root = getRootPath();
        final String relativePath = PathTreeVisit.resourceNameToFsPath(resourceName, root.getFileSystem());
        return root.resolve(relativePath);
    }

    @Override
    protected <T> T apply(String resourceName, Function<PathVisit, T> func, boolean manifestEnabled) {
        ensureResourcePath(resourceName);
        if (!PathFilter.isVisible(pathFilter, resourceName)) {
            return func.apply(null);
        }
        final Path path = resolveResource(resourceName, manifestEnabled);
        if (!Files.exists(path)) {
            return func.apply(null);
        }
        final Path root = getRootPath();
        return PathTreeVisit.process(root, root, path, pathFilter, func);
    }

    @Override
    public void accept(String resourceName, Consumer<PathVisit> consumer) {
        ensureResourcePath(resourceName);
        if (!PathFilter.isVisible(pathFilter, resourceName)) {
            consumer.accept(null);
            return;
        }
        final Path path = resolveResource(resourceName, manifestEnabled);
        if (!Files.exists(path)) {
            consumer.accept(null);
            return;
        }
        final Path root = getRootPath();
        PathTreeVisit.consume(root, root, path, pathFilter, consumer);
    }

    @Override
    public boolean contains(String resourceName) {
        ensureResourcePath(resourceName);
        if (!PathFilter.isVisible(pathFilter, resourceName)) {
            return false;
        }
        return Files.exists(resolveResource(resourceName, manifestEnabled));
    }

    @Override
    public Path getPath(String resourceName) {
        ensureResourcePath(resourceName);
        if (!PathFilter.isVisible(pathFilter, resourceName)) {
            return null;
        }
        final Path path = resolveResource(resourceName, manifestEnabled);
        return Files.exists(path) ? path : null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getContainerPath(), pathFilter, manifestEnabled);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OpenContainerPathTree other = (OpenContainerPathTree) obj;
        return Objects.equals(getContainerPath(), other.getContainerPath())
                && Objects.equals(pathFilter, other.pathFilter)
                && manifestEnabled == other.manifestEnabled;
    }

    @Override
    public String toString() {
        return getContainerPath().toString();
    }
}
