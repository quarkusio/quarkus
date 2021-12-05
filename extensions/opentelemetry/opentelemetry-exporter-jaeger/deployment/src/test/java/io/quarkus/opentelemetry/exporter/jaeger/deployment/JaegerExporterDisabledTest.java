package io.quarkus.opentelemetry.exporter.jaeger.deployment;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.opentelemetry.exporter.jaeger.runtime.LateBoundBatchSpanProcessor;
import io.quarkus.test.QuarkusUnitTest;

public class JaegerExporterDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.opentelemetry.tracer.exporter.jaeger.enabled", "false");

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    Instance<LateBoundBatchSpanProcessor> lateBoundBatchSpanProcessorInstance;

    @Test
    void testOpenTelemetryButNoBatchSpanProcessor() {
        Assertions.assertNotNull(openTelemetry);
        Assertions.assertFalse(lateBoundBatchSpanProcessorInstance.isResolvable());
    }
}
