package io.quarkus.observability.test;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * JVM metrics sent by the Micrometer JVM binder: {@code jvm_memory_max_bytes} and the {@code area} and {@code id}
 * labels.
 */
@QuarkusTest
@TestProfile(LgtmMicrometerJvmMemoryDashboardTest.MicrometerJvmMetrics.class)
@DisabledOnOs(OS.WINDOWS)
public class LgtmMicrometerJvmMemoryDashboardTest extends LgtmJvmMemoryDashboardTestBase {

    private static final String MICROMETER_OTLP_DASHBOARD = "edy6473ay1vk0c";

    @Test
    public void otlpDashboardMemoryPoolPanelsShowUsedAndCommittedOfEachPool() {
        assertPoolPanelsShowUsedAndCommittedOfEachPool(dashboard(MICROMETER_OTLP_DASHBOARD));
    }

    public static class MicrometerJvmMetrics implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.micrometer.binder.jvm", "true");
        }
    }
}
