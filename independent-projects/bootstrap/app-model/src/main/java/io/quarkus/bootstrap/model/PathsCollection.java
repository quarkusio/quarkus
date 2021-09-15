package io.quarkus.bootstrap.model;

import io.quarkus.paths.PathCollection;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PathsCollection implements PathCollection, Serializable {

    public static PathsCollection from(Iterable<Path> paths) {
        final List<Path> list = new ArrayList<>();
        paths.forEach(list::add);
        return new PathsCollection(list);
    }

    public static PathsCollection of(Path... paths) {
        return new PathsCollection(Arrays.asList(paths));
    }

    public static class Builder {
        private List<Path> paths = new ArrayList<>();

        private Builder() {
        }

        public Builder add(Path path) {
            paths.add(path);
            return this;
        }

        public boolean contains(Path p) {
            return paths.contains(p);
        }

        public PathsCollection build() {
            return new PathsCollection(paths);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private List<Path> paths;

    private PathsCollection(List<Path> paths) {
        this.paths = Collections.unmodifiableList(paths);
    }

    @Override
    public boolean isEmpty() {
        return paths.isEmpty();
    }

    @Override
    public int size() {
        return paths.size();
    }

    @Override
    public boolean isSinglePath() {
        return paths.size() == 1;
    }

    @Override
    public Iterator<Path> iterator() {
        return paths.iterator();
    }

    @Override
    public boolean contains(Path path) {
        return paths.contains(path);
    }

    @Override
    public PathsCollection add(Path... paths) {
        final List<Path> list = new ArrayList<>(this.paths.size() + paths.length);
        list.addAll(this.paths);
        for (int i = 0; i < paths.length; ++i) {
            list.add(paths[i]);
        }
        return new PathsCollection(list);
    }

    @Override
    public PathsCollection addFirst(Path... paths) {
        final List<Path> list = new ArrayList<>(this.paths.size() + paths.length);
        for (int i = 0; i < paths.length; ++i) {
            list.add(paths[i]);
        }
        list.addAll(this.paths);
        return new PathsCollection(list);
    }

    @Override
    public PathsCollection addAllFirst(Iterable<Path> i) {
        final List<Path> list = new ArrayList<>();
        i.forEach(list::add);
        paths.forEach(list::add);
        return new PathsCollection(list);
    }

    @Override
    public Path resolveExistingOrNull(String path) {
        for (Path p : paths) {
            final Path resolved = p.resolve(path);
            if (Files.exists(resolved)) {
                return resolved;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("[paths: ");
        forEach(p -> buf.append(p).append(';'));
        return buf.append(']').toString();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeInt(paths.size());
        for (Path p : paths) {
            out.writeUTF(p.toAbsolutePath().toString());
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        final int pathsTotal = in.readInt();
        List<Path> paths = new ArrayList<>(pathsTotal);
        for (int i = 0; i < pathsTotal; ++i) {
            paths.add(Paths.get(in.readUTF()));
        }
        this.paths = Collections.unmodifiableList(paths);
    }

    public Collection<Path> toList() {
        return new ArrayList<>(paths);
    }
}
