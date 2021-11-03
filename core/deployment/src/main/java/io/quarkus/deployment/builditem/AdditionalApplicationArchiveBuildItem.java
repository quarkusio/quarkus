package io.quarkus.deployment.builditem;

import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.paths.PathCollection;

/**
 * An additional application archive. This build item can only be consumed, it should not be produced by build steps.
 */
public final class AdditionalApplicationArchiveBuildItem extends MultiBuildItem {

    private final PathCollection path;

    public AdditionalApplicationArchiveBuildItem(PathCollection path) {
        this.path = path;
    }

    /**
     * @deprecated in favor of {@link #getResolvedPaths()}
     * @return
     */
    @Deprecated
    public PathsCollection getPaths() {
        return PathsCollection.from(path);
    }

    public PathCollection getResolvedPaths() {
        return path;
    }
}
