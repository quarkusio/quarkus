package io.quarkus.websockets.next.runtime.telemetry;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import io.quarkus.websockets.next.runtime.WebSocketEndpoint;
import io.quarkus.websockets.next.runtime.telemetry.ConnectionInterceptor.CompositeConnectionInterceptor;

/**
 * Quarkus uses this class internally to build {@link WebSocketTelemetryProvider}.
 */
public final class WebSocketTelemetryProviderBuilder {

    private Function<String, ConnectionInterceptor> pathToClientConnectionInterceptor;
    private Function<String, ConnectionInterceptor> pathToServerConnectionInterceptor;
    private Function<TelemetryWebSocketEndpointContext, WebSocketEndpoint> serverEndpointDecorator;
    private Function<TelemetryWebSocketEndpointContext, WebSocketEndpoint> clientEndpointDecorator;

    WebSocketTelemetryProviderBuilder() {
        serverEndpointDecorator = null;
        clientEndpointDecorator = null;
    }

    void clientEndpointDecorator(Function<TelemetryWebSocketEndpointContext, WebSocketEndpoint> decorator) {
        Objects.requireNonNull(decorator);
        if (this.clientEndpointDecorator == null) {
            this.clientEndpointDecorator = decorator;
        } else {
            this.clientEndpointDecorator = this.clientEndpointDecorator
                    .compose(new Function<TelemetryWebSocketEndpointContext, TelemetryWebSocketEndpointContext>() {
                        @Override
                        public TelemetryWebSocketEndpointContext apply(TelemetryWebSocketEndpointContext ctx) {
                            var decorated = decorator.apply(ctx);
                            return new TelemetryWebSocketEndpointContext(decorated, ctx.connection(), ctx.path(),
                                    ctx.contextData());
                        }
                    });
        }
    }

    void serverEndpointDecorator(Function<TelemetryWebSocketEndpointContext, WebSocketEndpoint> decorator) {
        Objects.requireNonNull(decorator);
        if (this.serverEndpointDecorator == null) {
            this.serverEndpointDecorator = decorator;
        } else {
            this.serverEndpointDecorator = this.serverEndpointDecorator
                    .compose(new Function<TelemetryWebSocketEndpointContext, TelemetryWebSocketEndpointContext>() {
                        @Override
                        public TelemetryWebSocketEndpointContext apply(TelemetryWebSocketEndpointContext ctx) {
                            var decorated = decorator.apply(ctx);
                            return new TelemetryWebSocketEndpointContext(decorated, ctx.connection(), ctx.path(),
                                    ctx.contextData());
                        }
                    });
        }
    }

    void pathToClientConnectionInterceptor(Function<String, ConnectionInterceptor> pathToInterceptor1) {
        Objects.requireNonNull(pathToInterceptor1);
        if (this.pathToClientConnectionInterceptor == null) {
            this.pathToClientConnectionInterceptor = pathToInterceptor1;
        } else {
            var pathToInterceptor2 = this.pathToClientConnectionInterceptor;
            this.pathToClientConnectionInterceptor = new Function<>() {
                @Override
                public ConnectionInterceptor apply(String path) {
                    var interceptor1 = pathToInterceptor1.apply(path);
                    var interceptor2 = pathToInterceptor2.apply(path);
                    return new CompositeConnectionInterceptor(List.of(interceptor1, interceptor2));
                }
            };
        }
    }

    void pathToServerConnectionInterceptor(Function<String, ConnectionInterceptor> pathToInterceptor1) {
        Objects.requireNonNull(pathToInterceptor1);
        if (this.pathToServerConnectionInterceptor == null) {
            this.pathToServerConnectionInterceptor = pathToInterceptor1;
        } else {
            var pathToInterceptor2 = this.pathToServerConnectionInterceptor;
            this.pathToServerConnectionInterceptor = new Function<>() {
                @Override
                public ConnectionInterceptor apply(String path) {
                    var interceptor1 = pathToInterceptor1.apply(path);
                    var interceptor2 = pathToInterceptor2.apply(path);
                    return new CompositeConnectionInterceptor(List.of(interceptor1, interceptor2));
                }
            };
        }
    }

    WebSocketTelemetryProvider build() {
        return new WebSocketTelemetryProvider(serverEndpointDecorator, clientEndpointDecorator,
                pathToClientConnectionInterceptor, pathToServerConnectionInterceptor);
    }

}
