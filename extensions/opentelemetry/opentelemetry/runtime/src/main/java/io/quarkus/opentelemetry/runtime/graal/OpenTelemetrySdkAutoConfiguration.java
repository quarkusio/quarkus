package io.quarkus.opentelemetry.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.opentelemetry.sdk.OpenTelemetrySdk;

// We can remove this substitution once AutoConfigure SPI is a separate artifact
@TargetClass(className = "io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkAutoConfiguration")
@Substitute
public final class OpenTelemetrySdkAutoConfiguration {
    @Substitute
    private OpenTelemetrySdkAutoConfiguration() {
    }

    @Substitute
    public static OpenTelemetrySdk initialize() {
        return null;
    }
}
