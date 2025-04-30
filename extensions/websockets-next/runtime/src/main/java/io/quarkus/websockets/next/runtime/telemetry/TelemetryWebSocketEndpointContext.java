package io.quarkus.websockets.next.runtime.telemetry;

import java.util.Map;

import io.quarkus.websockets.next.runtime.WebSocketConnectionBase;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint;

/**
 * Data carrier used to instantiate {@link TelemetrySupport}.
 */
record TelemetryWebSocketEndpointContext(WebSocketEndpoint endpoint, WebSocketConnectionBase connection, String path,
        Map<String, Object> contextData) {
}
