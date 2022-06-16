package io.quarkus.deployment.pkg.steps;

import java.util.function.BooleanSupplier;

import org.graalvm.home.Version;

import io.quarkus.deployment.pkg.PackageConfig;

/**
 * Supplier that can be used to only run build steps in the
 * native or native sources builds when using Graal >= 22.2.
 * Most build steps that need to be run conditionally should use this instead of {@link NativeBuild}.
 */
public class NativeOrNativeSourcesBuildGraal22_2OrLater implements BooleanSupplier {

    private final PackageConfig packageConfig;

    NativeOrNativeSourcesBuildGraal22_2OrLater(PackageConfig packageConfig) {
        this.packageConfig = packageConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return (packageConfig.type.equalsIgnoreCase(PackageConfig.NATIVE)
                || packageConfig.type.equalsIgnoreCase(PackageConfig.NATIVE_SOURCES))
                && (Version.getCurrent().isSnapshot() || Version.getCurrent().compareTo(22, 2) >= 0);
    }

}
