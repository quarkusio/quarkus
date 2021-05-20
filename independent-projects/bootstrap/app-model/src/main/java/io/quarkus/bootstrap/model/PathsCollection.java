package io.quarkus.bootstrap.model;

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

/**
 * A specific collection meant to be used to manipulate {@code Path}s.
 */
public interface PathsCollection extends Iterable<Path> {

    /**
     * @return {@code true} if the collection is empty, {@code false} otherwise.
     */
    boolean isEmpty();

    /**
     * @return the size of the collection id paths.
     */
    int size();

    /**
     * @return {@code true} if the collection contains one path. {@code false} otherwise.
     */
    boolean isSinglePath();

    /**
     * @return the only path that could be found into the collection.
     * @throws IllegalStateException if there is no path or there are more than one paths into the collection.
     */
    Path getSinglePath();

    /**
     * @param path the path to check.
     * @return {@code true} if the collection contains the given path, {@code false} otherwise.
     */
    boolean contains(Path path);

    /**
     * Appends the given paths to the collection.
     * 
     * @param paths the paths to append.
     * @return a new collection with the appended paths.
     */
    PathsCollection add(Path... paths);

    /**
     * Adds the given paths at the beginning of the collection
     * 
     * @param paths the paths to add.
     * @return a new collection with the added paths.
     */
    PathsCollection addFirst(Path... paths);

    /**
     * Adds the given paths at the beginning of the collection
     * 
     * @param paths the paths to add.
     * @return a new collection with the added paths.
     */
    PathsCollection addAllFirst(Iterable<Path> paths);

    /**
     * Gives a path of the collection for which a file could found at the given relative location.
     * 
     * @param path the relative location for which we want to find a matching root path.
     * @return the root path of the collection that could match with the given location, {@code null} otherwise.
     */
    Path resolveExistingOrNull(String path);

    /**
     * @return the content of the collection as a {@link List}.
     */
    Collection<Path> toList();

    static PathsCollection from(Iterable<Path> paths) {
        final List<Path> list = new ArrayList<>();
        paths.forEach(list::add);
        return new Default(list);
    }

    static PathsCollection of(Path... paths) {
        return new Default(Arrays.asList(paths));
    }

    class Builder {
        private final List<Path> paths = new ArrayList<>();

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
            return new Default(paths);
        }
    }

    static Builder builder() {
        return new Builder();
    }

    class Default implements PathsCollection, Serializable {
        private List<Path> paths;

        private Default(List<Path> paths) {
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
        public Path getSinglePath() {
            if (paths.size() != 1) {
                throw new IllegalStateException(
                        "Paths collection expected to contain a single path but contains " + paths.size());
            }
            return paths.get(0);
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
            list.addAll(Arrays.asList(paths));
            return new Default(list);
        }

        @Override
        public PathsCollection addFirst(Path... paths) {
            final List<Path> list = new ArrayList<>(this.paths.size() + paths.length);
            list.addAll(Arrays.asList(paths));
            list.addAll(this.paths);
            return new Default(list);
        }

        @Override
        public PathsCollection addAllFirst(Iterable<Path> i) {
            final List<Path> list = new ArrayList<>();
            i.forEach(list::add);
            list.addAll(paths);
            return new Default(list);
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
        public Collection<Path> toList() {
            return new ArrayList<>(paths);
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
    }
}
