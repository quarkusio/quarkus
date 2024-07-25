package io.quarkus.websockets.next.runtime.telemetry;

import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CLIENT_CONNECTION_CLOSED;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CONNECTION_CLIENT_ATTR_KEY;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CONNECTION_ENDPOINT_ATTR_KEY;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.CONNECTION_ID_ATTR_KEY;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.SERVER_CONNECTION_CLOSED;
import static io.quarkus.websockets.next.runtime.telemetry.TelemetryConstants.URI_ATTR_KEY;

import java.util.function.Function;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint;
import io.vertx.core.Future;

/**
 * {@link WebSocketEndpoint} wrapper that produces OpenTelemetry spans for closed connection.
 */
final class TracesWebSocketEndpointWrapper extends AbstractWebSocketEndpointWrapper {

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
    private final String connectionClosedSpanName;

    TracesWebSocketEndpointWrapper(WebSocketEndpoint delegate, Tracer tracer, WebSocketConnection connection,
            SpanContext onOpenSpanContext, String path) {
        super(delegate);
        this.tracer = tracer;
        this.onOpenSpanContext = onOpenSpanContext;
        this.connectionId = connection.id();
        this.targetIdKey = CONNECTION_ENDPOINT_ATTR_KEY;
        this.targetIdValue = connection.endpointId();
        this.path = path;
        this.connectionClosedSpanName = SERVER_CONNECTION_CLOSED;
    }

    TracesWebSocketEndpointWrapper(WebSocketEndpoint delegate, Tracer tracer, WebSocketClientConnection connection,
            SpanContext onOpenSpanContext, String path) {
        super(delegate);
        this.tracer = tracer;
        this.onOpenSpanContext = onOpenSpanContext;
        this.connectionId = connection.id();
        this.targetIdKey = CONNECTION_CLIENT_ATTR_KEY;
        this.targetIdValue = connection.clientId();
        this.path = path;
        this.connectionClosedSpanName = CLIENT_CONNECTION_CLOSED;
    }

    @Override
    public Future<Void> onClose() {
        return delegate.onClose().map(new Function<Void, Void>() {
            @Override
            public Void apply(Void unused) {
                tracer.spanBuilder(connectionClosedSpanName)
                        .addLink(onOpenSpanContext)
                        .setAttribute(CONNECTION_ID_ATTR_KEY, connectionId)
                        .setAttribute(URI_ATTR_KEY, path)
                        .setAttribute(targetIdKey, targetIdValue)
                        .startSpan()
                        .end();
                return null;
            }
        });
    }
}
