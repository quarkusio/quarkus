package io.quarkus.websockets.next.test.broadcast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

@WebSocket(path = "/up/{client}")
public class Up {

    @Inject
    WebSocketConnection connection;

    @OnTextMessage(broadcast = true)
    Uni<String> echo(String msg) {
        assertTrue(Context.isOnEventLoopThread());
        assertEquals(2, connection.getOpenConnections().size());
        return Uni.createFrom().item(connection.pathParam("client") + ":" + msg.toUpperCase());
    }

}
