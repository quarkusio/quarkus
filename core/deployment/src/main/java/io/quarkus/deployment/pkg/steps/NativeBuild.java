package io.quarkus.deployment.pkg.steps;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.pkg.PackageConfig;

/**
 * Supplier that can be used to only run build steps in the
 * native build.
 *
 * WARNING: In most cases extensions will want to use {@link NativeOrNativeSourcesBuild} to ensure that
 * the extension works properly when the build produces a {@code native-sources} artifact instead of a
 * native binary.
 * This build item should be used only when there is a real need for a step to run exclusively for a {@code native} build.
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
