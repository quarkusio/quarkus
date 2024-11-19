package io.quarkus.websockets.next.runtime.telemetry;

import java.util.function.Function;

import io.quarkus.websockets.next.runtime.WebSocketConnectionBase;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint;

public final class WebSocketTelemetryProvider {

    private final Function<TelemetryWebSocketEndpointContext, WebSocketEndpoint> serverEndpointDecorator;
    private final Function<TelemetryWebSocketEndpointContext, WebSocketEndpoint> clientEndpointDecorator;
    private final Function<String, ConnectionInterceptor> pathToClientConnectionInterceptor;
    private final Function<String, ConnectionInterceptor> pathToServerConnectionInterceptor;
    private final Function<String, SendingInterceptor> pathToClientSendingInterceptor;
    private final Function<String, SendingInterceptor> pathToServerSendingInterceptor;
    private final Function<String, ErrorInterceptor> pathToClientErrorInterceptor;
    private final Function<String, ErrorInterceptor> pathToServerErrorInterceptor;
    private final boolean clientTelemetryEnabled;
    private final boolean serverTelemetryEnabled;

    WebSocketTelemetryProvider(Function<TelemetryWebSocketEndpointContext, WebSocketEndpoint> serverEndpointDecorator,
            Function<TelemetryWebSocketEndpointContext, WebSocketEndpoint> clientEndpointDecorator,
            Function<String, ConnectionInterceptor> pathToClientConnectionInterceptor,
            Function<String, ConnectionInterceptor> pathToServerConnectionInterceptor,
            Function<String, SendingInterceptor> pathToClientSendingInterceptor,
            Function<String, SendingInterceptor> pathToServerSendingInterceptor,
            Function<String, ErrorInterceptor> pathToClientErrorInterceptor,
            Function<String, ErrorInterceptor> pathToServerErrorInterceptor) {
        this.serverTelemetryEnabled = serverEndpointDecorator != null || pathToServerConnectionInterceptor != null
                || pathToServerSendingInterceptor != null || pathToServerErrorInterceptor != null;
        this.serverEndpointDecorator = serverEndpointDecorator;
        this.pathToServerConnectionInterceptor = pathToServerConnectionInterceptor;
        this.clientTelemetryEnabled = clientEndpointDecorator != null || pathToClientConnectionInterceptor != null
                || pathToClientSendingInterceptor != null || pathToClientErrorInterceptor != null;
        this.clientEndpointDecorator = clientEndpointDecorator;
        this.pathToClientConnectionInterceptor = pathToClientConnectionInterceptor;
        this.pathToClientSendingInterceptor = pathToClientSendingInterceptor;
        this.pathToServerSendingInterceptor = pathToServerSendingInterceptor;
        this.pathToClientErrorInterceptor = pathToClientErrorInterceptor;
        this.pathToServerErrorInterceptor = pathToServerErrorInterceptor;
    }

    /**
     * This method may only be called on the Vert.x context of the initial HTTP request as it collects context data.
     *
     * @param path endpoint path with path param placeholders
     * @return TelemetryDecorator
     */
    public TelemetrySupport createServerTelemetrySupport(String path) {
        if (serverTelemetryEnabled) {
            return new TelemetrySupport(getServerConnectionInterceptor(path), getServerSendingInterceptor(path),
                    getServerErrorInterceptor(path)) {
                @Override
                public WebSocketEndpoint decorate(WebSocketEndpoint endpoint, WebSocketConnectionBase connection) {
                    if (serverEndpointDecorator == null) {
                        return endpoint;
                    }
                    return serverEndpointDecorator
                            .apply(new TelemetryWebSocketEndpointContext(endpoint, connection, path, getContextData()));
                }
            };
        }
        return null;
    }

    public TelemetrySupport createClientTelemetrySupport(String path) {
        if (clientTelemetryEnabled) {
            return new TelemetrySupport(getClientConnectionInterceptor(path), getClientSendingInterceptor(path),
                    getClientErrorInterceptor(path)) {
                @Override
                public WebSocketEndpoint decorate(WebSocketEndpoint endpoint, WebSocketConnectionBase connection) {
                    if (clientEndpointDecorator == null) {
                        return endpoint;
                    }
                    return clientEndpointDecorator
                            .apply(new TelemetryWebSocketEndpointContext(endpoint, connection, path, getContextData()));
                }
            };
        }
        return null;
    }

    private ConnectionInterceptor getServerConnectionInterceptor(String path) {
        return pathToServerConnectionInterceptor == null ? null : pathToServerConnectionInterceptor.apply(path);
    }

    private ConnectionInterceptor getClientConnectionInterceptor(String path) {
        return pathToClientConnectionInterceptor == null ? null : pathToClientConnectionInterceptor.apply(path);
    }

    private ErrorInterceptor getServerErrorInterceptor(String path) {
        return pathToServerErrorInterceptor == null ? null : pathToServerErrorInterceptor.apply(path);
    }

    private SendingInterceptor getServerSendingInterceptor(String path) {
        return pathToServerSendingInterceptor == null ? null : pathToServerSendingInterceptor.apply(path);
    }

    private ErrorInterceptor getClientErrorInterceptor(String path) {
        return pathToClientErrorInterceptor == null ? null : pathToClientErrorInterceptor.apply(path);
    }

    private SendingInterceptor getClientSendingInterceptor(String path) {
        return pathToClientSendingInterceptor == null ? null : pathToClientSendingInterceptor.apply(path);
    }
}
