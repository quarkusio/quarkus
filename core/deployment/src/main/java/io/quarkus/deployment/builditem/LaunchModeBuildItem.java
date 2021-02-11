package io.quarkus.deployment.builditem;

import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.runtime.LaunchMode;

/**
 * indicates the type of launch
 */
public final class LaunchModeBuildItem extends SimpleBuildItem {

    private final LaunchMode launchMode;

    private final Optional<DevModeType> devModeType;

    public LaunchModeBuildItem(LaunchMode launchMode, Optional<DevModeType> devModeType) {
        this.launchMode = launchMode;
        this.devModeType = devModeType;
    }

    public LaunchMode getLaunchMode() {
        return launchMode;
    }

    /**
     * The development mode type.
     *
     * Note that even for NORMAL launch modes this could be generating an application for the local side of remote
     * dev mode, so this may be set even for launch mode normal.
     */
    public Optional<DevModeType> getDevModeType() {
        return devModeType;
    }
}
