package io.quarkus.observability.devresource;

import java.util.function.Function;

/**
 * Relevant Observability extensions present.
 */
public record ExtensionsCatalog(
        Function<String, Boolean> resourceChecker,
        Function<String, Boolean> classChecker,
        boolean hasOpenTelemetry,
        boolean hasMicrometerOtlp) {
}
