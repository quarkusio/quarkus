package io.quarkus.websockets.next.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.vertx.core.Context;

@WebSocket(path = "/echo-blocking-await")
public class EchoBlockingAndAwait {

    @Inject
    EchoService echoService;

    @Inject
    WebSocketConnection connection;

    @OnTextMessage
    void echo(String msg) {
        assertTrue(Context.isOnWorkerThread());
        connection.sendTextAndAwait(echoService.echo(msg));
    }

}
