package io.quarkus.paths;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;

public class DirectoryPathTree extends PathTreeWithManifest implements OpenPathTree, Serializable {

    private Path dir;
    private PathFilter pathFilter;

    public DirectoryPathTree(Path dir) {
        this(dir, null);
    }

    public DirectoryPathTree(Path dir, PathFilter pathFilter) {
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

    @Override
    public <T> T processPath(String relativePath, Function<PathVisit, T> func, boolean multiReleaseSupport) {
        if (!PathFilter.isVisible(pathFilter, relativePath)) {
            return func.apply(null);
        }
        final Path path = dir.resolve(multiReleaseSupport ? toMultiReleaseRelativePath(relativePath) : relativePath);
        if (!Files.exists(path)) {
            return func.apply(null);
        }
        return PathTreeVisit.processPath(dir, dir, path, pathFilter, func);
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeUTF(dir.toAbsolutePath().toString());
        out.writeObject(pathFilter);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        dir = Paths.get(in.readUTF());
        pathFilter = (PathFilter) in.readObject();
    }

    @Override
    public OpenPathTree openTree() {
        return this;
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
        return Objects.hash(dir, pathFilter);
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
        return Objects.equals(dir, other.dir) && Objects.equals(pathFilter, other.pathFilter);
    }
}
