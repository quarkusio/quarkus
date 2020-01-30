package io.quarkus.bootstrap.app;

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
    private final Path archivePath;

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
        this.archivePath = archivePath;
        this.hotReloadable = hotReloadable;
        this.forceApplicationArchive = forceApplicationArchive;
    }

    public Path getArchivePath() {
        return archivePath;
    }

    public boolean isHotReloadable() {
        return hotReloadable;
    }

    public boolean isForceApplicationArchive() {
        return forceApplicationArchive;
    }
}
