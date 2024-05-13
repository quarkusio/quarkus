package io.quarkus.websockets.next.test;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@WebSocket(path = "/echo-multi-consume")
public class EchoMultiConsume {

    @Inject
    WebSocketConnection connection;

    @OnTextMessage
    Uni<Void> echo(Multi<String> multi) {
        multi.subscribe().with(msg -> {
            connection.sendText(msg).subscribe().with(v -> {
            });
        });
        return connection.sendText("subscribed");
    }

}
