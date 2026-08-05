package io.quarkus.micrometer.runtime.export.exemplars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;

/**
 * Regression test for <a href="https://github.com/quarkusio/quarkus/issues/55855">#55855</a>:
 * Prometheus exemplar sampling must not pass a null OpenTelemetry {@link Context} into
 * {@link Span#fromContextOrNull(Context)}, which OpenTelemetry 1.62+ reports as API misuse.
 */
public class OpenTelemetryExemplarSamplerProviderTest {

    private static final Logger OTEL_USAGE_LOGGER = Logger.getLogger("io.opentelemetry.usage");

    private final List<LogRecord> usageRecords = new ArrayList<>();
    private final Handler usageHandler = new Handler() {
        @Override
        public void publish(LogRecord record) {
            usageRecords.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };

    private Level previousLevel;

    @BeforeEach
    void setUp() {
        usageRecords.clear();
        previousLevel = OTEL_USAGE_LOGGER.getLevel();
        OTEL_USAGE_LOGGER.setLevel(Level.FINEST);
        OTEL_USAGE_LOGGER.addHandler(usageHandler);
    }

    @AfterEach
    void tearDown() {
        OTEL_USAGE_LOGGER.removeHandler(usageHandler);
        OTEL_USAGE_LOGGER.setLevel(previousLevel);
    }

    @Test
    void noCurrentContextDoesNotTriggerOpenTelemetryApiUsageWarning() {
        assertNull(QuarkusContextStorage.INSTANCE.current());

        io.prometheus.metrics.tracer.common.SpanContext sampler = new OpenTelemetryExemplarSamplerProvider()
                .exemplarSampler().orElseThrow();

        assertNull(sampler.getCurrentTraceId());
        assertNull(sampler.getCurrentSpanId());
        assertFalse(sampler.isCurrentSpanSampled());

        assertFalse(usageRecords.stream().anyMatch(OpenTelemetryExemplarSamplerProviderTest::isNullContextMisuse),
                "OpenTelemetry API misuse should not be logged when there is no current Context");
    }

    @Test
    void currentSpanIsExposedToPrometheusExemplarSampler() {
        String traceId = "0123456789abcdef0123456789abcdef";
        String spanId = "0123456789abcdef";
        Span span = Span.wrap(SpanContext.create(traceId, spanId, TraceFlags.getSampled(), TraceState.getDefault()));

        io.prometheus.metrics.tracer.common.SpanContext sampler = new OpenTelemetryExemplarSamplerProvider()
                .exemplarSampler().orElseThrow();

        try (Scope scope = QuarkusContextStorage.INSTANCE.attach(Context.root().with(span))) {
            assertEquals(traceId, sampler.getCurrentTraceId());
            assertEquals(spanId, sampler.getCurrentSpanId());
            assertTrue(sampler.isCurrentSpanSampled());
        }

        assertFalse(usageRecords.stream().anyMatch(OpenTelemetryExemplarSamplerProviderTest::isNullContextMisuse));
    }

    private static boolean isNullContextMisuse(LogRecord record) {
        String message = record.getMessage();
        return message != null && message.contains("fromContextOrNull(): context is null");
    }
}
