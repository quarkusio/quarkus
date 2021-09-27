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

    private final boolean auxiliaryApplication;
    private final Optional<DevModeType> auxiliaryDevModeType;

    private final boolean test;

    public LaunchModeBuildItem(LaunchMode launchMode, Optional<DevModeType> devModeType, boolean auxiliaryApplication,
            Optional<DevModeType> auxiliaryDevModeType, boolean test) {
        this.launchMode = launchMode;
        this.devModeType = devModeType;
        this.auxiliaryApplication = auxiliaryApplication;
        this.auxiliaryDevModeType = auxiliaryDevModeType;
        this.test = test;
    }

    public LaunchMode getLaunchMode() {
        return launchMode;
    }

    /**
     * The development mode type.
     * <p>
     * Note that even for NORMAL launch modes this could be generating an application for the local side of remote
     * dev mode, so this may be set even for launch mode normal.
     */
    public Optional<DevModeType> getDevModeType() {
        return devModeType;
    }

    /**
     * An Auxiliary Application is a second application running in the same JVM as a primary application.
     * <p>
     * Currently this is done to allow running tests in dev mode, while the main dev mode process continues to
     * run.
     */
    public boolean isAuxiliaryApplication() {
        return auxiliaryApplication;
    }

    /**
     * The dev mode type of the main application.
     *
     */
    public Optional<DevModeType> getAuxiliaryDevModeType() {
        return auxiliaryDevModeType;
    }

    /**
     * If this is a test. Dev mode tests don't launch with a launch mode TEST, so this
     * can be used to determine if we are in a dev mode test.
     */
    public boolean isTest() {
        return test;
    }
}
