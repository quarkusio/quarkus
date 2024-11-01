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

}
