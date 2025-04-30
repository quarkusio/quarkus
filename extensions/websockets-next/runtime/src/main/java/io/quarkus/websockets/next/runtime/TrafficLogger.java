package io.quarkus.websockets.next.runtime;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.logging.Logger;

import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.runtime.config.WebSocketsClientRuntimeConfig;
import io.quarkus.websockets.next.runtime.config.WebSocketsServerRuntimeConfig;
import io.vertx.core.buffer.Buffer;

class TrafficLogger {

    static TrafficLogger forClient(WebSocketsClientRuntimeConfig config) {
        return config.trafficLogging().enabled() ? new TrafficLogger(Type.CLIENT, config.trafficLogging().textPayloadLimit())
                : null;
    }

    static TrafficLogger forServer(WebSocketsServerRuntimeConfig config) {
        return config.trafficLogging().enabled() ? new TrafficLogger(Type.SERVER, config.trafficLogging().textPayloadLimit())
                : null;
    }

    private static final Logger LOG = Logger.getLogger("io.quarkus.websockets.next.traffic");

    private final Type type;

    private final int textPayloadLimit;

    private TrafficLogger(Type type, int textPayloadLimit) {
        this.type = type;
        this.textPayloadLimit = textPayloadLimit;
    }

    void connectionOpened(WebSocketConnectionBase connection) {
        if (LOG.isDebugEnabled()) {
            LOG.debugf("%s connection opened: %s, Connection[%s], Handshake headers[%s]",
                    typeToString(),
                    connection.handshakeRequest().path(),
                    connectionToString(connection),
                    headersToString(connection.handshakeRequest()));
        }
    }

    void textMessageReceived(WebSocketConnectionBase connection, String payload) {
        if (LOG.isDebugEnabled()) {
            LOG.debugf("%s received text message, Connection[%s], Payload: \n%s",
                    typeToString(),
                    connectionToString(connection),
                    payloadToString(payload));
        }
    }

    void textMessageSent(WebSocketConnectionBase connection, String payload) {
        if (LOG.isDebugEnabled()) {
            LOG.debugf("%s sent text message, Connection[%s], Payload: \n%s",
                    typeToString(),
                    connectionToString(connection),
                    payloadToString(payload));
        }
    }

    void binaryMessageReceived(WebSocketConnectionBase connection, Buffer payload) {
        if (LOG.isDebugEnabled()) {
            LOG.debugf("%s received binary message, Connection[%s], Payload[%s bytes]",
                    typeToString(),
                    connectionToString(connection),
                    payload.length());
        }
    }

    void binaryMessageSent(WebSocketConnectionBase connection, Buffer payload) {
        if (LOG.isDebugEnabled()) {
            LOG.debugf("%s sent binary message, Connection[%s], Payload[%s bytes]",
                    typeToString(),
                    connectionToString(connection),
                    payload.length());
        }
    }

    void connectionClosed(WebSocketConnectionBase connection) {
        if (LOG.isDebugEnabled()) {
            LOG.debugf("%s connection closed, Connection[%s], %s",
                    typeToString(),
                    connectionToString(connection),
                    connection.closeReason());
        }
    }

    private String payloadToString(String payload) {
        if (payload == null || payload.isBlank()) {
            return "n/a";
        } else if (textPayloadLimit < 0 || payload.length() <= textPayloadLimit) {
            return payload;
        } else {
            return payload.substring(0, payload.length()) + "...";
        }
    }

    private String headersToString(HandshakeRequest request) {
        Map<String, List<String>> headers = request.headers();
        if (headers.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Entry<String, List<String>> e : headers.entrySet()) {
            for (String value : e.getValue()) {
                builder.append(" ").append(e.getKey()).append("=").append(value);
            }
        }
        return builder.toString();
    }

    private String typeToString() {
        return type == Type.SERVER ? "[server]" : "[client]";
    }

    private String connectionToString(WebSocketConnectionBase connection) {
        StringBuilder builder = new StringBuilder();
        if (connection instanceof WebSocketConnection) {
            builder.append("endpointId=").append(((WebSocketConnection) connection).endpointId());
        } else {
            builder.append("clientId=").append(((WebSocketClientConnection) connection).clientId());
        }
        builder.append(", id=").append(connection.id());
        return builder.toString();
    }

    enum Type {
        SERVER,
        CLIENT
    }

}
