package io.quarkus.websockets.next.test.telemetry.endpoints.ontextmessage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;

@WebSocket(path = "/bounce", endpointId = "bounce-server-endpoint-id")
public class BounceEndpoint {

    public static final List<String> MESSAGES = new CopyOnWriteArrayList<>();
    public static CountDownLatch CLOSED_LATCH = new CountDownLatch(1);
    public static volatile String connectionId = null;
    public static volatile String endpointId = null;

    @ConfigProperty(name = "bounce-endpoint.prefix-responses", defaultValue = "false")
    boolean prefixResponses;

    @OnTextMessage
    public String onMessage(String message) {
        if (prefixResponses) {
            message = "echo 0: " + message;
        }
        MESSAGES.add(message);
        if (message.equals("throw-exception")) {
            throw new RuntimeException("Failing 'onMessage' to test behavior when an exception was thrown");
        }
        return message;
    }

    @OnOpen
    void open(WebSocketConnection connection) {
        connectionId = connection.id();
        endpointId = connection.endpointId();
    }

    @OnClose
    void onClose() {
        CLOSED_LATCH.countDown();
    }

}
