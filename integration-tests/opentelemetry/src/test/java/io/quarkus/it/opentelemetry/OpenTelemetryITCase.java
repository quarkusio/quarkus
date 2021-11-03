package io.quarkus.it.opentelemetry;

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class OpenTelemetryITCase extends OpenTelemetryTestCase {
    @Override
    protected void buildGlobalTelemetryInstance() {
        // When running native tests the test class is outside the Quarkus application,
        // so we need to set the propagator on the GlobalOpenTelemetry instance
        OpenTelemetrySdkBuilder builder = OpenTelemetrySdk.builder();
        builder.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()));
        builder.buildAndRegisterGlobal();
    }
}
