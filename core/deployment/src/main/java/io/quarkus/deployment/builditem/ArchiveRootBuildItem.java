package io.quarkus.deployment.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ArchiveRootBuildItem extends SimpleBuildItem {

    private final Path path;

    public ArchiveRootBuildItem(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }
}
