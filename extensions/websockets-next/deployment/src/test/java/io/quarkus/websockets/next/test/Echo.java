package io.quarkus.websockets.next.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

@WebSocket(path = "/echo")
public class Echo {

    @Inject
    EchoService echoService;

    @OnTextMessage
    Uni<String> echo(String msg) {
        assertTrue(Context.isOnEventLoopThread());
        return Uni.createFrom().item(echoService.echo(msg));
    }

}
