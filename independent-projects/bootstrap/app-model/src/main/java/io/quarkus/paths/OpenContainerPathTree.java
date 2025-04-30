package io.quarkus.paths;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

public abstract class OpenContainerPathTree extends PathTreeWithManifest implements OpenPathTree {

    private static final boolean USE_WINDOWS_ABSOLUTE_PATH_PATTERN = !FileSystems.getDefault().getSeparator().equals("/");

    private static volatile Pattern windowsAbsolutePathPattern;

    private static Pattern windowsAbsolutePathPattern() {
        return windowsAbsolutePathPattern == null ? windowsAbsolutePathPattern = Pattern.compile("[a-zA-Z]:\\\\.*")
                : windowsAbsolutePathPattern;
    }

    static boolean isAbsolutePath(String path) {
        return path != null && !path.isEmpty()
                && (path.charAt(0) == '/' // we want to check for '/' on every OS
                        || USE_WINDOWS_ABSOLUTE_PATH_PATTERN
                                && (windowsAbsolutePathPattern().matcher(path).matches())
                        || path.startsWith(FileSystems.getDefault().getSeparator()));
    }

    static void ensureResourcePath(FileSystem fs, String path) {
        if (isAbsolutePath(path)) {
            throw new IllegalArgumentException("Expected a path relative to the root of the path tree but got " + path);
        }
        // this is to disallow reading outside the path tree root
        if (path != null && path.contains("..")) {
            for (Path pathElement : fs.getPath(path)) {
                if (pathElement.toString().equals("..")) {
                    throw new IllegalArgumentException("'..' cannot be used in resource paths, but got " + path);
                }
            }
        }
    }

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
        PathTreeVisit.walk(getRootPath(), getRootPath(), getRootPath(), pathFilter, getMultiReleaseMapping(),
                visitor);
    }

    @Override
    public void walkIfContains(String relativePath, PathVisitor visitor) {
        ensureResourcePath(relativePath);
        if (!PathFilter.isVisible(pathFilter, relativePath)) {
            return;
        }
        final Path walkDir = getRootPath()
                .resolve(manifestEnabled ? toMultiReleaseRelativePath(relativePath) : relativePath);
        if (!Files.exists(walkDir)) {
            return;
        }
        PathTreeVisit.walk(getRootPath(), getRootPath(), walkDir, pathFilter, getMultiReleaseMapping(), visitor);
    }

    private void ensureResourcePath(String path) {
        ensureResourcePath(getRootPath().getFileSystem(), path);
    }

    @Override
    protected <T> T apply(String relativePath, Function<PathVisit, T> func, boolean manifestEnabled) {
        ensureResourcePath(relativePath);
        if (!PathFilter.isVisible(pathFilter, relativePath)) {
            return func.apply(null);
        }
        final Path path = getRootPath().resolve(manifestEnabled ? toMultiReleaseRelativePath(relativePath) : relativePath);
        if (!Files.exists(path)) {
            return func.apply(null);
        }
        return PathTreeVisit.process(getRootPath(), getRootPath(), path, pathFilter, func);
    }

    @Override
    public void accept(String relativePath, Consumer<PathVisit> consumer) {
        ensureResourcePath(relativePath);
        if (!PathFilter.isVisible(pathFilter, relativePath)) {
            consumer.accept(null);
            return;
        }
        final Path path = getRootPath().resolve(manifestEnabled ? toMultiReleaseRelativePath(relativePath) : relativePath);
        if (!Files.exists(path)) {
            consumer.accept(null);
            return;
        }
        PathTreeVisit.consume(getRootPath(), getRootPath(), path, pathFilter, consumer);
    }

    @Override
    public boolean contains(String relativePath) {
        ensureResourcePath(relativePath);
        if (!PathFilter.isVisible(pathFilter, relativePath)) {
            return false;
        }
        final Path path = getRootPath().resolve(manifestEnabled ? toMultiReleaseRelativePath(relativePath) : relativePath);
        return Files.exists(path);
    }

    @Override
    public Path getPath(String relativePath) {
        ensureResourcePath(relativePath);
        if (!PathFilter.isVisible(pathFilter, relativePath)) {
            return null;
        }
        final Path path = getRootPath().resolve(manifestEnabled ? toMultiReleaseRelativePath(relativePath) : relativePath);
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
