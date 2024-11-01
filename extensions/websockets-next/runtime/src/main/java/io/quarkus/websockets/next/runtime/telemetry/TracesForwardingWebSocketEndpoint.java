package io.quarkus.websockets.next.runtime.telemetry;

import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CONNECTION_CLIENT_ATTR_KEY;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CONNECTION_ENDPOINT_ATTR_KEY;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CONNECTION_ID_ATTR_KEY;

import java.util.function.Function;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.semconv.UrlAttributes;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint;
import io.vertx.core.Future;
import io.vertx.core.Handler;

/**
 * {@link WebSocketEndpoint} wrapper that produces OpenTelemetry spans for closed connection.
 */
final class TracesForwardingWebSocketEndpoint extends ForwardingWebSocketEndpoint {

    /**
     * Target ID represents either endpoint id or client id.
     */
    private final String targetIdKey;
    private final String targetIdValue;
    /**
     * Span context for an HTTP request used to establish the WebSocket connection.
     */
    private final Tracer tracer;
    private final String connectionId;
    private final String path;
    private final SpanContext onOpenSpanContext;
    private final SpanKind spanKind;

    TracesForwardingWebSocketEndpoint(WebSocketEndpoint delegate, Tracer tracer, WebSocketConnection connection,
            SpanContext onOpenSpanContext, String path) {
        this(delegate, tracer, onOpenSpanContext, path, CONNECTION_ENDPOINT_ATTR_KEY, SpanKind.SERVER, connection.endpointId(),
                connection.id());
    }

    TracesForwardingWebSocketEndpoint(WebSocketEndpoint delegate, Tracer tracer, WebSocketClientConnection connection,
            SpanContext onOpenSpanContext, String path) {
        this(delegate, tracer, onOpenSpanContext, path, CONNECTION_CLIENT_ATTR_KEY, SpanKind.CLIENT, connection.clientId(),
                connection.id());
    }

    private TracesForwardingWebSocketEndpoint(WebSocketEndpoint delegate, Tracer tracer, SpanContext onOpenSpanContext,
            String path, String targetIdKey, SpanKind spanKind, String targetIdValue, String connectionId) {
        super(delegate);
        this.tracer = tracer;
        this.onOpenSpanContext = onOpenSpanContext;
        this.connectionId = connectionId;
        this.targetIdKey = targetIdKey;
        this.targetIdValue = targetIdValue;
        this.path = path;
        this.spanKind = spanKind;
    }

    @Override
    public Future<Void> onClose() {
        return delegate.onClose().map(new Function<Void, Void>() {
            @Override
            public Void apply(Void unused) {
                tracer.spanBuilder("CLOSE " + path)
                        .setSpanKind(spanKind)
                        .addLink(onOpenSpanContext)
                        .setAttribute(CONNECTION_ID_ATTR_KEY, connectionId)
                        .setAttribute(UrlAttributes.URL_PATH, path)
                        .setAttribute(targetIdKey, targetIdValue)
                        .startSpan()
                        .end();
                return null;
            }
        }).onFailure(new Handler<Throwable>() {
            @Override
            public void handle(Throwable throwable) {
                tracer.spanBuilder("CLOSE " + path)
                        .setSpanKind(spanKind)
                        .addLink(onOpenSpanContext)
                        .setAttribute(CONNECTION_ID_ATTR_KEY, connectionId)
                        .setAttribute(UrlAttributes.URL_PATH, path)
                        .setAttribute(targetIdKey, targetIdValue)
                        .startSpan()
                        .recordException(throwable)
                        .end();
            }
        });
    }
}
