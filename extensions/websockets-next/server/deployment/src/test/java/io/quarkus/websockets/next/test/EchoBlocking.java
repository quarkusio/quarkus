package io.quarkus.websockets.next.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnMessage;
import io.quarkus.websockets.next.WebSocket;
import io.vertx.core.Context;

@WebSocket("/echo-blocking")
public class EchoBlocking {

    @Inject
    EchoService echoService;

    @OnMessage
    String echo(String msg) {
        assertTrue(Context.isOnWorkerThread());
        return echoService.echo(msg);
    }

}
