package io.quarkus.websockets.next.test;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketServerConnection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@WebSocket(path = "/echo-multi-consume")
public class EchoMultiConsume {

    @Inject
    WebSocketServerConnection connection;

    @OnMessage
    Uni<Void> echo(Multi<String> multi) {
        multi.subscribe().with(msg -> {
            connection.sendText(msg).subscribe().with(v -> {
            });
        });
        return connection.sendText("subscribed");
    }

}
