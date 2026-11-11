package io.quarkus.opentelemetry.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

@TargetClass(className = "io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder")
final class AutoConfiguredOpenTelemetrySdkBuilder {

    @Substitute
    private static AutoConfiguredOpenTelemetrySdk maybeConfigureFromFile(ConfigProperties config,
            ComponentLoader componentLoader) {
        // The declarative-config artifact is on the class path without its Jackson dependency, so the
        // DeclarativeConfigUtil path this method guards cannot link in a native image. File-based configuration
        // is not supported in Quarkus; returning null reports it as unconfigured.
        return null;
    }
}
