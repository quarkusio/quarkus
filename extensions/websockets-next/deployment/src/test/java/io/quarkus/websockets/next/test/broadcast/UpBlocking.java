package io.quarkus.websockets.next.test.broadcast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.vertx.core.Context;

@WebSocket(path = "/up-blocking/{client}")
public class UpBlocking {

    @Inject
    WebSocketConnection connection;

    @OnTextMessage(broadcast = true)
    String echo(String msg) {
        assertTrue(Context.isOnWorkerThread());
        assertEquals(2, connection.getOpenConnections().size());
        return connection.pathParam("client") + ":" + msg.toUpperCase();
    }

}
