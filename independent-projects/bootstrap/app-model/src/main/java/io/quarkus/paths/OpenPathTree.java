package io.quarkus.paths;

import java.io.Closeable;

public interface OpenPathTree extends PathTree, Closeable {

    PathTree getOriginalTree();
}
