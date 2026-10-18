package io.quarkus.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.runtime.LaunchMode;

/**
 * boolean supplier that returns true if the application is running in production
 * mode i.e. either NORMAL or RUN. Intended for use with {@link BuildStep#onlyIf()}
 */
public class IsProduction implements BooleanSupplier {

    private final LaunchMode launchMode;

    public IsProduction(LaunchMode launchMode) {
        this.launchMode = launchMode;
    }

    @Override
    public boolean getAsBoolean() {
        return launchMode == LaunchMode.NORMAL || launchMode == LaunchMode.RUN;
    }
}
