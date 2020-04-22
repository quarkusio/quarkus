package io.quarkus.jaeger.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.jboss.logging.MDC;
import org.junit.jupiter.api.Test;

import io.jaegertracing.internal.JaegerSpanContext;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.util.ThreadLocalScopeManager;
import io.quarkus.jaeger.runtime.MDCScopeManager;

public class NestedMdcScopesTest {
    @Test
    public void mdcIsRestoredCorrectly() {
        ThreadLocalScopeManager threadLocalScopeManager = new ThreadLocalScopeManager();
        MDCScopeManager mdcScopeManager = new MDCScopeManager(threadLocalScopeManager);

        assertNull(mdcScopeManager.active());
        assertNull(threadLocalScopeManager.active());

        JaegerSpanContext span = new JaegerSpanContext(1, 1, 1, 1, Byte.valueOf("0"));
        Scope scope = mdcScopeManager.activate(new TestSpan(span), true);
        assertEquals(span, threadLocalScopeManager.active().span().context());
        assertEquals("10000000000000001", MDC.get("traceId"));

        JaegerSpanContext subSpan = new JaegerSpanContext(2, 2, 2, 1, Byte.valueOf("0"));
        Scope subScope = mdcScopeManager.activate(new TestSpan(subSpan), true);
        assertEquals(subSpan, threadLocalScopeManager.active().span().context());
        assertEquals("20000000000000002", MDC.get("traceId"));

        subScope.close();

        assertEquals(span, threadLocalScopeManager.active().span().context());
        assertEquals("10000000000000001", MDC.get("traceId"));

        scope.close();

        assertNull(mdcScopeManager.active());
        assertNull(threadLocalScopeManager.active());
    }

    static class TestSpan implements Span {
        private final SpanContext spanContext;

        TestSpan(SpanContext spanContext) {
            this.spanContext = spanContext;
        }

        @Override
        public SpanContext context() {
            return spanContext;
        }

        @Override
        public Span setTag(String key, String value) {
            return this;
        }

        @Override
        public Span setTag(String key, boolean value) {
            return this;
        }

        @Override
        public Span setTag(String key, Number value) {
            return this;
        }

        @Override
        public Span log(Map<String, ?> fields) {
            return this;
        }

        @Override
        public Span log(long timestampMicroseconds, Map<String, ?> fields) {
            return this;
        }

        @Override
        public Span log(String event) {
            return this;
        }

        @Override
        public Span log(long timestampMicroseconds, String event) {
            return this;
        }

        @Override
        public Span setBaggageItem(String key, String value) {
            return this;
        }

        @Override
        public String getBaggageItem(String key) {
            return null;
        }

        @Override
        public Span setOperationName(String operationName) {
            return this;
        }

        @Override
        public void finish() {
        }

        @Override
        public void finish(long finishMicros) {
        }
    }
}
