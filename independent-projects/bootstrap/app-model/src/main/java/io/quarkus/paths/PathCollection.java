package io.quarkus.paths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public interface PathCollection extends Iterable<Path> {

    boolean isEmpty();

    int size();

    boolean isSinglePath();

    default Path getSinglePath() {
        if (size() != 1) {
            throw new IllegalStateException("Paths collection expected to contain a single path but contains " + size());
        }
        return iterator().next();
    }

    boolean contains(Path path);

    PathCollection add(Path... paths);

    PathCollection addFirst(Path... paths);

    PathCollection addAllFirst(Iterable<Path> i);

    Path resolveExistingOrNull(String path);

    default Stream<Path> stream() {
        final List<Path> list = new ArrayList<>(size());
        for (Path p : this) {
            list.add(p);
        }
        return list.stream();
    }
}
