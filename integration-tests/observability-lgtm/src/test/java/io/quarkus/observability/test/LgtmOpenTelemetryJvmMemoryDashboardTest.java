package io.quarkus.observability.test;

import java.util.Map;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * JVM metrics sent by the OpenTelemetry JVM instrumentation, following the semantic conventions:
 * {@code jvm_memory_limit_bytes} and the {@code jvm_memory_type} and {@code jvm_memory_pool_name} labels. This is what an
 * application using only the OpenTelemetry extension sends.
 */
@QuarkusTest
@TestProfile(LgtmOpenTelemetryJvmMemoryDashboardTest.OpenTelemetryJvmMetrics.class)
@DisabledOnOs(OS.WINDOWS)
public class LgtmOpenTelemetryJvmMemoryDashboardTest extends LgtmJvmMemoryDashboardTestBase {

    public static class OpenTelemetryJvmMetrics implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.otel.instrument.jvm-metrics", "true");
        }
    }
}
