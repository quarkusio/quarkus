package io.quarkus.micrometer.runtime.export.exemplars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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

    private static final Logger USAGE_LOGGER = Logger.getLogger("io.opentelemetry.usage");

    private final List<LogRecord> usageRecords = new CopyOnWriteArrayList<>();
    private final Handler recordingHandler = new Handler() {
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
    private Level originalLevel;

    @BeforeEach
    void installRecordingHandler() {
        usageRecords.clear();
        originalLevel = USAGE_LOGGER.getLevel();
        // WARNING is the user-visible symptom; FINEST carries the detailed misuse text.
        USAGE_LOGGER.setLevel(Level.FINEST);
        USAGE_LOGGER.addHandler(recordingHandler);
    }

    @AfterEach
    void removeRecordingHandler() {
        USAGE_LOGGER.removeHandler(recordingHandler);
        USAGE_LOGGER.setLevel(originalLevel);
    }

    @Test
    void noOpenTelemetryApiUsageComplaintWhenNoContextIsActive() {
        assertNull(QuarkusContextStorage.INSTANCE.current());

        // Prove FINEST misuse capture works before asserting absence (no reflection).
        Span.fromContextOrNull(null);
        assertTrue(hasNullContextMisuseDetail(),
                "test must capture ApiUsageLogger FINEST misuse detail; otherwise absence checks are meaningless");
        usageRecords.clear();

        io.prometheus.metrics.tracer.common.SpanContext sampler = new OpenTelemetryExemplarSamplerProvider()
                .exemplarSampler().orElseThrow();

        try (MockedStatic<Span> span = Mockito.mockStatic(Span.class, Mockito.CALLS_REAL_METHODS)) {
            assertNull(sampler.getCurrentTraceId());
            assertNull(sampler.getCurrentSpanId());
            assertFalse(sampler.isCurrentSpanSampled());

            assertFalse(hasNullContextMisuseDetail(),
                    "fromContextOrNull(null) misuse should not be logged on io.opentelemetry.usage");
            span.verify(() -> Span.fromContextOrNull(null), never());
        }
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

        assertFalse(hasNullContextMisuseDetail());
    }

    private boolean hasNullContextMisuseDetail() {
        return usageRecords.stream().anyMatch(record -> record.getMessage() != null
                && record.getMessage().contains("fromContextOrNull(): context is null"));
    }
}
