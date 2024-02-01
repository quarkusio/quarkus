package io.quarkus.websockets.next.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.quarkus.websockets.next.WebSocketServerConnection;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;

class WebSocketServerConnectionImpl implements WebSocketServerConnection {

    private final String endpoint;

    private final String identifier;

    private final ServerWebSocket webSocket;

    private final ConnectionManager connectionManager;

    private final Codecs codecs;

    private final Map<String, String> pathParams;

    private final BroadcastSender defaultBroadcast;

    WebSocketServerConnectionImpl(String endpoint, ServerWebSocket webSocket, ConnectionManager connectionManager,
            Map<String, String> pathParams, Codecs codecs) {
        this.endpoint = endpoint;
        this.identifier = UUID.randomUUID().toString();
        this.webSocket = Objects.requireNonNull(webSocket);
        this.connectionManager = Objects.requireNonNull(connectionManager);
        this.pathParams = pathParams;
        this.defaultBroadcast = new BroadcastImpl(null);
        this.codecs = codecs;
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
    public BroadcastSender broadcast() {
        return defaultBroadcast;
    }

    @Override
    public BroadcastSender broadcast(Predicate<WebSocketServerConnection> filter) {
        return new BroadcastImpl(Objects.requireNonNull(filter));
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
    public Set<WebSocketServerConnection> getOpenConnections() {
        return connectionManager.getConnections(endpoint).stream().filter(WebSocketServerConnection::isOpen)
                .collect(Collectors.toUnmodifiableSet());
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
        WebSocketServerConnectionImpl other = (WebSocketServerConnectionImpl) obj;
        return Objects.equals(identifier, other.identifier);
    }

    private class BroadcastImpl implements WebSocketServerConnection.BroadcastSender {

        private final Predicate<WebSocketServerConnection> filter;

        BroadcastImpl(Predicate<WebSocketServerConnection> filter) {
            this.filter = filter;
        }

        @Override
        public Uni<Void> sendText(String message) {
            return doSend(new Function<WebSocketServerConnection, Uni<Void>>() {

                @Override
                public Uni<Void> apply(WebSocketServerConnection c) {
                    return c.sendText(message);
                }
            });
        }

        @Override
        public <M> Uni<Void> sendText(M message) {
            return doSend(new Function<WebSocketServerConnection, Uni<Void>>() {

                @Override
                public Uni<Void> apply(WebSocketServerConnection c) {
                    return c.sendText(message);
                }
            });
        }

        @Override
        public Uni<Void> sendBinary(Buffer message) {
            return doSend(new Function<WebSocketServerConnection, Uni<Void>>() {

                @Override
                public Uni<Void> apply(WebSocketServerConnection c) {
                    return c.sendBinary(message);
                }
            });
        }

        private Uni<Void> doSend(Function<WebSocketServerConnection, Uni<Void>> function) {
            List<Uni<Void>> unis = new ArrayList<>();
            for (WebSocketServerConnection connection : connectionManager.getConnections(endpoint)) {
                if (connection.isOpen() && (filter == null || filter.test(connection))) {
                    unis.add(function.apply(connection));
                }
            }
            return unis.isEmpty() ? Uni.createFrom().voidItem() : Uni.join().all(unis).andFailFast().replaceWithVoid();
        }

    }

}
