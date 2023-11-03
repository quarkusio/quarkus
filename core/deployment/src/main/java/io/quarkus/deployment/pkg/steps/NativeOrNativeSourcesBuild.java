package io.quarkus.deployment.pkg.steps;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.pkg.PackageConfig;

/**
 * Supplier that can be used to only run build steps in the
 * native or native sources builds.
 * Most build steps that need to be run conditionally should use this instead of {@link NativeBuild}.
 */
public class NativeOrNativeSourcesBuild implements BooleanSupplier {

    private final PackageConfig packageConfig;

    NativeOrNativeSourcesBuild(PackageConfig packageConfig) {
        this.packageConfig = packageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return packageConfig.isNativeOrNativeSources();
    }

}
