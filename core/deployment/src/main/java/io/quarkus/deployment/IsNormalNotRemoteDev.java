package io.quarkus.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.runtime.LaunchMode;

/**
 * boolean supplier that returns true if the application is running in normal
 * mode, but is not a remote dev client. Intended for use with {@link BuildStep#onlyIf()}
 */
public class IsNormalNotRemoteDev implements BooleanSupplier {

    private final LaunchMode launchMode;
    private final DevModeType devModeType;

    public IsNormalNotRemoteDev(LaunchMode launchMode, DevModeType devModeType) {
        this.launchMode = launchMode;
        this.devModeType = devModeType;
    }

    @Override
    public boolean getAsBoolean() {
        return launchMode == LaunchMode.NORMAL && devModeType == null;
    }
}
