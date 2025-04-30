package io.quarkus.paths;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

public class EmptyPathTree implements OpenPathTree {

    private static final EmptyPathTree INSTANCE = new EmptyPathTree();

    public static EmptyPathTree getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isArchiveOrigin() {
        return false;
    }

    @Override
    public Collection<Path> getRoots() {
        return Collections.emptyList();
    }

    @Override
    public ManifestAttributes getManifestAttributes() {
        return null;
    }

    @Override
    public void walk(PathVisitor visitor) {
    }

    @Override
    public void walkIfContains(String relativePath, PathVisitor visitor) {
    }

    @Override
    public <T> T apply(String relativePath, Function<PathVisit, T> func) {
        return func.apply(null);
    }

    @Override
    public void accept(String relativePath, Consumer<PathVisit> func) {
        func.accept(null);
    }

    @Override
    public boolean contains(String relativePath) {
        return false;
    }

    @Override
    public Path getPath(String relativePath) {
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
    public String toString() {
        return "<empty>";
    }
}
