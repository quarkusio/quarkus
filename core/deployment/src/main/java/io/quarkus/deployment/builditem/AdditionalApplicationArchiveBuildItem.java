package io.quarkus.deployment.builditem;

import java.nio.file.Path;

import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * An additional application archive
 */
public final class AdditionalApplicationArchiveBuildItem extends MultiBuildItem {

    private final PathsCollection path;

    public AdditionalApplicationArchiveBuildItem(PathsCollection path) {
        this.path = path;
    }

    @Deprecated
    public Path getPath() {
        return path.getSinglePath();
    }

    public PathsCollection getPaths() {
        return path;
    }
}
