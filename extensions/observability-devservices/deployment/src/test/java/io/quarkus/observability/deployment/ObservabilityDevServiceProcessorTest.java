package io.quarkus.observability.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.Test;

import io.quarkus.observability.devresource.ExtensionsCatalog;
import io.smallrye.config.PropertiesConfigSource;

class ObservabilityDevServiceProcessorTest {

    private static final String OTEL_ENDPOINT = "quarkus.otel.exporter.otlp.endpoint";
    private static final String OTEL_PROTOCOL = "quarkus.otel.exporter.otlp.protocol";
    private static final String MICROMETER_OTLP_URL = "quarkus.micrometer.export.otlp.url";
    private static final ExtensionsCatalog OPEN_TELEMETRY_CATALOG = new ExtensionsCatalog(s -> false, s -> false, true,
            false);
    private static final ExtensionsCatalog MICROMETER_OTLP_CATALOG = new ExtensionsCatalog(s -> false, s -> false, false,
            true);
    private static final ExtensionsCatalog NO_COLLECTOR_CATALOG = new ExtensionsCatalog(s -> false, s -> false, false,
            false);

    @Test
    void lgtmProvidesOtlpDefaultsWhenNoEndpointIsConfigured() {
        assertThat(ObservabilityDevServiceProcessor.openTelemetryConfigProvider(config(Map.of())))
                .containsKeys(OTEL_ENDPOINT, OTEL_PROTOCOL);
    }

    @Test
    void lgtmDoesNotProvideOtlpDefaultsWhenGenericEndpointIsConfigured() {
        assertThat(ObservabilityDevServiceProcessor.openTelemetryConfigProvider(
                config(Map.of(OTEL_ENDPOINT, "http://localhost:4317"))))
                .doesNotContainKeys(OTEL_ENDPOINT, OTEL_PROTOCOL);
    }

    @Test
    void lgtmDoesNotProvideOtlpDefaultsWhenSignalEndpointIsConfigured() {
        assertThat(ObservabilityDevServiceProcessor.openTelemetryConfigProvider(
                config(Map.of("quarkus.otel.exporter.otlp.traces.endpoint", "http://localhost:4317"))))
                .doesNotContainKeys(OTEL_ENDPOINT, OTEL_PROTOCOL);
    }

    @Test
    void lgtmDoesNotStartWhenGenericOtelEndpointIsConfigured() {
        assertThat(ObservabilityDevServiceProcessor.shouldStartDevService("Lgtm", OPEN_TELEMETRY_CATALOG,
                config(Map.of(OTEL_ENDPOINT, "http://localhost:4317"))))
                .isFalse();
    }

    @Test
    void lgtmDoesNotStartWhenSignalOtelEndpointIsConfigured() {
        assertThat(ObservabilityDevServiceProcessor.shouldStartDevService("Lgtm", OPEN_TELEMETRY_CATALOG,
                config(Map.of("quarkus.otel.exporter.otlp.traces.endpoint", "http://localhost:4317"))))
                .isFalse();
    }

    @Test
    void lgtmDoesNotStartWhenMicrometerOtlpUrlIsConfigured() {
        assertThat(ObservabilityDevServiceProcessor.shouldStartDevService("Lgtm", MICROMETER_OTLP_CATALOG,
                config(Map.of(MICROMETER_OTLP_URL, "http://localhost:4318/v1/metrics"))))
                .isFalse();
    }

    @Test
    void lgtmStillStartsWhenConfiguredCollectorIsNotPresent() {
        assertThat(ObservabilityDevServiceProcessor.shouldStartDevService("Lgtm", NO_COLLECTOR_CATALOG,
                config(Map.of(OTEL_ENDPOINT, "http://localhost:4317", MICROMETER_OTLP_URL,
                        "http://localhost:4318/v1/metrics"))))
                .isTrue();
    }

    @Test
    void otherDevServicesStillStartWhenOtlpEndpointIsConfigured() {
        assertThat(ObservabilityDevServiceProcessor.shouldStartDevService("Other", OPEN_TELEMETRY_CATALOG,
                config(Map.of(OTEL_ENDPOINT, "http://localhost:4317"))))
                .isTrue();
    }

    private Config config(Map<String, String> config) {
        return ConfigProviderResolver.instance().getBuilder()
                .withSources(new PropertiesConfigSource(config, "test config", 1))
                .build();
    }
}
