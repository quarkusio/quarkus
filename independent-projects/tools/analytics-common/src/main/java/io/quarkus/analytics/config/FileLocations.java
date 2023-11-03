package io.quarkus.analytics.config;

import java.nio.file.Path;

/**
 * File location paths
 */
public interface FileLocations {
    /**
     * Returns the folder where all the build time analytics files are stored.
     *
     * @return
     */
    Path getFolder();

    /**
     * Returns the file where the user's UUID is stored.
     *
     * @return
     */
    Path getUUIDFile();

    /**
     * Returns the file where the build time analytics config is stored.
     *
     * @return
     */
    Path getRemoteConfigFile();

    /**
     * Returns the file where the last time the remote config was retrieved and stored.
     *
     * @return
     */
    Path getLastRemoteConfigTryFile();

    Path getLocalConfigFile();

    String lastTrackFileName();
}
