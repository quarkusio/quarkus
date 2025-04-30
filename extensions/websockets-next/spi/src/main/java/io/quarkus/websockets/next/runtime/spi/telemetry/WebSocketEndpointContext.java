package io.quarkus.websockets.next.runtime.spi.telemetry;

import java.util.Map;

public record WebSocketEndpointContext(String route, EndpointKind endpointKind, Map<String, Object> connectionContextStorage,
        String connectionId, String targetId) {
}
