package io.quarkus.opentelemetry.deployment.traces;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.quarkus.opentelemetry.deployment.common.TestUtil;
import io.quarkus.test.QuarkusUnitTest;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OpenTelemetryIdGeneratorTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(TestUtil.class));

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    @Order(1)
    void testGenerateIds()
            throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        IdGenerator idGenerator = TestUtil.getIdGenerator(openTelemetry);
        String spanId = idGenerator.generateSpanId();
        String traceId = idGenerator.generateTraceId();

        assertEquals(String.format("%016d", OtelConfiguration.SPAN_START_NUMBER + 1), spanId);
        assertEquals(String.format("%032d", OtelConfiguration.TRACE_START_NUMBER + 1), traceId);
    }

    @Test
    @Order(2)
    void testGenerateSpan() {
        Tracer testTracer = openTelemetry.getTracer("io.quarkus.opentelemetry.deployment");
        Span testSpan = testTracer.spanBuilder("testSpan").startSpan();
        testSpan.end();

        String generatedSpanId = testSpan.getSpanContext().getSpanId();
        String generatedTraceId = testSpan.getSpanContext().getTraceId();
        assertEquals(String.format("%016d", OtelConfiguration.SPAN_START_NUMBER + 2), generatedSpanId);
        assertEquals(String.format("%032d", OtelConfiguration.TRACE_START_NUMBER + 2), generatedTraceId);
    }

    @ApplicationScoped
    public static class OtelConfiguration {
        static final long TRACE_START_NUMBER = 42;
        static final long SPAN_START_NUMBER = 42;

        @Produces
        public IdGenerator idGenerator() {
            return new IdGenerator() {
                final AtomicLong traceId = new AtomicLong(OtelConfiguration.TRACE_START_NUMBER);
                final AtomicLong spanId = new AtomicLong(OtelConfiguration.SPAN_START_NUMBER);

                @Override
                public String generateSpanId() {
                    return String.format("%016d", spanId.incrementAndGet());
                }

                @Override
                public String generateTraceId() {
                    return String.format("%032d", traceId.incrementAndGet());
                }
            };
        }
    }
}
