package io.quarkus.deployment.pkg.steps;

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.pkg.NativeConfig;

/**
 * Supplier that can be used to only run build steps in the
 * native build.
 *
 * WARNING: In most cases extensions will want to use {@link NativeOrNativeSourcesBuild} to ensure that
 * the extension works properly when the build produces a {@code native-sources} artifact instead of a
 * native binary.
 * This build item should be used only when there is a real need for a step to run exclusively for a {@code native} build.
 *
 * @deprecated In the future, it will be possible to request multiple output types.
 */
@Deprecated
public class NativeBuild implements BooleanSupplier {

    private final NativeConfig nativeConfig;

    public NativeBuild(final NativeConfig nativeConfig) {
        this.nativeConfig = nativeConfig;
    }

    @Override
    public boolean getAsBoolean() {
        return nativeConfig.enabled() && !nativeConfig.sourcesOnly();
    }
}
