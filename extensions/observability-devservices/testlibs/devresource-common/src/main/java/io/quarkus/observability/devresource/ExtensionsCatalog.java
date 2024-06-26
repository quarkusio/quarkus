package io.quarkus.observability.devresource;

/**
 * Relevant Observability extensions present.
 */
public record ExtensionsCatalog(boolean hasOpenTelemetry,
        boolean hasMicrometerOtlp) {
}
