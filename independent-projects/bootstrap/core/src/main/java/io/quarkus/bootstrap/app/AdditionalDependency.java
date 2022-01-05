package io.quarkus.bootstrap.app;

import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;
import java.io.Serializable;
import java.nio.file.Path;

/**
 * An additional archive that should be added to the generated application.
 *
 * This is generally only used in dev and test mode, where additional
 * paths from the current project should be added to the current application.
 *
 * For production applications this should not be needed as the full set of
 * dependencies should already be available.
 */
public class AdditionalDependency implements Serializable {

    /**
     * The path to the application archive
     */
    private final PathCollection paths;

    /**
     * If this archive is hot reloadable, only takes effect in dev mode.
     */
    private final boolean hotReloadable;

    /**
     * If this is true then this will force this dependency to be an application archive, even if it would not
     * otherwise be one. This means it will be indexed so components can be discovered from the location.
     */
    private final boolean forceApplicationArchive;

    public AdditionalDependency(Path archivePath, boolean hotReloadable, boolean forceApplicationArchive) {
        this(PathList.of(archivePath), hotReloadable, forceApplicationArchive);
    }

    /**
     * @deprecated in favor of {@link #AdditionalDependency(PathCollection, boolean, boolean)}
     * @param archivePath archive paths
     * @param hotReloadable whether the dependency is reloadable
     * @param forceApplicationArchive whether it should be added as an application archive
     */
    @Deprecated
    public AdditionalDependency(PathsCollection archivePath, boolean hotReloadable, boolean forceApplicationArchive) {
        this(PathList.from(archivePath), hotReloadable, forceApplicationArchive);
    }

    public AdditionalDependency(PathCollection paths, boolean hotReloadable, boolean forceApplicationArchive) {
        this.paths = paths;
        this.hotReloadable = hotReloadable;
        this.forceApplicationArchive = forceApplicationArchive;
    }

    /**
     * @deprecated in favor of {@link #getResolvedPaths()}
     * @return archive paths
     */
    @Deprecated
    public PathsCollection getArchivePath() {
        return PathsCollection.from(paths);
    }

    public PathCollection getResolvedPaths() {
        return paths;
    }

    public boolean isHotReloadable() {
        return hotReloadable;
    }

    public boolean isForceApplicationArchive() {
        return forceApplicationArchive;
    }
}
