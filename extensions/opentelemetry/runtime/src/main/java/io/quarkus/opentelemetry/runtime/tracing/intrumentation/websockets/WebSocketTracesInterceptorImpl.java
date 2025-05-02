package io.quarkus.opentelemetry.runtime.tracing.intrumentation.websockets;

import java.util.Map;

import jakarta.enterprise.context.Dependent;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.semconv.UrlAttributes;
import io.quarkus.websockets.next.runtime.spi.telemetry.EndpointKind;
import io.quarkus.websockets.next.runtime.spi.telemetry.WebSocketEndpointContext;
import io.quarkus.websockets.next.runtime.spi.telemetry.WebSocketTracesInterceptor;

@Dependent
public final class WebSocketTracesInterceptorImpl implements WebSocketTracesInterceptor {

    /**
     * OpenTelemetry attributes added to spans created for opened and closed connections.
     */
    public static final String CONNECTION_ID_ATTR_KEY = "connection.id";
    public static final String CONNECTION_ENDPOINT_ATTR_KEY = "connection.endpoint.id";
    public static final String CONNECTION_CLIENT_ATTR_KEY = "connection.client.id";
    private static final String CONNECTION_OPENED_SPAN_CTX = "io.quarkus.opentelemetry.ws.connection-opened-span-ctx";

    private final Tracer tracer;

    WebSocketTracesInterceptorImpl(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Map<String, Object> onConnectionOpened(String route, EndpointKind endpointKind) {
        var span = tracer.spanBuilder("OPEN " + route)
                .setSpanKind(getSpanKind(endpointKind))
                .addLink(previousSpanContext())
                .setAttribute(UrlAttributes.URL_PATH, route)
                .startSpan();
        try (var ignored = span.makeCurrent()) {
            return Map.of(CONNECTION_OPENED_SPAN_CTX, span.getSpanContext());
        } finally {
            span.end();
        }
    }

    @Override
    public void onConnectionOpeningFailed(Throwable cause, String route, EndpointKind endpointKind,
            Map<String, Object> connectionOpenedContext) {
        tracer.spanBuilder("OPEN " + route)
                .setSpanKind(getSpanKind(endpointKind))
                .addLink(getOnOpenSpanContext(connectionOpenedContext))
                .setAttribute(UrlAttributes.URL_PATH, route)
                .startSpan()
                .recordException(cause)
                .end();
    }

    @Override
    public void onConnectionClosed(WebSocketEndpointContext ctx) {
        tracer.spanBuilder("CLOSE " + ctx.route())
                .setSpanKind(getSpanKind(ctx))
                .addLink(getOnOpenSpanContext(ctx))
                .setAttribute(CONNECTION_ID_ATTR_KEY, ctx.connectionId())
                .setAttribute(UrlAttributes.URL_PATH, ctx.route())
                .setAttribute(getTargetIdKey(ctx), ctx.targetId())
                .startSpan()
                .end();
    }

    @Override
    public void onConnectionClosingFailed(Throwable throwable, WebSocketEndpointContext ctx) {
        tracer.spanBuilder("CLOSE " + ctx.route())
                .setSpanKind(getSpanKind(ctx))
                .addLink(getOnOpenSpanContext(ctx))
                .setAttribute(CONNECTION_ID_ATTR_KEY, ctx.connectionId())
                .setAttribute(UrlAttributes.URL_PATH, ctx.route())
                .setAttribute(getTargetIdKey(ctx), ctx.targetId())
                .startSpan()
                .recordException(throwable)
                .end();
    }

    private static SpanContext getOnOpenSpanContext(WebSocketEndpointContext ctx) {
        return getOnOpenSpanContext(ctx.connectionContextStorage());
    }

    private static SpanContext getOnOpenSpanContext(Map<String, Object> connectionContextStorage) {
        if (connectionContextStorage == null) {
            return null;
        }
        return (SpanContext) connectionContextStorage.get(CONNECTION_OPENED_SPAN_CTX);
    }

    private static String getTargetIdKey(WebSocketEndpointContext ctx) {
        return getTargetIdKey(ctx.endpointKind());
    }

    private static String getTargetIdKey(EndpointKind endpointKind) {
        return switch (endpointKind) {
            case CLIENT -> CONNECTION_CLIENT_ATTR_KEY;
            case SERVER -> CONNECTION_ENDPOINT_ATTR_KEY;
        };
    }

    private static SpanKind getSpanKind(WebSocketEndpointContext context) {
        return getSpanKind(context.endpointKind());
    }

    private static SpanKind getSpanKind(EndpointKind endpointKind) {
        return switch (endpointKind) {
            case CLIENT -> SpanKind.CLIENT;
            case SERVER -> SpanKind.SERVER;
        };
    }

    private static SpanContext previousSpanContext() {
        var span = Span.current();
        if (span.getSpanContext().isValid()) {
            return span.getSpanContext();
        }
        return null;
    }
}
