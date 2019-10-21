package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ThinJarBuildItem extends SimpleBuildItem {

    private final Path path;
    private final Path libraryDir;

    public ThinJarBuildItem(Path path, Path libraryDir) {
        this.path = path;
        this.libraryDir = libraryDir;
    }

    public Path getPath() {
        return path;
    }

    public Path getLibraryDir() {
        return libraryDir;
    }
}
