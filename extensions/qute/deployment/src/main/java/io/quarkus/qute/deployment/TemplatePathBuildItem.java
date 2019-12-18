package io.quarkus.qute.deployment;

import java.nio.file.Path;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a template path.
 */
public final class TemplatePathBuildItem extends MultiBuildItem {

    private final String path;
    private final Path fullPath;
    private final boolean tag;

    public TemplatePathBuildItem(String path, Path fullPath, boolean tag) {
        this.path = path;
        this.fullPath = fullPath;
        this.tag = tag;
    }

    /**
     * Uses the {@code /} path separator.
     * 
     * @return the path relative to the template root
     */
    public String getPath() {
        return path;
    }

    /**
     * Uses the system-dependent path separator.
     * 
     * @return the full path of the template
     */
    public Path getFullPath() {
        return fullPath;
    }

    /**
     * 
     * @return {@code true} if it represents a user tag, {@code false} otherwise
     */
    public boolean isTag() {
        return tag;
    }

    public boolean isRegular() {
        return !isTag();
    }

}
