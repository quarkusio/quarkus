package io.quarkus.deployment.pkg.steps;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.pkg.PackageConfig;

/**
 * Supplier that can be used to only run build steps in the
 * native sources build.
 */
public class NativeSourcesBuild implements BooleanSupplier {

    private final PackageConfig packageConfig;

    NativeSourcesBuild(PackageConfig packageConfig) {
        this.packageConfig = packageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return packageConfig.type.equalsIgnoreCase(PackageConfig.BuiltInType.NATIVE_SOURCES.getValue());
    }
}
