package io.quarkus.paths;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /**
     * If at least one of the PathTrees is not an archive, we return false.
     */
    @Override
    public boolean isArchiveOrigin() {
        for (PathTree tree : trees) {
            if (!tree.isArchiveOrigin()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Collection<Path> getRoots() {
        return roots;
    }

    @Override
    public ManifestAttributes getManifestAttributes() {
        for (PathTree tree : trees) {
            final ManifestAttributes m = tree.getManifestAttributes();
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
    public void walkIfContains(String relativePath, PathVisitor visitor) {
        if (trees.length == 0) {
            return;
        }
        for (PathTree t : trees) {
            t.walkIfContains(relativePath, visitor);
        }
    }

    @Override
    public <T> T apply(String relativePath, Function<PathVisit, T> func) {
        for (PathTree tree : trees) {
            T result = tree.apply(relativePath, func);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public void accept(String relativePath, Consumer<PathVisit> func) {
        final AtomicBoolean consumed = new AtomicBoolean();
        final Consumer<PathVisit> wrapper = new Consumer<>() {
            @Override
            public void accept(PathVisit t) {
                if (t != null) {
                    func.accept(t);
                    consumed.set(true);
                }
            }
        };
        for (PathTree tree : trees) {
            tree.accept(relativePath, wrapper);
            if (consumed.get()) {
                break;
            }
        }
        if (!consumed.get()) {
            func.accept(null);
        }
    }

    @Override
    public void acceptAll(String relativePath, Consumer<PathVisit> func) {
        final AtomicBoolean consumed = new AtomicBoolean();
        final Consumer<PathVisit> wrapper = new Consumer<>() {
            @Override
            public void accept(PathVisit t) {
                if (t != null) {
                    func.accept(t);
                    consumed.set(true);
                }
            }
        };
        for (PathTree tree : trees) {
            tree.accept(relativePath, wrapper);
        }
        if (!consumed.get()) {
            func.accept(null);
        }
    }

    @Override
    public boolean contains(String relativePath) {
        for (PathTree tree : trees) {
            if (tree.contains(relativePath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Path getPath(String relativePath) {
        for (PathTree tree : trees) {
            if (tree instanceof OpenPathTree) {
                final Path p = ((OpenPathTree) tree).getPath(relativePath);
                if (p != null) {
                    return p;
                }
            } else {
                throw new UnsupportedOperationException();
            }
        }
        return null;
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

    @Override
    public String toString() {
        return roots.stream().map(p -> p.toString()).collect(Collectors.joining(", ", "[", "]"));
    }
}
