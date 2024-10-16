package io.quarkus.deployment.pkg.steps;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.pkg.NativeConfig;

/**
 * Supplier that can be used to only run build steps in the
 * native sources build.
 *
 * @deprecated In the future, it will be possible to request multiple output types.
 */
@Deprecated
public class NativeSourcesBuild implements BooleanSupplier {

    private final NativeConfig nativeConfig;

    public NativeSourcesBuild(final NativeConfig nativeConfig) {
        this.nativeConfig = nativeConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return nativeConfig.enabled() && nativeConfig.sourcesOnly();
    }
}
