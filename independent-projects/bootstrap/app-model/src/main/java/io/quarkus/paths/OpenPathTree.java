package io.quarkus.paths;

import java.io.Closeable;
import java.nio.file.Path;

public interface OpenPathTree extends PathTree, Closeable {

    PathTree getOriginalTree();

    boolean isOpen();

    Path getPath(String relativePath);
}
