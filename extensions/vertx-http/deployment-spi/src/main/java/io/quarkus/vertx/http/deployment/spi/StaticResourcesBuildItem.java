package io.quarkus.vertx.http.deployment.spi;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class StaticResourcesBuildItem extends SimpleBuildItem {

    private final Set<Entry> entries;

    public StaticResourcesBuildItem(Set<Entry> entries) {
        this.entries = entries;
    }

    public Set<Entry> getEntries() {
        return entries;
    }

    public Set<String> getPaths() {
        Set<String> paths = new HashSet<>(entries.size());
        for (Entry entry : entries) {
            paths.add(entry.getPath());
        }
        return paths;
    }

    public static class Entry {
        private final String path;
        private final boolean isDirectory;

        public Entry(String path, boolean isDirectory) {
            this.path = path;
            this.isDirectory = isDirectory;
        }

        public String getPath() {
            return path;
        }

        public boolean isDirectory() {
            return isDirectory;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Entry entry = (Entry) o;
            return isDirectory == entry.isDirectory && path.equals(entry.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, isDirectory);
        }
    }

}
