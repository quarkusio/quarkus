package io.quarkus.deployment.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * An additional application archive
 */
public final class AdditionalApplicationArchiveBuildItem extends MultiBuildItem {

    private final Path path;

    public AdditionalApplicationArchiveBuildItem(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }
}
