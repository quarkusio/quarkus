package io.quarkus.opentelemetry.deployment;

import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.trace.Tracer;
import io.quarkus.test.QuarkusUnitTest;

public class TracerDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.opentelemetry.tracer.enabled", "false")
            .assertException(t -> Assertions.assertEquals(DeploymentException.class, t.getClass()));

    @Inject
    Tracer tracer;

    @Test
    void testNoTracer() {
        //Should not be reached: dump what was injected if it somehow passed
        Assertions.assertNull(tracer,
                "A Tracer instance should not be found/injected when OpenTelemetry tracer is disabled");
    }
}
