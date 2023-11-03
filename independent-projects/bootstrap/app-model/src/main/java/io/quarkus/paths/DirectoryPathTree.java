package io.quarkus.paths;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

public class DirectoryPathTree extends PathTreeWithManifest implements OpenPathTree, Serializable {

    private static final long serialVersionUID = 2255956884896445059L;

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

    private Path dir;
    private PathFilter pathFilter;

    /**
     * For deserialization
     */
    public DirectoryPathTree() {
        super();
    }

    public DirectoryPathTree(Path dir) {
        this(dir, null);
    }

    public DirectoryPathTree(Path dir, PathFilter pathFilter) {
        this(dir, pathFilter, false);
    }

    public DirectoryPathTree(Path dir, PathFilter pathFilter, boolean manifestEnabled) {
        super(manifestEnabled);
        this.dir = dir;
        this.pathFilter = pathFilter;
    }

    protected DirectoryPathTree(Path dir, PathFilter pathFilter, PathTreeWithManifest pathTreeWithManifest) {
        super(pathTreeWithManifest);
        this.dir = dir;
        this.pathFilter = pathFilter;
    }

    @Override
    public Collection<Path> getRoots() {
        return Collections.singletonList(dir);
    }

    @Override
    public void walk(PathVisitor visitor) {
        PathTreeVisit.walk(dir, dir, pathFilter, getMultiReleaseMapping(), visitor);
    }

    private void ensureResourcePath(String path) {
        ensureResourcePath(dir.getFileSystem(), path);
    }

    @Override
    protected <T> T apply(String relativePath, Function<PathVisit, T> func, boolean manifestEnabled) {
        ensureResourcePath(relativePath);
        if (!PathFilter.isVisible(pathFilter, relativePath)) {
            return func.apply(null);
        }
        final Path path = dir.resolve(manifestEnabled ? toMultiReleaseRelativePath(relativePath) : relativePath);
        if (!Files.exists(path)) {
            return func.apply(null);
        }
        return PathTreeVisit.process(dir, dir, path, pathFilter, func);
    }

    @Override
    public void accept(String relativePath, Consumer<PathVisit> consumer) {
        ensureResourcePath(relativePath);
        if (!PathFilter.isVisible(pathFilter, relativePath)) {
            consumer.accept(null);
            return;
        }
        final Path path = dir.resolve(manifestEnabled ? toMultiReleaseRelativePath(relativePath) : relativePath);
        if (!Files.exists(path)) {
            consumer.accept(null);
            return;
        }
        PathTreeVisit.consume(dir, dir, path, pathFilter, consumer);
    }

    @Override
    public boolean contains(String relativePath) {
        ensureResourcePath(relativePath);
        if (!PathFilter.isVisible(pathFilter, relativePath)) {
            return false;
        }
        final Path path = dir.resolve(manifestEnabled ? toMultiReleaseRelativePath(relativePath) : relativePath);
        return Files.exists(path);
    }

    @Override
    public Path getPath(String relativePath) {
        ensureResourcePath(relativePath);
        if (!PathFilter.isVisible(pathFilter, relativePath)) {
            return null;
        }
        final Path path = dir.resolve(manifestEnabled ? toMultiReleaseRelativePath(relativePath) : relativePath);
        return Files.exists(path) ? path : null;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeUTF(dir.toAbsolutePath().toString());
        out.writeObject(pathFilter);
        out.writeBoolean(manifestEnabled);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        dir = Paths.get(in.readUTF());
        pathFilter = (PathFilter) in.readObject();
        manifestEnabled = in.readBoolean();
    }

    @Override
    public OpenPathTree open() {
        return this;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public PathTree getOriginalTree() {
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dir, pathFilter, manifestEnabled);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DirectoryPathTree other = (DirectoryPathTree) obj;
        return Objects.equals(dir, other.dir) && Objects.equals(pathFilter, other.pathFilter)
                && manifestEnabled == other.manifestEnabled;
    }

}
