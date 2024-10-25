package io.quarkus.websockets.next.runtime.telemetry;

public interface TelemetryConstants {

    /**
     * OpenTelemetry attributes added to spans created for opened and closed connections.
     */
    String CONNECTION_ID_ATTR_KEY = "connection.id";
    String CONNECTION_ENDPOINT_ATTR_KEY = "connection.endpoint.id";
    String CONNECTION_CLIENT_ATTR_KEY = "connection.client.id";

}
