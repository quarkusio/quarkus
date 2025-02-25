package io.quarkus.websockets.next.runtime.telemetry;

import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.semconv.UrlAttributes;

final class TracesConnectionInterceptor implements ConnectionInterceptor {

    static final String CONNECTION_OPENED_SPAN_CTX = "io.quarkus.websockets.next.connection-opened-span-ctx";

    private final Tracer tracer;
    private final String path;
    private final Map<String, Object> contextData;
    private final SpanKind spanKind;

    TracesConnectionInterceptor(Tracer tracer, SpanKind spanKind, String path) {
        this.tracer = tracer;
        this.path = path;
        this.contextData = new HashMap<>();
        this.spanKind = spanKind;
    }

    @Override
    public void connectionOpened() {
        var span = tracer.spanBuilder("OPEN " + path)
                .setSpanKind(spanKind)
                .addLink(previousSpanContext())
                .setAttribute(UrlAttributes.URL_PATH, path)
                .startSpan();
        try (var ignored = span.makeCurrent()) {
            contextData.put(CONNECTION_OPENED_SPAN_CTX, span.getSpanContext());
        } finally {
            span.end();
        }
    }

    @Override
    public void connectionOpeningFailed(Throwable cause) {
        tracer.spanBuilder("OPEN " + path)
                .setSpanKind(spanKind)
                .addLink((SpanContext) contextData.get(CONNECTION_OPENED_SPAN_CTX))
                .setAttribute(UrlAttributes.URL_PATH, path)
                .startSpan()
                .recordException(cause)
                .end();
    }

    @Override
    public Map<String, Object> getContextData() {
        return contextData;
    }

    private static SpanContext previousSpanContext() {
        var span = Span.current();
        if (span.getSpanContext().isValid()) {
            return span.getSpanContext();
        }
        return null;
    }
}
