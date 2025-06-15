package io.quarkus.analytics.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File location paths used in production code
 */
public class FileLocationsImpl implements FileLocations {

    public static final FileLocations INSTANCE = new FileLocationsImpl();

    private static final Path RED_HAT = Paths.get(
            System.getProperty("user.home"),
            ".redhat");

    private static final Path UUID_FILE = RED_HAT.resolve("anonymousId");
    private static final Path REMOTE_CONFIG_FILE = RED_HAT.resolve("io.quarkus.analytics.remoteconfig");
    private static final Path LAST_REMOTE_CONFIG_TRY_FILE = RED_HAT.resolve(
            "io.quarkus.analytics.lasttry");
    private static final Path LOCAL_CONFIG_FILE = RED_HAT.resolve("io.quarkus.analytics.localconfig");
    private static final String BUILD_ANALYTICS_EVENT_FILE_NAME = "build-analytics-event.json";

    // singleton
    private FileLocationsImpl() {
        // not much
    }

    @Override
    public Path getFolder() {
        return RED_HAT;
    }

    @Override
    public Path getUUIDFile() {
        return UUID_FILE;
    }

    @Override
    public Path getRemoteConfigFile() {
        return REMOTE_CONFIG_FILE;
    }

    @Override
    public Path getLastRemoteConfigTryFile() {
        return LAST_REMOTE_CONFIG_TRY_FILE;
    }

    @Override
    public Path getLocalConfigFile() {
        return LOCAL_CONFIG_FILE;
    }

    @Override
    public String lastTrackFileName() {
        return BUILD_ANALYTICS_EVENT_FILE_NAME;
    }
}
