package io.quarkus.paths;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;
import java.util.jar.Manifest;

class FilePathTree implements OpenPathTree {

    private final Path file;
    private final PathFilter pathFilter;

    FilePathTree(Path file) {
        this(file, null);
    }

    FilePathTree(Path file, PathFilter pathFilter) {
        this.file = file;
        this.pathFilter = pathFilter;
    }

    @Override
    public Collection<Path> getRoots() {
        return Collections.singletonList(file);
    }

    @Override
    public Manifest getManifest() {
        return null;
    }

    @Override
    public void walk(PathVisitor visitor) {
        if (pathFilter != null) {
            final String pathStr = file.getFileSystem().getSeparator().equals("/") ? file.toString()
                    : file.toString().replace('\\', '/');
            if (!pathFilter.isVisible(pathStr)) {
                return;
            }
            return;
        }
        visitor.visitPath(new PathVisit() {

            @Override
            public Path getRoot() {
                return file;
            }

            @Override
            public Path getPath() {
                return file;
            }

            @Override
            public void stopWalking() {
            }

            @Override
            public String getRelativePath(String separator) {
                return "";
            }
        });
    }

    @Override
    public <T> T processPath(String relativePath, Function<PathVisit, T> func, boolean multiReleaseSupport) {
        if (relativePath.isEmpty()) {
            return PathTreeVisit.processPath(file, file, file, pathFilter, func);
        }
        return func.apply(null);
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
        return Objects.hash(file, pathFilter);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FilePathTree other = (FilePathTree) obj;
        return Objects.equals(file, other.file) && Objects.equals(pathFilter, other.pathFilter);
    }
}
