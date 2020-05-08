package io.quarkus.picocli.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "picocli")
class PicocliDeploymentConfiguration {

    /**
     * <p>
     * Set this to false to use the `picocli-codegen` annotation processor instead of build steps.
     * </p>
     * <p>
     * CAUTION: this will have serious build-time performance impact since
     * this is run on every restart in devmode, use with care!
     * </p>
     * <p>
     * This property is intended to be used only in cases where an incompatible change in the
     * picocli library causes problems in the build steps used to support GraalVM Native images.
     * </p>
     * <p>
     * In such cases this property allows users to make the trade-off between fast build cycles
     * with the older version of picocli, and temporarily accept slower build cycles with
     * the latest version of picocli until the updated extension is available.
     * </p>
     */
    @ConfigItem(name = "native-image.processing.enable", defaultValue = "true")
    boolean nativeImageProcessingEnabled;
}
