package io.quarkus.websockets.next.runtime.telemetry;

import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CONNECTION_FAILURE_ATTR_KEY;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.URI_ATTR_KEY;

import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;

final class TracesConnectionInterceptor implements ConnectionInterceptor {

    static final String CONNECTION_OPENED_SPAN_CTX = "io.quarkus.websockets.next.connection-opened-span-ctx";

    private final Tracer tracer;
    private final String connectionOpenedSpanName;
    private final String connectionOpeningFailedSpanName;
    private final String path;
    private final Map<String, Object> contextData;

    TracesConnectionInterceptor(Tracer tracer, String connectionOpenedSpanName, String connectionOpeningFailedSpanName,
            String path) {
        this.tracer = tracer;
        this.connectionOpenedSpanName = connectionOpenedSpanName;
        this.connectionOpeningFailedSpanName = connectionOpeningFailedSpanName;
        this.path = path;
        this.contextData = new HashMap<>();
    }

    @Override
    public void connectionOpened() {
        var span = tracer.spanBuilder(connectionOpenedSpanName)
                .addLink(previousSpanContext())
                .setAttribute(URI_ATTR_KEY, path)
                .startSpan();
        contextData.put(CONNECTION_OPENED_SPAN_CTX, span.getSpanContext());
        span.end();
    }

    @Override
    public void connectionOpeningFailed(Throwable cause) {
        tracer.spanBuilder(connectionOpeningFailedSpanName)
                .addLink((SpanContext) contextData.get(CONNECTION_OPENED_SPAN_CTX))
                .setAttribute(URI_ATTR_KEY, path)
                .setAttribute(CONNECTION_FAILURE_ATTR_KEY, cause.getMessage())
                .startSpan()
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
