package io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx;

import java.util.function.BiConsumer;

import org.jboss.logging.Logger;

import io.vertx.core.Context;
import io.vertx.core.spi.VertxTracerFactory;
import io.vertx.core.spi.tracing.SpanKind;
import io.vertx.core.spi.tracing.TagExtractor;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingOptions;
import io.vertx.core.tracing.TracingPolicy;

public class OpenTelemetryVertxTracingFactory implements VertxTracerFactory {
    private final VertxDelegator vertxTracerDelegator = new VertxDelegator();

    public OpenTelemetryVertxTracingFactory() {
    }

    public VertxDelegator getVertxTracerDelegator() {
        return vertxTracerDelegator;
    }

    @Override
    public VertxTracer<?, ?> tracer(final TracingOptions options) {
        return vertxTracerDelegator;
    }

    public static class VertxDelegator implements VertxTracer {
        private static final Logger log = Logger.getLogger(VertxDelegator.class);

        private VertxTracer delegate;

        public VertxTracer getDelegate() {
            return delegate;
        }

        public VertxDelegator setDelegate(final VertxTracer delegate) {
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
            if (delegate == null) {
                log.warnv("VertxTracer delegate not set. Will not submit this trace. " +
                        "SpanKind: {0}; Request: {1}; Operation:{2}.",
                        kind,
                        request == null ? "null" : request.toString(),
                        operation);
                return null;
            }
            return delegate.receiveRequest(context, kind, policy, request, operation, headers, tagExtractor);
        }

        @Override
        public void sendResponse(
                final Context context,
                final Object response,
                final Object payload,
                final Throwable failure,
                final TagExtractor tagExtractor) {
            if (delegate == null) {
                log.warnv("VertxTracer delegate not set. Will not submit this trace. " +
                        "Response: {0}; Failure: {1}.",
                        response == null ? "null" : response.toString(),
                        failure == null ? "null" : failure.getMessage());
                return;
            }
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
            if (delegate == null) {
                log.warnv("VertxTracer delegate not set. Will not submit this trace. " +
                        "SpanKind: {0}; Request: {1}; Operation:{2}.",
                        kind,
                        request == null ? "null" : request.toString(),
                        operation);
                return null;
            }
            return delegate.sendRequest(context, kind, policy, request, operation, headers, tagExtractor);
        }

        @Override
        public void receiveResponse(
                final Context context,
                final Object response,
                final Object payload,
                final Throwable failure,
                final TagExtractor tagExtractor) {
            if (delegate == null) {
                log.warnv("VertxTracer delegate not set. Will not submit this trace. " +
                        "Response: {0}; Failure: {1}.",
                        response == null ? "null" : response.toString(),
                        failure == null ? "null" : failure.getMessage());
                return;
            }
            delegate.receiveResponse(context, response, payload, failure, tagExtractor);
        }
    }
}
