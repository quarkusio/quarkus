package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A jar that is build to run the native image
 */
public final class NativeImageSourceJarBuildItem extends SimpleBuildItem {

    private final Path path;
    private final Path libraryDir;

    public NativeImageSourceJarBuildItem(Path path, Path libraryDir) {
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
