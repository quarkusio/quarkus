package io.quarkus.websockets.next.test.broadcast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketServerConnection;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Context;

@WebSocket("/up-multi-bidi/{client}")
public class UpMultiBidi {

    @Inject
    WebSocketServerConnection connection;

    @OnMessage(broadcast = true)
    Multi<String> echo(Multi<String> multi) {
        assertTrue(Context.isOnEventLoopThread());
        assertEquals(2, connection.getOpenConnections().size());
        return multi.map(m -> connection.pathParam("client") + ":" + m.toUpperCase());
    }

}
