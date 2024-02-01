package io.quarkus.websockets.next.test.broadcast;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketServerConnection;
import io.vertx.core.Context;

@WebSocket("/lo-blocking/{client}")
public class LoBlocking {

    @Inject
    WebSocketServerConnection connection;

    @OnOpen(broadcast = true)
    String open() {
        assertTrue(Context.isOnWorkerThread());
        return connection.pathParam("client").toLowerCase();
    }

}
