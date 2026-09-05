package io.quarkus.websockets.next.runtime.produi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import io.quarkus.websockets.next.OpenConnections;
import io.quarkus.websockets.next.WebSocketConnection;

/**
 * Read-only Prod UI view of the registered WebSocket server endpoints. For each
 * endpoint it exposes the endpoint class, path, inbound execution mode, the
 * declared callbacks and the current number of active connections. The endpoint
 * metadata is seeded at runtime init (build-time data) and the live connection
 * count is derived from the always-present {@link OpenConnections} bean. The Dev
 * UI is not reused: its component is a message-injection console (opens Dev UI
 * connections and sends/clears messages) and pulls in dev-only web components.
 * This view offers no message injection and no connection management.
 */
@ApplicationScoped
public class WebSocketsNextProdUIService {

    @Inject
    OpenConnections openConnections;

    // Seeded at runtime init. Each entry holds: clazz, endpointId, path, executionMode, callbacks (List<Map>)
    private volatile List<Map<String, Object>> endpoints = List.of();

    public void setEndpoints(List<Map<String, Object>> endpoints) {
        this.endpoints = endpoints;
    }

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only list of the registered WebSocket server endpoints with their active connection count")
    public List<Map<String, Object>> getEndpoints() {
        Map<String, Long> countsByEndpointId = openConnections.listAll().stream()
                .collect(Collectors.groupingBy(WebSocketConnection::endpointId, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> endpoint : endpoints) {
            Map<String, Object> view = new LinkedHashMap<>(endpoint);
            view.put("connectionCount", countsByEndpointId.getOrDefault(endpoint.get("endpointId"), 0L));
            result.add(view);
        }
        return result;
    }
}
