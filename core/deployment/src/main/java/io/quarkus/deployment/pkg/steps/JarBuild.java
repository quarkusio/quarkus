package io.quarkus.deployment.pkg.steps;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.pkg.PackageConfig;

/**
 * supplier that can be used to only run build steps in the
 * regular jar build.
 */
public class JarBuild implements BooleanSupplier {

    private final PackageConfig packageConfig;

    JarBuild(PackageConfig packageConfig) {
        this.packageConfig = packageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return packageConfig.type.equalsIgnoreCase(PackageConfig.JAR);
    }
}
