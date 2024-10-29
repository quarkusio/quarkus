package io.quarkus.observability.test;

import java.util.Map;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * Simple case were you have to provide your own configuration
 */
@QuarkusTest
@TestProfile(LgtmResourcesTest.DevResourcesTestProfileOnly.class)
@DisabledOnOs(OS.WINDOWS)
public class LgtmResourcesTest extends LgtmConfigTestBase {

    public static class DevResourcesTestProfileOnly implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.micrometer.export.otlp.url", "http://${otel-collector.url}/v1/metrics",
                    "quarkus.otel.exporter.otlp.protocol", "http/protobuf",
                    "quarkus.otel.exporter.otlp.endpoint", "http://${otel-collector.url}",
                    "quarkus.observability.dev-resources", "true",
                    "quarkus.observability.enabled", "false");
        }
    }
}
