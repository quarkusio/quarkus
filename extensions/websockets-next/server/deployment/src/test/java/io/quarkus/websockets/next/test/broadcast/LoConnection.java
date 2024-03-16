package io.quarkus.websockets.next.test.broadcast;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;

@WebSocket(path = "/lo-connection/{client}")
public class LoConnection {

    @Inject
    WebSocketConnection connection;

    @OnOpen
    void open() {
        // Send the message only to the current connection
        // This does not make much sense but it's good enough to test the filter
        connection.broadcast()
                .filter(c -> connection.id().equals(c.id()))
                .sendTextAndAwait(connection.pathParam("client").toLowerCase());
    }

}
