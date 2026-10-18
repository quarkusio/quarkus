package io.quarkus.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.runtime.LaunchMode;

/**
 * boolean supplier that returns true if Dev Resources are enabled.
 * Intended for use with {@link BuildStep#onlyIf()}
 */
public class IsDevResourcesSupportedByLaunchMode implements BooleanSupplier {

    private final LaunchMode launchMode;

    public IsDevResourcesSupportedByLaunchMode(LaunchMode launchMode) {
        this.launchMode = launchMode;
    }

    @Override
    public boolean getAsBoolean() {
        return launchMode.isDevResourcesSupported();
    }
}
