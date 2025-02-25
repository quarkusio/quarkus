package io.quarkus.websockets.next.runtime.telemetry;

public final class TelemetryConstants {

    private TelemetryConstants() {
        // class with constants
    }

    /**
     * OpenTelemetry attributes added to spans created for opened and closed connections.
     */
    public static final String CONNECTION_ID_ATTR_KEY = "connection.id";
    public static final String CONNECTION_ENDPOINT_ATTR_KEY = "connection.endpoint.id";
    public static final String CONNECTION_CLIENT_ATTR_KEY = "connection.client.id";

    /**
     * Counts all the WebSockets client opened connections.
     */
    public static final String CLIENT_CONNECTION_OPENED = "quarkus.websockets.client.connections.opened";

    /**
     * Counts all the WebSockets client opened connections.
     */
    public static final String SERVER_CONNECTION_OPENED = "quarkus.websockets.server.connections.opened";

    /**
     * Counts number of times that opening of a WebSocket server connection resulted in error.
     */
    public static final String SERVER_CONNECTION_OPENED_ERROR = "quarkus.websockets.server.connections.opened.errors";

    /**
     * Counts number of times that opening of a WebSocket client connection resulted in error.
     */
    public static final String CLIENT_CONNECTION_OPENED_ERROR = "quarkus.websockets.client.connections.opened.errors";

    /**
     * Counts all the WebSockets client closed connections.
     */
    public static final String CLIENT_CONNECTION_CLOSED = "quarkus.websockets.client.connections.closed";

    /**
     * Counts all the WebSockets client closed connections.
     */
    public static final String SERVER_CONNECTION_CLOSED = "quarkus.websockets.server.connections.closed";

    /**
     * Counts all the WebSockets server endpoint errors.
     */
    public static final String SERVER_ENDPOINT_COUNT_ERRORS = "quarkus.websockets.server.endpoint.count.errors";

    /**
     * Counts all the WebSockets client endpoint errors.
     */
    public static final String CLIENT_ENDPOINT_COUNT_ERRORS = "quarkus.websockets.client.endpoint.count.errors";

    /**
     * Number of messages sent and received by server endpoints.
     */
    public static final String SERVER_COUNT = "quarkus.websockets.server.count";
    /**
     * Number of bytes sent and received by server endpoints.
     */
    public static final String SERVER_BYTES = "quarkus.websockets.server.bytes";
    /**
     * Number of messages sent and received by client endpoints.
     */
    public static final String CLIENT_COUNT = "quarkus.websockets.client.count";
    /**
     * Number of bytes sent and received by client endpoints.
     */
    public static final String CLIENT_BYTES = "quarkus.websockets.client.bytes";

    /**
     * {@link Direction} tag key.
     */
    public static final String DIRECTION_TAG_KEY = "direction";

    /**
     * Direction added as a tag to following metrics:
     * <ul>
     * <li>{@link #SERVER_BYTES}</li>
     * <li>{@link #SERVER_COUNT}</li>
     * <li>{@link #CLIENT_BYTES}</li>
     * <li>{@link #CLIENT_COUNT}</li>
     * </ul>
     */
    public enum Direction {
        /**
         * The direction marking received messages.
         */
        INBOUND,
        /**
         * The direction marking sent messages.
         */
        OUTBOUND
    }
}
