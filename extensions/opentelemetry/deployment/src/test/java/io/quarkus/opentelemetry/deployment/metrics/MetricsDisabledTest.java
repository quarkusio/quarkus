package io.quarkus.opentelemetry.deployment.metrics;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.metrics.Meter;
import io.quarkus.test.QuarkusUnitTest;

public class MetricsDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.metrics.enabled", "false")
            .assertException(t -> Assertions.assertEquals(DeploymentException.class, t.getClass()));

    @Inject
    Meter openTelemetryMeter;

    @Test
    void testNoOpenTelemetry() {
        //Should not be reached: dump what was injected if it somehow passed
        Assertions.assertNull(openTelemetryMeter,
                "A OpenTelemetry Meter instance should not be found/injected when OpenTelemetry metrics is disabled");
    }
}
