package io.quarkus.qute.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item represents a source of template files.
 * <p>
 * By default, the templates are found in the {@code templates} directory. However, an extension can produce this build item to
 * register an additional root path.
 * <p>
 * The path is relative to the artifact/project root and OS-agnostic, i.e. {@code /} is used as a path separator.
 */
public final class TemplateRootBuildItem extends MultiBuildItem {

    private final String path;

    public TemplateRootBuildItem(String path) {
        this.path = normalize(path);
    }

    public String getPath() {
        return path;
    }

    static String normalize(String path) {
        path = path.strip();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

}
