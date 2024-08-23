package io.quarkus.websockets.next.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.vertx.core.Context;

@WebSocket(path = "/echo-blocking")
public class EchoBlocking {

    @Inject
    EchoService echoService;

    @OnTextMessage
    String echo(String msg) {
        assertTrue(Context.isOnWorkerThread());
        return echoService.echo(msg);
    }

}
