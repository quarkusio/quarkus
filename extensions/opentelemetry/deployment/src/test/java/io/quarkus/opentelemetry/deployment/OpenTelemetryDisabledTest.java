package io.quarkus.opentelemetry.deployment;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.test.QuarkusUnitTest;

public class OpenTelemetryDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.enabled", "false")
            .assertException(t -> Assertions.assertEquals(DeploymentException.class, t.getClass()));

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void testNoOpenTelemetry() {
        //Should not be reached: dump what was injected if it somehow passed
        Assertions.assertNull(openTelemetry,
                "A OpenTelemetry instance should not be found/injected when OpenTelemetry is disabled");
    }
}
