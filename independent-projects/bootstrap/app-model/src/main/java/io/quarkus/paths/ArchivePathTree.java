package io.quarkus.paths;

import io.quarkus.fs.util.ZipUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.jar.Manifest;

public class ArchivePathTree extends PathTreeWithManifest implements PathTree {

    private final Path archive;
    private final PathFilter pathFilter;

    public ArchivePathTree(Path archive) {
        this(archive, null);
    }

    ArchivePathTree(Path archive, PathFilter pathFilter) {
        this.archive = archive;
        this.pathFilter = pathFilter;
    }

    @Override
    public Collection<Path> getRoots() {
        return Collections.singletonList(archive);
    }

    @Override
    public void walk(PathVisitor visitor) {
        try (FileSystem fs = openFs()) {
            final Path dir = fs.getPath("/");
            PathTreeVisit.walk(archive, dir, pathFilter, getMultiReleaseMapping(), visitor);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + archive, e);
        }
    }

    @Override
    public <T> T processPath(String relativePath, Function<PathVisit, T> func, boolean multiReleaseSupport) {
        if (!PathFilter.isVisible(pathFilter, relativePath)) {
            return func.apply(null);
        }
        if (multiReleaseSupport) {
            relativePath = toMultiReleaseRelativePath(relativePath);
        }
        try (FileSystem fs = openFs()) {
            for (Path root : fs.getRootDirectories()) {
                final Path path = root.resolve(relativePath);
                if (!Files.exists(path)) {
                    continue;
                }
                return PathTreeVisit.processPath(archive, root, path, pathFilter, func);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + archive, e);
        }
        return func.apply(null);
    }

    private FileSystem openFs() throws IOException {
        return ZipUtils.newFileSystem(archive);
    }

    @Override
    public OpenPathTree openTree() {
        try {
            return new OpenArchivePathTree(openFs());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(archive, pathFilter);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ArchivePathTree other = (ArchivePathTree) obj;
        return Objects.equals(archive, other.archive) && Objects.equals(pathFilter, other.pathFilter);
    }

    private class OpenArchivePathTree extends DirectoryPathTree {

        private final FileSystem fs;

        private OpenArchivePathTree(FileSystem fs) {
            super(fs.getPath("/"), pathFilter, ArchivePathTree.this);
            this.fs = fs;
        }

        @Override
        protected void initManifest(Manifest m) {
            super.initManifest(m);
            ArchivePathTree.this.manifestReadLock().lock();
            try {
                if (!ArchivePathTree.this.manifestInitialized) {
                    ArchivePathTree.this.manifestReadLock().unlock();
                    ArchivePathTree.this.manifestWriteLock().lock();
                    ArchivePathTree.this.initManifest(m);
                    ArchivePathTree.this.manifestReadLock().lock();
                    ArchivePathTree.this.manifestWriteLock().unlock();
                }
            } finally {
                ArchivePathTree.this.manifestReadLock().unlock();
            }
        }

        @Override
        protected void initMultiReleaseMapping(Map<String, String> mrMapping) {
            super.initMultiReleaseMapping(mrMapping);
            if (ArchivePathTree.this.multiReleaseMapping == null) {
                ArchivePathTree.this.initMultiReleaseMapping(mrMapping);
            }
        }

        @Override
        public void close() throws IOException {
            Throwable t = null;
            try {
                super.close();
            } catch (Throwable e) {
                t = e;
                throw e;
            } finally {
                try {
                    fs.close();
                } catch (IOException e) {
                    if (t != null) {
                        e.addSuppressed(t);
                    }
                    throw e;
                }
            }
        }

        @Override
        public PathTree getOriginalTree() {
            return ArchivePathTree.this;
        }
    }
}
