package io.quarkus.bootstrap.workspace;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import io.quarkus.paths.DirectoryPathTree;
import io.quarkus.paths.PathTree;

public class DefaultSourceDir implements SourceDir, Serializable {

    private static final long serialVersionUID = 6544177650615687691L;
    private final PathTree srcTree;
    private final PathTree outputTree;
    private final PathTree generatedSourcesTree;
    private final Map<Object, Object> data;

    public DefaultSourceDir(Path srcDir, Path destinationDir, Path generatedSourcesDir) {
        this(srcDir, destinationDir, generatedSourcesDir, Collections.emptyMap());
    }

    public DefaultSourceDir(Path srcDir, Path destinationDir, Path generatedSourcesDir, Map<Object, Object> data) {
        this(new DirectoryPathTree(srcDir), new DirectoryPathTree(destinationDir),
                generatedSourcesDir != null ? new DirectoryPathTree(generatedSourcesDir) : null,
                data);
    }

    public DefaultSourceDir(PathTree srcTree, PathTree outputTree, PathTree generatedSourcesTree, Map<Object, Object> data) {
        this.srcTree = srcTree;
        this.outputTree = outputTree;
        this.generatedSourcesTree = generatedSourcesTree;
        this.data = data;
    }

    @Override
    public Path getDir() {
        return srcTree.getRoots().iterator().next();
    }

    @Override
    public PathTree getSourceTree() {
        return srcTree;
    }

    @Override
    public Path getOutputDir() {
        return outputTree.getRoots().iterator().next();
    }

    @Override
    public Path getAptSourcesDir() {
        return generatedSourcesTree != null ? generatedSourcesTree.getRoots().iterator().next() : null;
    }

    @Override
    public PathTree getOutputTree() {
        return outputTree;
    }

    public <T> T getValue(Object key, Class<T> type) {
        final Object o = data.get(key);
        return o == null ? null : type.cast(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, outputTree, srcTree, generatedSourcesTree);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultSourceDir other = (DefaultSourceDir) obj;
        return Objects.equals(data, other.data) && Objects.equals(outputTree, other.outputTree)
                && Objects.equals(srcTree, other.srcTree)
                && Objects.equals(generatedSourcesTree, other.generatedSourcesTree);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(srcTree.getRoots()).append(" -> ").append(outputTree.getRoots());
        buf.append(" generated sources: ").append(generatedSourcesTree != null ? generatedSourcesTree.getRoots() : null);
        if (!data.isEmpty()) {
            final Iterator<Map.Entry<Object, Object>> i = data.entrySet().iterator();
            Map.Entry<Object, Object> e = i.next();
            buf.append(" ").append(e.getKey()).append("=").append(e.getValue());
            while (i.hasNext()) {
                e = i.next();
                buf.append(",").append(e.getKey()).append("=").append(e.getValue());
            }
        }
        return buf.toString();
    }
}
