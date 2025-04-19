package io.quarkus.websockets.next.runtime.telemetry;

import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkus.websockets.next.runtime.WebSocketEndpoint;
import io.quarkus.websockets.next.runtime.config.WebSocketsClientRuntimeConfig;
import io.quarkus.websockets.next.runtime.config.WebSocketsServerRuntimeConfig;
import io.quarkus.websockets.next.runtime.spi.telemetry.EndpointKind;
import io.quarkus.websockets.next.runtime.spi.telemetry.WebSocketTracesInterceptor;

/**
 * Installs traces support into the WebSockets extension.
 */
public final class TracesBuilderCustomizer implements Consumer<WebSocketTelemetryProviderBuilder> {

    @Inject
    WebSocketsServerRuntimeConfig serverRuntimeConfig;

    @Inject
    WebSocketsClientRuntimeConfig clientRuntimeConfig;

    @Inject
    Instance<WebSocketTracesInterceptor> tracesInterceptorInstance;

    @Override
    public void accept(WebSocketTelemetryProviderBuilder builder) {
        if (!tracesInterceptorInstance.isResolvable()) {
            return;
        }
        var serverTracesEnabled = serverRuntimeConfig.telemetry().tracesEnabled();
        var clientTracesEnabled = clientRuntimeConfig.telemetry().tracesEnabled();
        if (serverTracesEnabled || clientTracesEnabled) {
            final WebSocketTracesInterceptor tracesInterceptor = this.tracesInterceptorInstance.get();
            if (serverTracesEnabled) {
                addServerTracesSupport(builder, tracesInterceptor);
            }
            if (clientTracesEnabled) {
                addClientTracesSupport(builder, tracesInterceptor);
            }
        }
    }

    private static void addServerTracesSupport(WebSocketTelemetryProviderBuilder builder,
            WebSocketTracesInterceptor tracesInterceptor) {
        builder.serverEndpointDecorator(new Function<>() {
            @Override
            public WebSocketEndpoint apply(TelemetryWebSocketEndpointContext ctx) {
                return new TracesForwardingWebSocketEndpoint(ctx.endpoint(), tracesInterceptor, ctx.forServer());
            }
        });
        builder.pathToServerConnectionInterceptor(new Function<>() {
            @Override
            public ConnectionInterceptor apply(String path) {
                return new TracesConnectionInterceptor(tracesInterceptor, path, EndpointKind.SERVER);
            }
        });
    }

    private static void addClientTracesSupport(WebSocketTelemetryProviderBuilder builder,
            WebSocketTracesInterceptor tracesInterceptor) {
        builder.clientEndpointDecorator(new Function<>() {
            @Override
            public WebSocketEndpoint apply(TelemetryWebSocketEndpointContext ctx) {
                return new TracesForwardingWebSocketEndpoint(ctx.endpoint(), tracesInterceptor, ctx.forClient());
            }
        });
        builder.pathToClientConnectionInterceptor(new Function<>() {
            @Override
            public ConnectionInterceptor apply(String path) {
                return new TracesConnectionInterceptor(tracesInterceptor, path, EndpointKind.CLIENT);
            }
        });
    }

}
