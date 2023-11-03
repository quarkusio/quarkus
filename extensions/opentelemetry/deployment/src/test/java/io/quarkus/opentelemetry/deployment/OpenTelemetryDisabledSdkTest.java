package io.quarkus.opentelemetry.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.quarkus.opentelemetry.runtime.exporter.otlp.LateBoundBatchSpanProcessor;
import io.quarkus.test.QuarkusUnitTest;

@Disabled("Not implemented")
public class OpenTelemetryDisabledSdkTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.sdk.disabled", "true");

    @Inject
    LateBoundBatchSpanProcessor batchSpanProcessor;

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void testNoTracer() {
        // The OTel API doesn't provide a clear way to check if a tracer is an effective NOOP tracer.
        Assertions.assertTrue(batchSpanProcessor.isDelegateNull(), "BatchSpanProcessor delegate must not be set");
    }
}
