package io.quarkus.opentelemetry.runtime;

/**
 * Marker for the Quarkus-internal {@link AutoConfiguredOpenTelemetrySdkBuilderCustomizer} implementations that wire
 * the legacy autoconfigure engine. The built-in customizer beans are {@code @Typed} to this interface, which removes
 * the deprecated interface from their bean types: a resolvable bean of the deprecated type is therefore always
 * user-provided, and that is what engages the legacy engine. Removed together with the deprecated SPI in Quarkus 4.3.
 */
@SuppressWarnings("removal")
public interface InternalSdkBuilderCustomizer extends AutoConfiguredOpenTelemetrySdkBuilderCustomizer {
}
