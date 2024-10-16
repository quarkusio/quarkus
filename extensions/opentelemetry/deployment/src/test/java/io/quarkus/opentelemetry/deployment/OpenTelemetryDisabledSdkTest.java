package io.quarkus.opentelemetry.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.LateBoundBatchSpanProcessor;
import io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx.HttpInstrumenterVertxTracer;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.spi.observability.HttpRequest;
import io.vertx.core.spi.observability.HttpResponse;

public class OpenTelemetryDisabledSdkTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.otel.sdk.disabled", "true");

    @Inject
    LateBoundBatchSpanProcessor batchSpanProcessor;

    @Inject
    OpenTelemetry openTelemetry;

    @Inject
    OTelRuntimeConfig runtimeConfig;

    @Test
    void testNoTracer() {
        // The OTel API doesn't provide a clear way to check if a tracer is an effective NOOP tracer.
        Assertions.assertTrue(batchSpanProcessor.isDelegateNull(), "BatchSpanProcessor delegate must not be set");
    }

    @Test
    void noReceiveRequestInstrumenter() {
        HttpInstrumenterVertxTracer instrumenter = new HttpInstrumenterVertxTracer(openTelemetry, runtimeConfig);

        Instrumenter<HttpRequest, HttpResponse> receiveRequestInstrumenter = instrumenter.getReceiveRequestInstrumenter();
        assertFalse(receiveRequestInstrumenter.shouldStart(null, null),
                "Instrumenter must not start, if it does, it will throw an exception because of the null objects we are passing");
    }

    @Test
    void noReceiveResponseInstrumenter() {
        HttpInstrumenterVertxTracer instrumenter = new HttpInstrumenterVertxTracer(openTelemetry, runtimeConfig);

        Instrumenter<HttpRequest, HttpResponse> receiveRequestInstrumenter = instrumenter.getReceiveResponseInstrumenter();
        assertFalse(receiveRequestInstrumenter.shouldStart(null, null),
                "Instrumenter must not start, if it does, it will throw an exception because of the null objects we are passing");
    }

    @Test
    void noSendRequestInstrumenter() {
        HttpInstrumenterVertxTracer instrumenter = new HttpInstrumenterVertxTracer(openTelemetry, runtimeConfig);

        Instrumenter<HttpRequest, HttpResponse> receiveRequestInstrumenter = instrumenter.getSendRequestInstrumenter();
        assertFalse(receiveRequestInstrumenter.shouldStart(null, null),
                "Instrumenter must not start, if it does, it will throw an exception because of the null objects we are passing");
    }

    @Test
    void noSendResponseInstrumenter() {
        HttpInstrumenterVertxTracer instrumenter = new HttpInstrumenterVertxTracer(openTelemetry, runtimeConfig);

        Instrumenter<HttpRequest, HttpResponse> receiveRequestInstrumenter = instrumenter.getSendResponseInstrumenter();
        assertFalse(receiveRequestInstrumenter.shouldStart(null, null),
                "Instrumenter must not start, if it does, it will throw an exception because of the null objects we are passing");
    }
}
