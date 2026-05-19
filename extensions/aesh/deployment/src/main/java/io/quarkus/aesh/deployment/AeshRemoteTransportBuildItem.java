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
    private final String path;

    public AeshRemoteTransportBuildItem(String name) {
        this(name, null);
    }

    public AeshRemoteTransportBuildItem(String name, String path) {
        this.name = name;
        this.path = path;
    }

    /**
     * The transport name, e.g. "ssh" or "websocket".
     */
    public String getName() {
        return name;
    }

    /**
     * The endpoint path, if applicable (e.g. the WebSocket endpoint path).
     */
    public String getPath() {
        return path;
    }
}
