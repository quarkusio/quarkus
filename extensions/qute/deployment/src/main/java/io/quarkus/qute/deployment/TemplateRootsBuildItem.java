package io.quarkus.qute.deployment;

import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The set of template root paths.
 */
public final class TemplateRootsBuildItem extends SimpleBuildItem implements Iterable<String> {

    private Set<String> rootPaths;

    public TemplateRootsBuildItem(Set<String> paths) {
        this.rootPaths = paths;
    }

    public Set<String> getPaths() {
        return rootPaths;
    }

    @Override
    public Iterator<String> iterator() {
        return rootPaths.iterator();
    }

    /**
     * The path must be relative to the resource root.
     *
     * @param path
     *
     * @return {@code true} is the given path represents a template root, {@code false} otherwise
     */
    public boolean isRoot(Path path) {
        String pathStr = normalize(path);
        for (String rootPath : rootPaths) {
            if (pathStr.equals(rootPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The path must be relative to the resource root.
     *
     * @param path
     *
     * @return {@code true} is the given path may represent a template root, {@code false} otherwise
     */
    public boolean maybeRoot(Path path) {
        String pathStr = normalize(path);
        for (String rootPath : rootPaths) {
            if ((rootPath.contains("/") && rootPath.startsWith(pathStr)) || rootPath.equals(pathStr)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(Path path) {
        String pathStr = path.toString();
        if (File.separatorChar != '/') {
            // \foo\bar\templates -> /foo/bar/templates
            pathStr = pathStr.replace(File.separatorChar, '/');
        }
        return TemplateRootBuildItem.normalize(pathStr);
    }

}
