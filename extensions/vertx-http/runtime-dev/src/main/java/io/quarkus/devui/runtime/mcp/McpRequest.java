package io.quarkus.devui.runtime.mcp;

import io.vertx.core.json.JsonObject;

interface McpRequest {

    String serverName();

    Object json();

    McpConnectionBase connection();

    Sender sender();

    ContextSupport contextSupport();

    default void messageReceived(JsonObject message) {
        if (connection().trafficLogger() != null) {
            connection().trafficLogger().messageReceived(message, connection());
        }
    }

    default void messageSent(JsonObject message) {
        if (connection().trafficLogger() != null) {
            connection().trafficLogger().messageSent(message, connection());
        }
    }

    void contextStart();

    void contextEnd();
}
