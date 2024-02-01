package io.quarkus.websockets.next.test.broadcast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketServerConnection;
import io.vertx.core.Context;

@WebSocket("/up-blocking/{client}")
public class UpBlocking {

    @Inject
    WebSocketServerConnection connection;

    @OnMessage(broadcast = true)
    String echo(String msg) {
        assertTrue(Context.isOnWorkerThread());
        assertEquals(2, connection.getOpenConnections().size());
        return connection.pathParam("client") + ":" + msg.toUpperCase();
    }

}
