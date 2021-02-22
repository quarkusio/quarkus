package io.quarkus.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.runtime.LaunchMode;

/**
 * boolean supplier that returns true if the application is running in normal
 * mode. Intended for use with {@link BuildStep#onlyIf()}
 */
public class IsNormal implements BooleanSupplier {

    private final LaunchMode launchMode;

    public IsNormal(LaunchMode launchMode) {
        this.launchMode = launchMode;
    }

    @Override
    public boolean getAsBoolean() {
        return launchMode == LaunchMode.NORMAL;
    }
}
