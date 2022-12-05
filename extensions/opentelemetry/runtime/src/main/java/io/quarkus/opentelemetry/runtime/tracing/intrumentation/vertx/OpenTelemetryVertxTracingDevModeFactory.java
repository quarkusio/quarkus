package io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx;

import java.util.function.BiConsumer;

import io.vertx.core.Context;
import io.vertx.core.spi.VertxTracerFactory;
import io.vertx.core.spi.tracing.SpanKind;
import io.vertx.core.spi.tracing.TagExtractor;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingOptions;
import io.vertx.core.tracing.TracingPolicy;

public class OpenTelemetryVertxTracingDevModeFactory implements VertxTracerFactory {
    private final Delegator vertxTracerDelegator = new Delegator();

    public OpenTelemetryVertxTracingDevModeFactory() {
    }

    public Delegator getVertxTracerDelegator() {
        return vertxTracerDelegator;
    }

    @Override
    public VertxTracer<?, ?> tracer(final TracingOptions options) {
        return vertxTracerDelegator;
    }

    public static class Delegator implements VertxTracer {
        private VertxTracer delegate;

        public VertxTracer getDelegate() {
            return delegate;
        }

        public Delegator setDelegate(final VertxTracer delegate) {
            this.delegate = delegate;
            return this;
        }

        @Override
        public Object receiveRequest(
                final Context context,
                final SpanKind kind,
                final TracingPolicy policy,
                final Object request,
                final String operation,
                final Iterable headers,
                final TagExtractor tagExtractor) {
            return delegate.receiveRequest(context, kind, policy, request, operation, headers, tagExtractor);
        }

        @Override
        public void sendResponse(
                final Context context,
                final Object response,
                final Object payload,
                final Throwable failure,
                final TagExtractor tagExtractor) {
            delegate.sendResponse(context, response, payload, failure, tagExtractor);
        }

        @Override
        public Object sendRequest(
                final Context context,
                final SpanKind kind,
                final TracingPolicy policy,
                final Object request,
                final String operation,
                final BiConsumer headers,
                final TagExtractor tagExtractor) {
            return delegate.sendRequest(context, kind, policy, request, operation, headers, tagExtractor);
        }

        @Override
        public void receiveResponse(
                final Context context,
                final Object response,
                final Object payload,
                final Throwable failure,
                final TagExtractor tagExtractor) {
            delegate.receiveResponse(context, response, payload, failure, tagExtractor);
        }
    }
}
