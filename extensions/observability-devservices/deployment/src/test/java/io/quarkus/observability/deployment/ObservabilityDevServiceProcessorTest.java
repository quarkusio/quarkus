package io.quarkus.observability.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.Test;

import io.smallrye.config.PropertiesConfigSource;

class ObservabilityDevServiceProcessorTest {

    private static final String OTEL_ENDPOINT = "quarkus.otel.exporter.otlp.endpoint";
    private static final String OTEL_PROTOCOL = "quarkus.otel.exporter.otlp.protocol";

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

    private Config config(Map<String, String> config) {
        return ConfigProviderResolver.instance().getBuilder()
                .withSources(new PropertiesConfigSource(config, "test config", 1))
                .build();
    }
}
