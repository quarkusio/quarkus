package io.quarkus.deployment.builditem;

import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.paths.PathCollection;

/**
 * Represents an additional application archive that must be considered part of the
 * application during the augmentation phase.
 *
 * <p>
 * This build item is produced internally by the Quarkus bootstrap process when
 * extra archives need to be indexed and treated as application code.
 * </p>
 *
 * <p>
 * It is intended to be consumed by build steps that analyze or process
 * application archives. Extensions must not produce this build item directly.
 * </p>
 *
 */
public final class AdditionalApplicationArchiveBuildItem extends MultiBuildItem {
    /**
     * The resolved paths that compose the additional application archive.
     */
    private final PathCollection path;

    /**
     * Creates a new build item representing an additional application archive.
     *
     * @param path the resolved paths that form the archive
     */
    public AdditionalApplicationArchiveBuildItem(PathCollection path) {
        this.path = path;
    }

    /**
     * @deprecated Use {@link #getResolvedPaths()} instead.
     */
    @Deprecated
    public PathsCollection getPaths() {
        return PathsCollection.from(path);
    }

    public PathCollection getResolvedPaths() {
        return path;
    }
}
