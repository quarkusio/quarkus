package io.quarkus.paths;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.jar.Manifest;

public class EmptyPathTree implements OpenPathTree {

    private static final EmptyPathTree INSTANCE = new EmptyPathTree();

    public static EmptyPathTree getInstance() {
        return INSTANCE;
    }

    @Override
    public Collection<Path> getRoots() {
        return Collections.emptyList();
    }

    @Override
    public Manifest getManifest() {
        return null;
    }

    @Override
    public void walk(PathVisitor visitor) {
    }

    @Override
    public <T> T processPath(String relativePath, Function<PathVisit, T> func, boolean multiReleaseSupport) {
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
}
