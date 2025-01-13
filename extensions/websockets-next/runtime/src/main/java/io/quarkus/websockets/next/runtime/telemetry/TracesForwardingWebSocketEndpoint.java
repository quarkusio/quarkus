package io.quarkus.websockets.next.runtime.telemetry;

import java.util.function.Function;

import io.quarkus.websockets.next.runtime.WebSocketEndpoint;
import io.quarkus.websockets.next.runtime.spi.telemetry.WebSocketEndpointContext;
import io.quarkus.websockets.next.runtime.spi.telemetry.WebSocketTracesInterceptor;
import io.vertx.core.Future;
import io.vertx.core.Handler;

/**
 * {@link WebSocketEndpoint} wrapper that produces OpenTelemetry spans for closed connection.
 */
final class TracesForwardingWebSocketEndpoint extends ForwardingWebSocketEndpoint {

    private final WebSocketTracesInterceptor tracesInterceptor;
    private final WebSocketEndpointContext endpointContext;

    TracesForwardingWebSocketEndpoint(WebSocketEndpoint delegate, WebSocketTracesInterceptor tracesInterceptor,
            WebSocketEndpointContext endpointContext) {
        super(delegate);
        this.tracesInterceptor = tracesInterceptor;
        this.endpointContext = endpointContext;
    }

    @Override
    public Future<Void> onClose() {
        return delegate.onClose().map(new Function<Void, Void>() {
            @Override
            public Void apply(Void unused) {
                tracesInterceptor.onConnectionClosed(endpointContext);
                return null;
            }
        }).onFailure(new Handler<Throwable>() {
            @Override
            public void handle(Throwable throwable) {
                tracesInterceptor.onConnectionClosingFailed(throwable, endpointContext);
            }
        });
    }
}
