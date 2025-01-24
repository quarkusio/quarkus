package io.quarkus.paths;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class FilteredPathTree implements PathTree {

    private final PathTree original;
    protected final PathFilter filter;

    public FilteredPathTree(PathTree tree, PathFilter filter) {
        this.original = Objects.requireNonNull(tree, "tree is null");
        this.filter = Objects.requireNonNull(filter, "filter is null");
    }

    @Override
    public boolean isArchiveOrigin() {
        return original.isArchiveOrigin();
    }

    @Override
    public Collection<Path> getRoots() {
        return original.getRoots();
    }

    @Override
    public ManifestAttributes getManifestAttributes() {
        return original.getManifestAttributes();
    }

    @Override
    public void walk(PathVisitor visitor) {
        original.walk(visit -> {
            if (visit != null && filter.isVisible(visit.getRelativePath("/"))) {
                visitor.visitPath(visit);
            }
        });
    }

    @Override
    public void walkIfContains(String relativePath, PathVisitor visitor) {
        if (!PathFilter.isVisible(filter, relativePath)) {
            return;
        }
        original.walkIfContains(relativePath, visit -> {
            if (visit != null && filter.isVisible(visit.getRelativePath())) {
                visitor.visitPath(visit);
            }
        });
    }

    @Override
    public <T> T apply(String relativePath, Function<PathVisit, T> func) {
        if (!PathFilter.isVisible(filter, relativePath)) {
            return func.apply(null);
        }
        return original.apply(relativePath, func);
    }

    @Override
    public void accept(String relativePath, Consumer<PathVisit> consumer) {
        if (!PathFilter.isVisible(filter, relativePath)) {
            consumer.accept(null);
        } else {
            original.accept(relativePath, consumer);
        }
    }

    @Override
    public void acceptAll(String relativePath, Consumer<PathVisit> consumer) {
        if (!PathFilter.isVisible(filter, relativePath)) {
            consumer.accept(null);
        } else {
            original.acceptAll(relativePath, consumer);
        }
    }

    @Override
    public boolean contains(String relativePath) {
        return PathFilter.isVisible(filter, relativePath) && original.contains(relativePath);
    }

    @Override
    public OpenPathTree open() {
        return new OpenFilteredPathTree(original.open(), filter);
    }

    @Override
    public String toString() {
        return original.toString() + " (filtered)";
    }

    private static class OpenFilteredPathTree extends FilteredPathTree implements OpenPathTree {

        private final OpenPathTree original;

        private OpenFilteredPathTree(OpenPathTree original, PathFilter filter) {
            super(original, filter);
            this.original = original;
        }

        @Override
        public PathTree getOriginalTree() {
            return original.getOriginalTree();
        }

        @Override
        public boolean isOpen() {
            return original.isOpen();
        }

        @Override
        public Path getPath(String relativePath) {
            if (!PathFilter.isVisible(filter, relativePath)) {
                return null;
            }
            return original.getPath(relativePath);
        }

        @Override
        public void close() throws IOException {
            original.close();
        }
    }
}
