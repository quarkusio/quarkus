package io.quarkus.websockets.next.runtime.telemetry;

import java.util.Map;

import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.runtime.WebSocketConnectionBase;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint;
import io.quarkus.websockets.next.runtime.spi.telemetry.EndpointKind;
import io.quarkus.websockets.next.runtime.spi.telemetry.WebSocketEndpointContext;

/**
 * Data carrier used to instantiate {@link TelemetrySupport}.
 */
record TelemetryWebSocketEndpointContext(WebSocketEndpoint endpoint, WebSocketConnectionBase connection, String path,
        Map<String, Object> contextData) {

    WebSocketEndpointContext forClient() {
        WebSocketClientConnection clientConnection = (WebSocketClientConnection) connection;
        return new WebSocketEndpointContext(path, EndpointKind.CLIENT, contextData, connection.id(),
                clientConnection.clientId());
    }

    WebSocketEndpointContext forServer() {
        WebSocketConnection serverConnection = (WebSocketConnection) connection;
        return new WebSocketEndpointContext(path, EndpointKind.SERVER, contextData, connection.id(),
                serverConnection.endpointId());
    }
}
