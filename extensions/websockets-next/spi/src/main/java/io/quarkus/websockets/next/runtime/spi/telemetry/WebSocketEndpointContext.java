package io.quarkus.websockets.next.runtime.spi.telemetry;

import java.util.Map;

/**
 * A WebSocket endpoint contextual data needed when collecting WebSockets metrics and traces.
 *
 * @param route endpoint route
 * @param endpointKind endpoint kind (either client endpoint or server endpoint)
 * @param connectionContextStorage context data recorded when the WebSocket connection opened
 * @param connectionId the WebSocket connection id
 * @param targetId for a server endpoint, this is an endpoint id, while for a client endpoint, this is a client id
 */
public record WebSocketEndpointContext(String route, EndpointKind endpointKind, Map<String, Object> connectionContextStorage,
        String connectionId, String targetId) {
}
