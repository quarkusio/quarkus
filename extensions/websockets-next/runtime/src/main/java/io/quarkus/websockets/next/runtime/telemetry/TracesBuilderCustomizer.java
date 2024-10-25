package io.quarkus.websockets.next.runtime.telemetry;

import static io.quarkus.websockets.next.runtime.telemetry.TracesConnectionInterceptor.CONNECTION_OPENED_SPAN_CTX;

import java.util.function.Consumer;
import java.util.function.Function;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.arc.Arc;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.WebSocketsClientRuntimeConfig;
import io.quarkus.websockets.next.WebSocketsServerRuntimeConfig;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint;

/**
 * Installs traces support into the WebSockets extension.
 */
public final class TracesBuilderCustomizer implements Consumer<WebsocketTelemetryProviderBuilder> {

    @Override
    public void accept(WebsocketTelemetryProviderBuilder builder) {
        var container = Arc.container();

        var serverTracesEnabled = container.instance(WebSocketsServerRuntimeConfig.class).get().telemetry().tracesEnabled();
        var clientTracesEnabled = container.instance(WebSocketsClientRuntimeConfig.class).get().telemetry().tracesEnabled();
        if (serverTracesEnabled || clientTracesEnabled) {
            final Tracer tracer = container.instance(Tracer.class).get();
            if (serverTracesEnabled) {
                addServerTracesSupport(builder, tracer);
            }
            if (clientTracesEnabled) {
                addClientTracesSupport(builder, tracer);
            }
        }
    }

    private static void addServerTracesSupport(WebsocketTelemetryProviderBuilder builder, Tracer tracer) {
        builder.serverEndpointDecorator(new Function<>() {
            @Override
            public WebSocketEndpoint apply(TelemetryWebSocketEndpointContext ctx) {
                var onOpenSpanCtx = (SpanContext) ctx.contextData().get(CONNECTION_OPENED_SPAN_CTX);
                return new TracesWebSocketEndpointWrapper(ctx.endpoint(), tracer, (WebSocketConnection) ctx.connection(),
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

    private static void addClientTracesSupport(WebsocketTelemetryProviderBuilder builder, Tracer tracer) {
        builder.clientEndpointDecorator(new Function<>() {
            @Override
            public WebSocketEndpoint apply(TelemetryWebSocketEndpointContext ctx) {
                var onOpenSpanCtx = (SpanContext) ctx.contextData().get(CONNECTION_OPENED_SPAN_CTX);
                return new TracesWebSocketEndpointWrapper(ctx.endpoint(), tracer, (WebSocketClientConnection) ctx.connection(),
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
