package io.quarkus.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.runtime.LaunchMode;

/**
 * boolean supplier that returns true if live reload is enabled.
 * Intended for use with {@link BuildStep#onlyIf()}
 */
public class IsLiveReloadSupportedByLaunchMode implements BooleanSupplier {

    private final LaunchMode launchMode;

    public IsLiveReloadSupportedByLaunchMode(LaunchMode launchMode) {
        this.launchMode = launchMode;
    }

    @Override
    public boolean getAsBoolean() {
        return launchMode.isLiveReloadSupported();
    }
}
