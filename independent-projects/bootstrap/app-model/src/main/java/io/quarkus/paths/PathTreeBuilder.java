package io.quarkus.paths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PathTreeBuilder {

    private boolean archive;
    private Boolean manifestEnabled;
    private Path root;
    private List<String> includes;
    private List<String> excludes;

    public PathTreeBuilder() {
    }

    public PathTreeBuilder setArchive(boolean archive) {
        this.archive = archive;
        return this;
    }

    public PathTreeBuilder setManifestEnabled(boolean manifestEnabled) {
        this.manifestEnabled = manifestEnabled;
        return this;
    }

    public PathTreeBuilder setRoot(Path root) {
        this.root = root;
        return this;
    }

    Path getRoot() {
        return root;
    }

    public PathTreeBuilder include(String expr) {
        if (includes == null) {
            includes = new ArrayList<>(1);
        }
        includes.add(expr);
        return this;
    }

    List<String> getIncludes() {
        return includes;
    }

    public PathTreeBuilder exclude(String expr) {
        if (excludes == null) {
            excludes = new ArrayList<>(1);
        }
        excludes.add(expr);
        return this;
    }

    List<String> getExcludes() {
        return includes;
    }

    PathFilter getPathFilter() {
        return includes == null && excludes == null ? null : new PathFilter(includes, excludes);
    }

    public PathTree build() {
        if (root == null) {
            throw new RuntimeException("The tree root has not been provided");
        }
        if (!Files.exists(root)) {
            throw new IllegalArgumentException(root + " does not exist");
        }
        if (archive) {
            return ArchivePathTree.forPath(root, getPathFilter(), manifestEnabled == null ? true : manifestEnabled);
        }
        if (Files.isDirectory(root)) {
            return new DirectoryPathTree(root, getPathFilter(), manifestEnabled == null ? false : manifestEnabled);
        }
        return new FilePathTree(root, getPathFilter());
    }
}
