package io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.opentelemetry.context.Scope;
import io.vertx.core.Context;
import io.vertx.core.MultiMap;
import io.vertx.core.spi.tracing.SpanKind;
import io.vertx.core.spi.tracing.TagExtractor;
import io.vertx.core.spi.tracing.VertxTracer;
import io.vertx.core.tracing.TracingPolicy;

public class OpenTelemetryVertxTracer
        implements VertxTracer<OpenTelemetryVertxTracer.SpanOperation, OpenTelemetryVertxTracer.SpanOperation> {

    private final List<InstrumenterVertxTracer<?, ?>> instrumenterVertxTracers;

    public OpenTelemetryVertxTracer(final List<InstrumenterVertxTracer<?, ?>> instrumenterVertxTracers) {
        this.instrumenterVertxTracers = Collections.unmodifiableList(instrumenterVertxTracers);
    }

    @Override
    public <R> SpanOperation receiveRequest(final Context context, final SpanKind kind, final TracingPolicy policy,
            final R request, final String operation, final Iterable<Map.Entry<String, String>> headers,
            final TagExtractor<R> tagExtractor) {

        return getTracer(request, tagExtractor).receiveRequest(context, kind, policy, request, operation, headers,
                tagExtractor);
    }

    @Override
    public <R> void sendResponse(final Context context, final R response, final SpanOperation spanOperation,
            final Throwable failure, final TagExtractor<R> tagExtractor) {

        getTracer(spanOperation, tagExtractor).sendResponse(context, response, spanOperation, failure, tagExtractor);
    }

    @Override
    public <R> SpanOperation sendRequest(final Context context, final SpanKind kind, final TracingPolicy policy,
            final R request, final String operation, final BiConsumer<String, String> headers,
            final TagExtractor<R> tagExtractor) {

        return getTracer(request, tagExtractor).sendRequest(context, kind, policy, request, operation, headers,
                tagExtractor);
    }

    @Override
    public <R> void receiveResponse(final Context context, final R response, final SpanOperation spanOperation,
            final Throwable failure, final TagExtractor<R> tagExtractor) {

        getTracer(spanOperation, tagExtractor).receiveResponse(context, response, spanOperation, failure, tagExtractor);
    }

    @SuppressWarnings("unchecked")
    private <R> VertxTracer<SpanOperation, SpanOperation> getTracer(final R request,
            final TagExtractor<R> tagExtractor) {

        for (InstrumenterVertxTracer<?, ?> instrumenterVertxTracer : instrumenterVertxTracers) {
            if (instrumenterVertxTracer.canHandle(request, tagExtractor)) {
                return instrumenterVertxTracer;
            }
        }

        return NOOP;
    }

    @SuppressWarnings("unchecked")
    private <R> VertxTracer<SpanOperation, SpanOperation> getTracer(final SpanOperation spanOperation,
            final TagExtractor<R> tagExtractor) {
        return spanOperation != null ? getTracer((R) spanOperation.getRequest(), tagExtractor) : NOOP;
    }

    static class SpanOperation {
        private final Context context;
        private final Object request;
        private final MultiMap headers;
        private final io.opentelemetry.context.Context spanContext;
        private final Scope scope;

        public SpanOperation(final Context context, final Object request, final MultiMap headers,
                final io.opentelemetry.context.Context spanContext, final Scope scope) {
            this.context = context;
            this.request = request;
            this.headers = headers;
            this.spanContext = spanContext;
            this.scope = scope;
        }

        public Context getContext() {
            return context;
        }

        public Object getRequest() {
            return request;
        }

        public MultiMap getHeaders() {
            return headers;
        }

        public io.opentelemetry.context.Context getSpanContext() {
            return spanContext;
        }

        public Scope getScope() {
            return scope;
        }

        static SpanOperation span(final Context context, final Object request, final MultiMap headers,
                final io.opentelemetry.context.Context spanContext, final Scope scope) {

            return new SpanOperation(context, request, headers, spanContext, scope);
        }
    }
}
