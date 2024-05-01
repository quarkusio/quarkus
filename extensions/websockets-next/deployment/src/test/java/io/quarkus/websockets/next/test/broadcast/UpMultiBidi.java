package io.quarkus.websockets.next.test.broadcast;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Context;

@WebSocket(path = "/up-multi-bidi/{client}")
public class UpMultiBidi {

    @Inject
    WebSocketConnection connection;

    // Keep in mind that this callback is invoked eagerly immediately after @OnOpen - due to consumed Multi
    // That's why we cannot assert the number of open connections inside the callback
    @OnTextMessage(broadcast = true)
    Multi<String> echo(Multi<String> multi) {
        assertTrue(Context.isOnEventLoopThread());
        return multi.map(m -> connection.pathParam("client") + ":" + m.toUpperCase());
    }

}
