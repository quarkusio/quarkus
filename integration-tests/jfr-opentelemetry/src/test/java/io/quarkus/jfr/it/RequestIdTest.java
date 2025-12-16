package io.quarkus.jfr.it;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.jfr.api.IdProducer;
import io.quarkus.test.junit.QuarkusTest;

@ActivateRequestContext
@QuarkusTest
public class RequestIdTest {

    @Inject
    IdProducer idProducer;

    @Inject
    Tracer tracer;

    @Test
    public void test() {
        String traceId = idProducer.getTraceId();
        String spanId = idProducer.getSpanId();

        Assertions.assertNotNull(traceId);
        Assertions.assertNotNull(spanId);
    }

    @Test
    public void testNewSpan() {

        String traceId1;
        String spanId1;
        String traceId2;
        String spanId2;
        String traceId3;
        String spanId3;

        Span span1 = tracer.spanBuilder("first").startSpan();
        try (Scope scope1 = span1.makeCurrent()) {
            traceId1 = idProducer.getTraceId();
            spanId1 = idProducer.getSpanId();

            Span span2 = tracer.spanBuilder("second").startSpan();
            try (Scope scope2 = span2.makeCurrent()) {
                traceId2 = idProducer.getTraceId();
                spanId2 = idProducer.getSpanId();
            } catch (Exception e) {
                span2.recordException(e);
                span2.setStatus(StatusCode.ERROR);
                throw e;
            } finally {
                span2.end();
            }

            traceId3 = idProducer.getTraceId();
            spanId3 = idProducer.getSpanId();
        } catch (Exception e) {
            span1.recordException(e);
            span1.setStatus(StatusCode.ERROR);
            throw e;
        } finally {
            span1.end();
        }

        Assertions.assertEquals(traceId1, traceId2);
        Assertions.assertNotEquals(spanId1, spanId2);

        Assertions.assertEquals(traceId1, traceId3);
        Assertions.assertEquals(spanId1, spanId3);
    }
}
