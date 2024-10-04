package io.quarkus.websockets.next.runtime.telemetry;

public interface TelemetryConstants {

    /**
     * OpenTelemetry name for Spans created for opened client WebSocket connections.
     * Metric key for opened server WebSocket connections.
     */
    String CLIENT_CONNECTION_OPENED = "quarkus.websockets.client.connection.opened";

    /**
     * OpenTelemetry name for Spans created for opened server WebSocket connections.
     * Metric key for opened server WebSocket connections.
     */
    String SERVER_CONNECTION_OPENED = "quarkus.websockets.server.connection.opened";

    /**
     * OpenTelemetry name for Spans created when opening of a client WebSocket connection fails.
     */
    String CLIENT_CONNECTION_OPENED_ERROR = "quarkus.websockets.client.connection.opened.error";

    /**
     * OpenTelemetry name for Spans created when opening of a server WebSocket connection fails.
     */
    String SERVER_CONNECTION_OPENED_ERROR = "quarkus.websockets.server.connection.opened.error";

    /**
     * OpenTelemetry name for Spans created for closed client WebSocket connections.
     * Metric key for closed client WebSocket connections.
     */
    String CLIENT_CONNECTION_CLOSED = "quarkus.websockets.client.connection.closed";

    /**
     * OpenTelemetry name for Spans created for closed server WebSocket connections.
     * Metric key for closed server WebSocket connections.
     */
    String SERVER_CONNECTION_CLOSED = "quarkus.websockets.server.connection.closed";

    /**
     * OpenTelemetry attribute added to {@link #SERVER_CONNECTION_OPENED_ERROR} or {@link #CLIENT_CONNECTION_OPENED_ERROR}.
     * It contains error messages of the failure that prevent a WebSocket connection from opening.
     */
    String CONNECTION_FAILURE_ATTR_KEY = "connection.failure";

    /**
     * OpenTelemetry attributes added to {@link #CLIENT_CONNECTION_CLOSED} or {@link #SERVER_CONNECTION_CLOSED} spans.
     */
    String CONNECTION_ID_ATTR_KEY = "connection.id";
    String CONNECTION_ENDPOINT_ATTR_KEY = "connection.endpoint.id";
    String CONNECTION_CLIENT_ATTR_KEY = "connection.client.id";

    /**
     * WebSocket endpoint path (with path params in it).
     * This attribute is added to created OpenTelemetry spans.
     * Micrometer metrics are tagged with URI as well.
     */
    String URI_ATTR_KEY = "uri";

    /**
     * Number of messages received by server endpoints.
     */
    String SERVER_MESSAGES_COUNT_RECEIVED = "quarkus.websockets.server.messages.count.received";

    /**
     * Counts all the WebSockets server endpoint errors.
     */
    String SERVER_MESSAGES_COUNT_ERRORS = "quarkus.websockets.server.messages.count.errors";

    /**
     * Number of bytes sent from server endpoints.
     */
    String SERVER_MESSAGES_COUNT_SENT_BYTES = "quarkus.websockets.server.messages.count.sent.bytes";

    /**
     * Number of bytes received by server endpoints.
     */
    String SERVER_MESSAGES_COUNT_RECEIVED_BYTES = "quarkus.websockets.server.messages.count.received.bytes";

    /**
     * Number of messages sent from client endpoints.
     */
    String CLIENT_MESSAGES_COUNT_SENT = "quarkus.websockets.client.messages.count.sent";

    /**
     * Counts all the WebSockets client endpoint errors.
     */
    String CLIENT_MESSAGES_COUNT_ERRORS = "quarkus.websockets.client.messages.count.errors";

    /**
     * Number of bytes sent from client endpoints.
     */
    String CLIENT_MESSAGES_COUNT_SENT_BYTES = "quarkus.websockets.client.messages.count.sent.bytes";

    /**
     * Number of bytes received by client endpoints.
     */
    String CLIENT_MESSAGES_COUNT_RECEIVED_BYTES = "quarkus.websockets.client.messages.count.received.bytes";

}
