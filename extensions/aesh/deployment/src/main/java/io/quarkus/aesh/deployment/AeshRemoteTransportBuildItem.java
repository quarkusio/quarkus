package io.quarkus.aesh.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Marker build item indicating that a remote transport (SSH or WebSocket) is available.
 * <p>
 * Produced by sub-extension deployment processors (e.g. {@code AeshSshProcessor},
 * {@code AeshWebSocketProcessor}) to signal that CLI access is available over a
 * remote channel. When present, the core {@link AeshProcessor} will skip starting
 * the local console by default.
 */
public final class AeshRemoteTransportBuildItem extends MultiBuildItem {

    private final String name;

    public AeshRemoteTransportBuildItem(String name) {
        this.name = name;
    }

    /**
     * The transport name, e.g. "ssh" or "websocket".
     */
    public String getName() {
        return name;
    }
}
