package io.quarkus.bootstrap.workspace;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import io.quarkus.paths.DirectoryPathTree;
import io.quarkus.paths.EmptyPathTree;
import io.quarkus.paths.PathFilter;
import io.quarkus.paths.PathTree;

/**
 * This implementation checks whether a directory exists before returning path trees instead of eagerly during initialization.
 */
public class LazySourceDir implements SourceDir, Serializable {

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
        return Files.exists(srcDir) ? new DirectoryPathTree(srcDir, srcFilter) : EmptyPathTree.getInstance();
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
        return Files.exists(destDir) ? new DirectoryPathTree(destDir, destFilter) : EmptyPathTree.getInstance();
    }

    public <T> T getValue(Object key, Class<T> type) {
        final Object o = data.get(key);
        return o == null ? null : type.cast(o);
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
}
