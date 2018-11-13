package org.jboss.shamrock.deployment.builditem;

import java.nio.file.Path;

import org.jboss.builder.item.SimpleBuildItem;

public final class ArchiveRootBuildItem extends SimpleBuildItem {

    private final Path path;

    public ArchiveRootBuildItem(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }
}
