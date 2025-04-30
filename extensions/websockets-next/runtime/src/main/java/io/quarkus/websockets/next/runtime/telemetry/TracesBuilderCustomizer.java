package io.quarkus.websockets.next.runtime.telemetry;

import static io.quarkus.websockets.next.runtime.telemetry.TracesConnectionInterceptor.CONNECTION_OPENED_SPAN_CTX;

import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint;
import io.quarkus.websockets.next.runtime.config.WebSocketsClientRuntimeConfig;
import io.quarkus.websockets.next.runtime.config.WebSocketsServerRuntimeConfig;

/**
 * Installs traces support into the WebSockets extension.
 */
public final class TracesBuilderCustomizer implements Consumer<WebSocketTelemetryProviderBuilder> {

    @Inject
    WebSocketsServerRuntimeConfig serverRuntimeConfig;

    @Inject
    WebSocketsClientRuntimeConfig clientRuntimeConfig;

    @Inject
    Instance<Tracer> tracerInstance;

    @Override
    public void accept(WebSocketTelemetryProviderBuilder builder) {
        var serverTracesEnabled = serverRuntimeConfig.telemetry().tracesEnabled();
        var clientTracesEnabled = clientRuntimeConfig.telemetry().tracesEnabled();
        if (serverTracesEnabled || clientTracesEnabled) {
            final Tracer tracer = tracerInstance.get();
            if (serverTracesEnabled) {
                addServerTracesSupport(builder, tracer);
            }
            if (clientTracesEnabled) {
                addClientTracesSupport(builder, tracer);
            }
        }
    }

    private static void addServerTracesSupport(WebSocketTelemetryProviderBuilder builder, Tracer tracer) {
        builder.serverEndpointDecorator(new Function<>() {
            @Override
            public WebSocketEndpoint apply(TelemetryWebSocketEndpointContext ctx) {
                var onOpenSpanCtx = (SpanContext) ctx.contextData().get(CONNECTION_OPENED_SPAN_CTX);
                return new TracesForwardingWebSocketEndpoint(ctx.endpoint(), tracer, (WebSocketConnection) ctx.connection(),
                        onOpenSpanCtx, ctx.path());
            }
        });
        builder.pathToServerConnectionInterceptor(new Function<>() {
            @Override
            public ConnectionInterceptor apply(String path) {
                return new TracesConnectionInterceptor(tracer, SpanKind.SERVER, path);
            }
        });
    }

    private static void addClientTracesSupport(WebSocketTelemetryProviderBuilder builder, Tracer tracer) {
        builder.clientEndpointDecorator(new Function<>() {
            @Override
            public WebSocketEndpoint apply(TelemetryWebSocketEndpointContext ctx) {
                var onOpenSpanCtx = (SpanContext) ctx.contextData().get(CONNECTION_OPENED_SPAN_CTX);
                return new TracesForwardingWebSocketEndpoint(ctx.endpoint(), tracer,
                        (WebSocketClientConnection) ctx.connection(),
                        onOpenSpanCtx, ctx.path());
            }
        });
        builder.pathToClientConnectionInterceptor(new Function<>() {
            @Override
            public ConnectionInterceptor apply(String path) {
                return new TracesConnectionInterceptor(tracer, SpanKind.CLIENT, path);
            }
        });
    }

}
