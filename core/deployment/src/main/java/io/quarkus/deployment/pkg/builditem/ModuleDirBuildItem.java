package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ModuleDirBuildItem extends SimpleBuildItem {

    private final Path path;

    public ModuleDirBuildItem(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return this.path;
    }

}
