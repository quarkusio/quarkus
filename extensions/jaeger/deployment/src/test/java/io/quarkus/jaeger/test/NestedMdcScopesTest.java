package io.quarkus.jaeger.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;

import org.jboss.logging.MDC;
import org.junit.jupiter.api.Test;

import io.jaegertracing.internal.JaegerSpanContext;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.tag.Tag;
import io.opentracing.util.ThreadLocalScopeManager;
import io.quarkus.jaeger.runtime.MDCScopeManager;

public class NestedMdcScopesTest {
    @Test
    public void mdcIsRestoredCorrectly() {
        ThreadLocalScopeManager threadLocalScopeManager = new ThreadLocalScopeManager();
        MDCScopeManager mdcScopeManager = new MDCScopeManager(threadLocalScopeManager);

        assertNull(mdcScopeManager.activeSpan());
        assertNull(threadLocalScopeManager.activeSpan());
        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("parentId"));

        JaegerSpanContext span = new JaegerSpanContext(1, 1, 1, 0, Byte.parseByte("0"));
        Scope scope = mdcScopeManager.activate(new TestSpan(span));
        assertSame(span, threadLocalScopeManager.activeSpan().context());
        assertEquals("00000000000000010000000000000001", MDC.get("traceId"));
        assertEquals("0", MDC.get("parentId"));

        JaegerSpanContext subSpan = new JaegerSpanContext(2, 2, 2, 1, Byte.parseByte("0"));
        Scope subScope = mdcScopeManager.activate(new TestSpan(subSpan));
        assertSame(subSpan, threadLocalScopeManager.activeSpan().context());
        assertEquals("00000000000000020000000000000002", MDC.get("traceId"));
        assertEquals("1", MDC.get("parentId"));

        subScope.close();

        assertSame(span, threadLocalScopeManager.activeSpan().context());
        assertEquals("00000000000000010000000000000001", MDC.get("traceId"));
        assertEquals("0", MDC.get("parentId"));

        scope.close();

        assertNull(mdcScopeManager.activeSpan());
        assertNull(threadLocalScopeManager.activeSpan());
        assertNull(MDC.get("traceId"));
        assertNull(MDC.get("parentId"));
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
        public <T> Span setTag(final Tag<T> tag, final T value) {
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
