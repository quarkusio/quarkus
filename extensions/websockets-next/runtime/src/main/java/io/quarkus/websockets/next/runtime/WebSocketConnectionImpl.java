package io.quarkus.websockets.next.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.runtime.telemetry.SendingInterceptor;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocketBase;
import io.vertx.ext.web.RoutingContext;

class WebSocketConnectionImpl extends WebSocketConnectionBase implements WebSocketConnection {

    private final String generatedEndpointClass;

    private final String endpointId;

    private final ServerWebSocket webSocket;

    private final ConnectionManager connectionManager;

    private final BroadcastSender defaultBroadcast;

    private final SecuritySupport securitySupport;

    WebSocketConnectionImpl(String generatedEndpointClass, String endpointClass, ServerWebSocket webSocket,
            ConnectionManager connectionManager, Codecs codecs, RoutingContext ctx,
            TrafficLogger trafficLogger, SendingInterceptor sendingInterceptor,
            Function<WebSocketConnectionImpl, SecuritySupport> securitySupportCreator) {
        super(Map.copyOf(ctx.pathParams()), codecs, new HandshakeRequestImpl(webSocket, ctx), trafficLogger,
                new UserDataImpl(), sendingInterceptor);
        this.generatedEndpointClass = generatedEndpointClass;
        this.endpointId = endpointClass;
        this.webSocket = Objects.requireNonNull(webSocket);
        this.connectionManager = Objects.requireNonNull(connectionManager);
        this.defaultBroadcast = new BroadcastImpl(null);
        this.securitySupport = securitySupportCreator.apply(this);
    }

    SecuritySupport securitySupport() {
        return securitySupport;
    }

    @Override
    WebSocketBase webSocket() {
        return webSocket;
    }

    @Override
    public String endpointId() {
        return endpointId;
    }

    @Override
    public BroadcastSender broadcast() {
        return defaultBroadcast;
    }

    @Override
    public Set<WebSocketConnection> getOpenConnections() {
        return connectionManager.getConnections(generatedEndpointClass).stream().filter(WebSocketConnection::isOpen)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public String toString() {
        return "WebSocket connection [endpointId=" + endpointId + ", path=" + webSocket.path() + ", id=" + identifier + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WebSocketConnectionImpl other = (WebSocketConnectionImpl) obj;
        return Objects.equals(identifier, other.identifier);
    }

    private static class HandshakeRequestImpl implements HandshakeRequest {

        private final ServerWebSocket webSocket;

        private final Map<String, List<String>> headers;

        HandshakeRequestImpl(ServerWebSocket webSocket, RoutingContext ctx) {
            this.webSocket = webSocket;
            this.headers = initHeaders(ctx);
        }

        @Override
        public String header(String name) {
            List<String> values = headers(name);
            return values.isEmpty() ? null : values.get(0);
        }

        @Override
        public List<String> headers(String name) {
            return headers.getOrDefault(Objects.requireNonNull(name).toLowerCase(), List.of());
        }

        @Override
        public Map<String, List<String>> headers() {
            return headers;
        }

        @Override
        public String scheme() {
            return webSocket.scheme();
        }

        @Override
        public String host() {
            return webSocket.authority().host();
        }

        @Override
        public int port() {
            return webSocket.authority().port();
        }

        @Override
        public String path() {
            return webSocket.path();
        }

        @Override
        public String query() {
            return webSocket.query();
        }

        static Map<String, List<String>> initHeaders(RoutingContext ctx) {
            Map<String, List<String>> headers = new HashMap<>();
            for (Entry<String, String> e : ctx.request().headers()) {
                String key = e.getKey().toLowerCase();
                List<String> values = headers.get(key);
                if (values == null) {
                    values = new ArrayList<>();
                    headers.put(key, values);
                }
                values.add(e.getValue());
            }
            for (Entry<String, List<String>> e : headers.entrySet()) {
                // Make the list of values immutable
                e.setValue(List.copyOf(e.getValue()));
            }
            return Map.copyOf(headers);
        }

    }

    private class BroadcastImpl implements WebSocketConnection.BroadcastSender {

        private static final BiFunction<WebSocketConnection, String, Uni<Void>> SEND_TEXT_STR = new BiFunction<>() {
            @Override
            public Uni<Void> apply(WebSocketConnection c, String s) {
                return c.sendText(s);
            }
        };
        private static final BiFunction<WebSocketConnection, Object, Uni<Void>> SEND_TEXT_POJO = new BiFunction<>() {
            @Override
            public Uni<Void> apply(WebSocketConnection c, Object o) {
                return c.sendText(o);
            }
        };
        private static final BiFunction<WebSocketConnection, Buffer, Uni<Void>> SEND_BINARY = new BiFunction<>() {
            @Override
            public Uni<Void> apply(WebSocketConnection c, Buffer b) {
                return c.sendBinary(b);
            }
        };

        private final Predicate<WebSocketConnection> filter;

        BroadcastImpl(Predicate<WebSocketConnection> filter) {
            this.filter = filter;
        }

        @Override
        public BroadcastSender filter(Predicate<WebSocketConnection> predicate) {
            return new BroadcastImpl(Objects.requireNonNull(predicate));
        }

        @Override
        public Uni<Void> sendText(String message) {
            return doSend(SEND_TEXT_STR, message);
        }

        @Override
        public <M> Uni<Void> sendText(M message) {
            return doSend(SEND_TEXT_POJO, message);
        }

        @Override
        public Uni<Void> sendBinary(Buffer message) {
            return doSend(SEND_BINARY, message);
        }

        @Override
        public Uni<Void> sendPing(Buffer data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Uni<Void> sendPong(Buffer data) {
            throw new UnsupportedOperationException();
        }

        private <M> Uni<Void> doSend(BiFunction<WebSocketConnection, M, Uni<Void>> sendFunction, M message) {
            Set<WebSocketConnection> connections = connectionManager.getConnections(generatedEndpointClass);
            if (connections.isEmpty()) {
                return Uni.createFrom().voidItem();
            }
            List<Uni<Void>> unis = new ArrayList<>(connections.size());
            for (WebSocketConnection connection : connections) {
                if (connection.isOpen()
                        && (filter == null || filter.test(connection))) {
                    unis.add(sendFunction.apply(connection, message)
                            // Intentionally ignore 'WebSocket is closed' failures
                            // It might happen that the connection is closed in the mean time
                            .onFailure(t -> Endpoints.isWebSocketIsClosedFailure(t, (WebSocketConnectionBase) connection))
                            .recoverWithNull());
                }
            }
            if (unis.isEmpty()) {
                return Uni.createFrom().voidItem();
            }
            return Uni.join().all(unis).andCollectFailures().replaceWithVoid();
        }

    }

}
