package io.quarkus.deployment.pkg.steps;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.pkg.NativeConfig;

/**
 * Supplier that can be used to only run build steps in the
 * native or native sources builds.
 * Most build steps that need to be run conditionally should use this instead of {@link NativeBuild}.
 *
 * @deprecated In the future, it will be possible to request multiple output types.
 */
@Deprecated
public class NativeOrNativeSourcesBuild implements BooleanSupplier {

    private final NativeConfig nativeConfig;

    public NativeOrNativeSourcesBuild(final NativeConfig nativeConfig) {
        this.nativeConfig = nativeConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return nativeConfig.enabled();
    }
}
