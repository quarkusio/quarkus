package io.quarkus.deployment.pkg;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.pkg.PackageConfig.JarConfig.JarType;

public class AotJarEnabled implements BooleanSupplier {

    private final PackageConfig packageConfig;

    AotJarEnabled(PackageConfig packageConfig) {
        this.packageConfig = packageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return packageConfig.jar().enabled() &&
                packageConfig.jar().type() == JarType.AOT_JAR;
    }
}
