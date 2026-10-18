package io.quarkus.aesh.deployment;

import java.util.Objects;

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

    /**
     * Creates a remote transport build item with no endpoint path.
     *
     * @param name the transport name (e.g. "ssh"), must not be {@code null}
     */
    public AeshRemoteTransportBuildItem(String name) {
        this(name, null);
    }

    /**
     * Creates a remote transport build item.
     *
     * @param name the transport name (e.g. "ssh" or "websocket"), must not be {@code null}
     * @param path the endpoint path (e.g. "/aesh/terminal"), or {@code null} if the
     *        transport does not expose an HTTP endpoint path
     */
    public AeshRemoteTransportBuildItem(String name, String path) {
        this.name = Objects.requireNonNull(name, "name must not be null");
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
     * Returns {@code null} if the transport does not expose an endpoint path.
     */
    public String getPath() {
        return path;
    }
}
