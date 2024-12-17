package io.quarkus.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.dev.spi.DevModeType;
import io.quarkus.runtime.LaunchMode;

/**
 * Similar to {@link IsDevelopment} except checks whether the application is being launched in dev mode but not from a
 * {@code mutable-jar} package,
 * in other words, not a remote server in a remote dev session.
 */
public class IsLocalDevelopment implements BooleanSupplier {

    private final LaunchMode launchMode;
    private final DevModeType devModeType;

    public IsLocalDevelopment(LaunchMode launchMode, DevModeType devModeType) {
        this.launchMode = launchMode;
        this.devModeType = devModeType;
    }

    @Override
    public boolean getAsBoolean() {
        return launchMode == LaunchMode.DEVELOPMENT && devModeType != DevModeType.REMOTE_SERVER_SIDE;
    }
}
