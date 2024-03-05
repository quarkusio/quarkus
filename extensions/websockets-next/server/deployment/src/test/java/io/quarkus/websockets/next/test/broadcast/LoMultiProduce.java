package io.quarkus.websockets.next.test.broadcast;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Context;

@WebSocket(path = "/lo-multi-produce/{client}")
public class LoMultiProduce {

    @Inject
    WebSocketConnection connection;

    @OnOpen(broadcast = true)
    Multi<String> open() {
        assertTrue(Context.isOnEventLoopThread());
        return Multi.createFrom().item(connection.pathParam("client").toLowerCase());
    }

}
