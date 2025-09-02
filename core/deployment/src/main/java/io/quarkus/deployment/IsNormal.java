package io.quarkus.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.runtime.LaunchMode;

/**
 * boolean supplier that returns true if the application is running in normal
 * mode. Intended for use with {@link BuildStep#onlyIf()}
 *
 * @deprecated This class was marked as deprecated to raise awareness that the semantic you want is probably provided by
 *             {@link IsProduction}. If you actually need this specific supplier, please open an issue so that we undeprecate
 *             it.
 */
@Deprecated(since = "3.25")
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
