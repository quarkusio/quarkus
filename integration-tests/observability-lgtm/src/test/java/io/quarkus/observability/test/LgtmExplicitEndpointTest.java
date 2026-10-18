package io.quarkus.observability.test;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * Verifies that LGTM Dev Services do not start when an OTLP exporter endpoint
 * is explicitly configured, preventing the dev service from overriding a
 * user-provided collector endpoint.
 */
@QuarkusTest
@TestProfile(LgtmExplicitEndpointTest.ExplicitEndpointProfile.class)
@DisabledOnOs(OS.WINDOWS)
public class LgtmExplicitEndpointTest {

    @Inject
    Config config;

    @Test
    public void testDevServicesNotStartedWhenEndpointExplicitlyConfigured() {
        Optional<String> grafanaEndpoint = config.getOptionalValue("grafana.endpoint", String.class);
        Assertions.assertTrue(grafanaEndpoint.isEmpty(),
                "grafana.endpoint should not be set when an OTLP endpoint is explicitly configured, but was: "
                        + grafanaEndpoint.orElse(""));

        Optional<String> otelCollectorUrl = config.getOptionalValue("otel-collector.url", String.class);
        Assertions.assertTrue(otelCollectorUrl.isEmpty(),
                "otel-collector.url should not be set when an OTLP endpoint is explicitly configured, but was: "
                        + otelCollectorUrl.orElse(""));
    }

    public static class ExplicitEndpointProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.otel.exporter.otlp.traces.endpoint", "http://localhost:14250");
        }
    }
}
