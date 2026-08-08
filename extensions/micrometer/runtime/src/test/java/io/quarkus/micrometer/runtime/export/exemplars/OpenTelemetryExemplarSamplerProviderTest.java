package io.quarkus.micrometer.runtime.export.exemplars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
import io.opentelemetry.common.impl.ApiUsageLogger;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;

/**
 * Regression test for <a href="https://github.com/quarkusio/quarkus/issues/55855">#55855</a>:
 * Prometheus exemplar sampling must not pass a null OpenTelemetry {@link Context} into
 * {@link Span#fromContextOrNull(Context)}, which OpenTelemetry 1.62+ reports as API misuse.
 */
public class OpenTelemetryExemplarSamplerProviderTest {

    private static final String API_USAGE_WARNING = "OpenTelemetry API usage issue detected";

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
    private AtomicBoolean warnOnce;

    @BeforeEach
    void setUp() throws Exception {
        usageRecords.clear();
        warnOnce = warnOnceFlag();
        warnOnce.set(false);

        previousLevel = OTEL_USAGE_LOGGER.getLevel();
        // WARNING is what users see; FINEST carries the detailed misuse text.
        OTEL_USAGE_LOGGER.setLevel(Level.FINEST);
        OTEL_USAGE_LOGGER.addHandler(usageHandler);
    }

    @AfterEach
    void tearDown() {
        OTEL_USAGE_LOGGER.removeHandler(usageHandler);
        OTEL_USAGE_LOGGER.setLevel(previousLevel);
        warnOnce.set(false);
    }

    @Test
    void noCurrentContextDoesNotPassNullToFromContextOrNullOrLogApiMisuse() {
        assertNull(QuarkusContextStorage.INSTANCE.current());

        // Prove log capture works in this JVM before asserting absence of misuse logs.
        // ApiUsageLogger emits the user-visible WARNING only once unless WARN_ONCE is reset.
        Span.fromContextOrNull(null);
        assertTrue(hasApiUsageWarning(),
                "test must capture ApiUsageLogger WARNING; otherwise absence checks are meaningless");
        warnOnce.set(false);
        usageRecords.clear();

        io.prometheus.metrics.tracer.common.SpanContext sampler = new OpenTelemetryExemplarSamplerProvider()
                .exemplarSampler().orElseThrow();

        // CALLS_REAL_METHODS keeps ApiUsageLogger active if null is passed (old buggy path).
        try (MockedStatic<Span> span = Mockito.mockStatic(Span.class, Mockito.CALLS_REAL_METHODS)) {
            assertNull(sampler.getCurrentTraceId());
            assertNull(sampler.getCurrentSpanId());
            assertFalse(sampler.isCurrentSpanSampled());

            // Assert logs first (user-visible symptom from #55855), then the call site.
            assertFalse(warnOnce.get(), "OpenTelemetry API misuse should not be recorded");
            assertFalse(hasApiUsageWarning(), "io.opentelemetry.usage WARNING should not be logged");
            assertFalse(hasNullContextMisuseDetail(),
                    "detailed fromContextOrNull(null) misuse should not be logged at FINEST");
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

        assertFalse(warnOnce.get());
        assertFalse(hasApiUsageWarning());
    }

    private boolean hasApiUsageWarning() {
        return usageRecords.stream().anyMatch(record -> record.getLevel() == Level.WARNING
                && record.getMessage() != null
                && record.getMessage().contains(API_USAGE_WARNING));
    }

    private boolean hasNullContextMisuseDetail() {
        return usageRecords.stream().anyMatch(record -> record.getMessage() != null
                && record.getMessage().contains("fromContextOrNull(): context is null"));
    }

    private static AtomicBoolean warnOnceFlag() throws Exception {
        Field warnOnceField = ApiUsageLogger.class.getDeclaredField("WARN_ONCE");
        warnOnceField.setAccessible(true);
        return (AtomicBoolean) warnOnceField.get(null);
    }
}
