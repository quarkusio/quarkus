package io.quarkus.qute.deployment;

import java.nio.file.Path;

import io.quarkus.builder.item.MultiBuildItem;

public final class TemplatePathBuildItem extends MultiBuildItem {

    private final String path;
    private final Path fullPath;
    private final boolean tag;

    public TemplatePathBuildItem(String path, Path fullPath) {
        this(path, fullPath, false);
    }

    public TemplatePathBuildItem(String path, Path fullPath, boolean tag) {
        this.path = path;
        this.fullPath = fullPath;
        this.tag = tag;
    }

    public String getPath() {
        return path;
    }

    public Path getFullPath() {
        return fullPath;
    }

    public boolean isTag() {
        return tag;
    }

    public boolean isRegular() {
        return !isTag();
    }

}
