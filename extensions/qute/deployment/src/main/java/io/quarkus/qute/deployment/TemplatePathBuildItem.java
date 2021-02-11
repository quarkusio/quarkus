package io.quarkus.qute.deployment;

import java.nio.file.Path;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a template path.
 */
public final class TemplatePathBuildItem extends MultiBuildItem {

    static final String TAGS = "tags/";

    private final String path;
    private final Path fullPath;

    public TemplatePathBuildItem(String path, Path fullPath) {
        this.path = path;
        this.fullPath = fullPath;
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
        return path.startsWith(TAGS);
    }

    public boolean isRegular() {
        return !isTag();
    }

}
