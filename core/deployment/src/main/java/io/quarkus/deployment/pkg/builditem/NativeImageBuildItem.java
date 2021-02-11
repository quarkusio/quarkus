package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

public final class NativeImageBuildItem extends SimpleBuildItem {

    private final Path path;

    public NativeImageBuildItem(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }
}
