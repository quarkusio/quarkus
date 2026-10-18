package io.quarkus.bootstrap.workspace;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.MappableCollectionFactory;
import io.quarkus.paths.DirectoryPathTree;
import io.quarkus.paths.PathFilter;
import io.quarkus.paths.PathTree;

/**
 * This implementation checks whether a directory exists before returning path trees instead of eagerly during initialization.
 */
public class LazySourceDir implements SourceDir, Serializable {

    static SourceDir fromMap(Map<String, Object> map) {
        final Path srcDir = Path.of(map.get(BootstrapConstants.MAPPABLE_SRC_DIR).toString());
        Map<String, Object> pathFilter = (Map<String, Object>) map.get(BootstrapConstants.MAPPABLE_SRC_PATH_FILTER);
        final PathFilter srcFilter = pathFilter == null ? null : PathFilter.fromMap(pathFilter);

        final Path destDir = Path.of(map.get(BootstrapConstants.MAPPABLE_DEST_DIR).toString());
        pathFilter = (Map<String, Object>) map.get(BootstrapConstants.MAPPABLE_DEST_PATH_FILTER);
        final PathFilter destFilter = pathFilter == null ? null : PathFilter.fromMap(pathFilter);

        final Path genSrcDir;
        Object o = map.get(BootstrapConstants.MAPPABLE_APT_SOURCES_DIR);
        if (o == null) {
            genSrcDir = null;
        } else {
            genSrcDir = Path.of(o.toString());
        }
        return new LazySourceDir(srcDir, srcFilter, destDir, destFilter, genSrcDir, Collections.emptyMap());
    }

    private Path srcDir;
    private PathFilter srcFilter;
    private Path destDir;
    private PathFilter destFilter;
    private Path genSrcDir;
    private Map<Object, Object> data;

    /**
     * For deserialization only
     */
    public LazySourceDir() {
    }

    public LazySourceDir(Path srcDir, Path destinationDir) {
        this(srcDir, destinationDir, null, Collections.emptyMap());
    }

    public LazySourceDir(Path srcDir, Path destinationDir, Path generatedSourcesDir) {
        this(srcDir, destinationDir, generatedSourcesDir, Collections.emptyMap());
    }

    public LazySourceDir(Path srcDir, Path destinationDir, Path generatedSourcesDir, Map<Object, Object> data) {
        this(srcDir, null, destinationDir, null, generatedSourcesDir, data);
    }

    public LazySourceDir(Path srcDir, PathFilter srcFilter, Path destDir, PathFilter destFilter, Path genSrcDir,
            Map<Object, Object> data) {
        this.srcDir = Objects.requireNonNull(srcDir, "srcDir is null");
        this.srcFilter = srcFilter;
        this.destDir = Objects.requireNonNull(destDir, "destDir is null");
        this.destFilter = destFilter;
        this.genSrcDir = genSrcDir;
        this.data = data;
    }

    @Override
    public Path getDir() {
        return srcDir;
    }

    @Override
    public PathTree getSourceTree() {
        return new DirectoryPathTree(srcDir, srcFilter);
    }

    @Override
    public Path getOutputDir() {
        return destDir;
    }

    @Override
    public Path getAptSourcesDir() {
        return genSrcDir;
    }

    @Override
    public PathTree getOutputTree() {
        return new DirectoryPathTree(destDir, destFilter);
    }

    public <T> T getValue(Object key, Class<T> type) {
        final Object o = data.get(key);
        return o == null ? null : type.cast(o);
    }

    @Override
    public Map<String, Object> asMap(MappableCollectionFactory factory) {
        final Map<String, Object> map = factory.newMap(5);
        map.put(BootstrapConstants.MAPPABLE_SRC_DIR, srcDir.toString());
        if (srcFilter != null) {
            map.put(BootstrapConstants.MAPPABLE_SRC_PATH_FILTER, srcFilter.asMap(factory));
        }
        map.put(BootstrapConstants.MAPPABLE_DEST_DIR, destDir.toString());
        if (destFilter != null) {
            map.put(BootstrapConstants.MAPPABLE_DEST_PATH_FILTER, destFilter.asMap(factory));
        }
        if (genSrcDir != null) {
            map.put(BootstrapConstants.MAPPABLE_APT_SOURCES_DIR, genSrcDir.toString());
        }
        return map;
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeUTF(srcDir.toAbsolutePath().toString());
        out.writeObject(srcFilter);
        out.writeUTF(destDir.toAbsolutePath().toString());
        out.writeObject(destFilter);
        out.writeUTF(genSrcDir == null ? "null" : genSrcDir.toAbsolutePath().toString());
        out.writeObject(data);
    }

    @Serial
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        srcDir = Path.of(in.readUTF());
        srcFilter = (PathFilter) in.readObject();
        destDir = Path.of(in.readUTF());
        destFilter = (PathFilter) in.readObject();
        final String genSrcStr = in.readUTF();
        if (!"null".equals(genSrcStr)) {
            genSrcDir = Path.of(genSrcStr);
        }
        data = (Map<Object, Object>) in.readObject();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("dir=").append(srcDir);
        if (srcFilter != null) {
            sb.append(" src-filter=").append(srcFilter);
        }
        sb.append(", dest=").append(destDir);
        if (destFilter != null) {
            sb.append(" dest-filter=").append(destFilter);
        }
        if (genSrcDir != null) {
            sb.append("  gen-src-dir=").append(genSrcDir);
        }
        return sb.toString();
    }
}
