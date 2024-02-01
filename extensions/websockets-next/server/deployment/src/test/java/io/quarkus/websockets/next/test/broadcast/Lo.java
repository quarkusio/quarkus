package io.quarkus.websockets.next.test.broadcast;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketServerConnection;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

@WebSocket("/lo/{client}")
public class Lo {

    @Inject
    WebSocketServerConnection connection;

    @OnOpen(broadcast = true)
    Uni<String> open() {
        assertTrue(Context.isOnEventLoopThread());
        return Uni.createFrom().item(connection.pathParam("client").toLowerCase());
    }

}
