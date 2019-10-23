package io.quarkus.deployment.pkg.steps;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.pkg.PackageConfig;

/**
 * supplier that can be used to only run build steps in the
 * native build.
 */
public class NativeBuild implements BooleanSupplier {

    private final PackageConfig packageConfig;

    NativeBuild(PackageConfig packageConfig) {
        this.packageConfig = packageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return packageConfig.type.equalsIgnoreCase(PackageConfig.NATIVE);
    }
}
