package io.quarkus.paths;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.jar.Manifest;

public class MultiRootPathTree implements OpenPathTree {

    private final PathTree[] trees;
    private final List<Path> roots;

    public MultiRootPathTree(PathTree... trees) {
        this.trees = trees;
        final ArrayList<Path> tmp = new ArrayList<>();
        for (PathTree t : trees) {
            tmp.addAll(t.getRoots());
        }
        tmp.trimToSize();
        roots = tmp;
    }

    @Override
    public Collection<Path> getRoots() {
        return roots;
    }

    @Override
    public Manifest getManifest() {
        for (PathTree tree : trees) {
            final Manifest m = tree.getManifest();
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    @Override
    public void walk(PathVisitor visitor) {
        if (trees.length == 0) {
            return;
        }
        for (PathTree t : trees) {
            t.walk(visitor);
        }
    }

    @Override
    public <T> T processPath(String relativePath, Function<PathVisit, T> func, boolean multiReleaseSupport) {
        for (PathTree tree : trees) {
            T result = tree.processPath(relativePath, func, multiReleaseSupport);
            if (result != null) {
                return result;
            }
        }
        return null;
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
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(trees);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MultiRootPathTree other = (MultiRootPathTree) obj;
        return Arrays.equals(trees, other.trees);
    }
}
