package io.quarkus.websockets.next.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

class WebSocketConnectionImpl implements WebSocketConnection {

    private final String endpoint;

    private final String identifier;

    private final ServerWebSocket webSocket;

    private final ConnectionManager connectionManager;

    private final Codecs codecs;

    private final Map<String, String> pathParams;

    private final HandshakeRequest handshakeRequest;

    private final BroadcastSender defaultBroadcast;

    WebSocketConnectionImpl(String endpoint, ServerWebSocket webSocket, ConnectionManager connectionManager,
            Codecs codecs, RoutingContext ctx) {
        this.endpoint = endpoint;
        this.identifier = UUID.randomUUID().toString();
        this.webSocket = Objects.requireNonNull(webSocket);
        this.connectionManager = Objects.requireNonNull(connectionManager);
        this.pathParams = Map.copyOf(ctx.pathParams());
        this.defaultBroadcast = new BroadcastImpl(null);
        this.codecs = codecs;
        this.handshakeRequest = new HandshakeRequestImpl(ctx);
    }

    @Override
    public String id() {
        return identifier;
    }

    @Override
    public String pathParam(String name) {
        return pathParams.get(name);
    }

    @Override
    public Uni<Void> sendText(String message) {
        return UniHelper.toUni(webSocket.writeTextMessage(message));
    }

    @Override
    public Uni<Void> sendBinary(Buffer message) {
        return UniHelper.toUni(webSocket.writeBinaryMessage(message));
    }

    @Override
    public <M> Uni<Void> sendText(M message) {
        return UniHelper.toUni(webSocket.writeTextMessage(codecs.textEncode(message, null).toString()));
    }

    @Override
    public Uni<Void> sendPing(Buffer data) {
        return UniHelper.toUni(webSocket.writePing(data));
    }

    @Override
    public Uni<Void> sendPong(Buffer data) {
        return UniHelper.toUni(webSocket.writePong(data));
    }

    @Override
    public BroadcastSender broadcast() {
        return defaultBroadcast;
    }

    @Override
    public Uni<Void> close() {
        return UniHelper.toUni(webSocket.close());
    }

    @Override
    public boolean isSecure() {
        return webSocket.isSsl();
    }

    @Override
    public boolean isClosed() {
        return webSocket.isClosed();
    }

    @Override
    public Set<WebSocketConnection> getOpenConnections() {
        return connectionManager.getConnections(endpoint).stream().filter(WebSocketConnection::isOpen)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public HandshakeRequest handshakeRequest() {
        return handshakeRequest;
    }

    @Override
    public String toString() {
        return "WebSocket connection [id=" + identifier + ", path=" + webSocket.path() + "]";
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

    private class HandshakeRequestImpl implements HandshakeRequest {

        private final Map<String, List<String>> headers;

        HandshakeRequestImpl(RoutingContext ctx) {
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

        private <M> Uni<Void> doSend(BiFunction<WebSocketConnection, M, Uni<Void>> function, M message) {
            Set<WebSocketConnection> connections = connectionManager.getConnections(endpoint);
            if (connections.isEmpty()) {
                return Uni.createFrom().voidItem();
            }
            List<Uni<Void>> unis = new ArrayList<>(connections.size());
            for (WebSocketConnection connection : connections) {
                if (connection.isOpen() && (filter == null || filter.test(connection))) {
                    unis.add(function.apply(connection, message));
                }
            }
            return unis.isEmpty() ? Uni.createFrom().voidItem() : Uni.join().all(unis).andFailFast().replaceWithVoid();
        }

    }

}
