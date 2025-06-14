package io.quarkus.websockets.next.runtime;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Consumer;

import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.runtime.telemetry.SendingInterceptor;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketBase;

class WebSocketClientConnectionImpl extends WebSocketConnectionBase implements WebSocketClientConnection {

    private final String clientId;

    private final WebSocket webSocket;

    private final Consumer<WebSocketClientConnection> cleanup;

    WebSocketClientConnectionImpl(String clientId, WebSocket webSocket, Codecs codecs,
            Map<String, String> pathParams, URI serverEndpointUri, Map<String, List<String>> headers,
            TrafficLogger trafficLogger, Map<String, Object> userData, SendingInterceptor sendingInterceptor,
            Consumer<WebSocketClientConnection> cleanup) {
        super(Map.copyOf(pathParams), codecs, new ClientHandshakeRequestImpl(serverEndpointUri, headers), trafficLogger,
                new UserDataImpl(userData), sendingInterceptor);
        this.clientId = clientId;
        this.webSocket = Objects.requireNonNull(webSocket);
        this.cleanup = cleanup;
    }

    @Override
    WebSocketBase webSocket() {
        return webSocket;
    }

    @Override
    public String clientId() {
        return clientId;
    }

    @Override
    public String toString() {
        return "WebSocket client connection [id=" + identifier + ", clientId=" + clientId + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    protected void cleanup() {
        if (cleanup != null) {
            cleanup.accept(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WebSocketClientConnectionImpl other = (WebSocketClientConnectionImpl) obj;
        return Objects.equals(identifier, other.identifier);
    }

    private static class ClientHandshakeRequestImpl implements HandshakeRequest {

        private final URI serverEndpointUrl;
        private final Map<String, List<String>> headers;

        ClientHandshakeRequestImpl(URI serverEndpointUrl, Map<String, List<String>> headers) {
            this.serverEndpointUrl = serverEndpointUrl;
            Map<String, List<String>> copy = new HashMap<>();
            for (Entry<String, List<String>> e : headers.entrySet()) {
                copy.put(e.getKey().toLowerCase(), List.copyOf(e.getValue()));
            }
            this.headers = copy;
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
            return serverEndpointUrl.getScheme();
        }

        @Override
        public String host() {
            return serverEndpointUrl.getHost();
        }

        @Override
        public int port() {
            return serverEndpointUrl.getPort();
        }

        @Override
        public String path() {
            return serverEndpointUrl.getPath();
        }

        @Override
        public String query() {
            return serverEndpointUrl.getQuery();
        }

    }

}
