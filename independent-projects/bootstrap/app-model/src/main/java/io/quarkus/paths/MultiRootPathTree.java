package io.quarkus.paths;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MultiRootPathTree implements OpenPathTree {

    private final PathTree[] trees;
    private final List<Path> roots;
    private transient volatile Set<String> resourceNames;

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
    public void walkRaw(PathVisitor visitor) {
        if (trees.length == 0) {
            return;
        }
        for (PathTree t : trees) {
            t.walkRaw(visitor);
        }
    }

    @Override
    public void walkIfContains(String resourceDirName, PathVisitor visitor) {
        if (trees.length == 0) {
            return;
        }
        for (PathTree t : trees) {
            t.walkIfContains(resourceDirName, visitor);
        }
    }

    @Override
    public Set<String> getResourceNames() {
        return resourceNames == null ? resourceNames = collectResourceNames() : resourceNames;
    }

    private Set<String> collectResourceNames() {
        if (trees.length > 0) {
            Set<String> result = new HashSet<>();
            for (PathTree t : trees) {
                t.walk(visit -> result.add(visit.getResourceName()));
            }
            return result;
        }
        return Set.of();
    }

    @Override
    public <T> T apply(String resourceName, Function<PathVisit, T> func) {
        final AtomicBoolean applied = new AtomicBoolean();
        var wrapper = new Function<PathVisit, T>() {
            @Override
            public T apply(PathVisit pathVisit) {
                if (pathVisit != null) {
                    applied.set(true);
                    return func.apply(pathVisit);
                }
                return null;
            }
        };

        for (PathTree tree : trees) {
            T result = tree.apply(resourceName, wrapper);
            if (applied.get()) {
                return result;
            }
        }
        if (!applied.get()) {
            return func.apply(null);
        }
        return null;
    }

    @Override
    public void accept(String resourceName, Consumer<PathVisit> func) {
        final NonNullVisitConsumer wrapper = new NonNullVisitConsumer(func);
        for (PathTree tree : trees) {
            tree.accept(resourceName, wrapper);
            if (wrapper.accepted) {
                break;
            }
        }
        if (!wrapper.accepted) {
            func.accept(null);
        }
    }

    @Override
    public void acceptAll(String resourceName, Consumer<PathVisit> func) {
        final NonNullVisitConsumer wrapper = new NonNullVisitConsumer(func);
        for (PathTree tree : trees) {
            tree.accept(resourceName, wrapper);
        }
        if (!wrapper.accepted) {
            func.accept(null);
        }
    }

    @Override
    public boolean contains(String resourceName) {
        for (PathTree tree : trees) {
            if (tree.contains(resourceName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Path getPath(String relativePath) {
        for (PathTree tree : trees) {
            if (tree instanceof OpenPathTree openTree) {
                final Path p = openTree.getPath(relativePath);
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
        return roots.stream().map(Path::toString).collect(Collectors.joining(", ", "[", "]"));
    }

    private static class NonNullVisitConsumer implements Consumer<PathVisit> {

        final Consumer<PathVisit> delegate;
        boolean accepted;

        private NonNullVisitConsumer(Consumer<PathVisit> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void accept(PathVisit visit) {
            if (visit != null) {
                accepted = true;
                delegate.accept(visit);
            }
        }
    }
}
