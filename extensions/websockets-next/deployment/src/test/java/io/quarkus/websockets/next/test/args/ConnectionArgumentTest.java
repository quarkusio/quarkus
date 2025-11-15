package io.quarkus.websockets.next.test.args;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.net.URI;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.Connection;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.JsonObject;

public class ConnectionArgumentTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Echo.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo")
    URI testUri;

    @Test
    void testArgument() {
        String message = "ok";
        String header = "fool";
        try (WSClient client = WSClient.create(vertx).connect(new WebSocketConnectOptions().addHeader("X-Test", header),
                testUri)) {
            JsonObject reply = client.sendAndAwaitReply(message).toJsonObject();
            assertEquals(header, reply.getString("header"), reply.toString());
            assertEquals(message, reply.getString("message"), reply.toString());
        }
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        @Inject
        WebSocketConnection c;

        @OnOpen
        void connect(Connection connection) {
            assertInstanceOf(WebSocketConnection.class, connection);
        }

        @OnTextMessage
        Uni<Void> process(WebSocketConnection connection, String message) throws InterruptedException {
            assertEquals(c.id(), connection.id());
            return connection.sendText(
                    new JsonObject()
                            .put("id", connection.id())
                            .put("message", message)
                            .put("header", connection.handshakeRequest().header("X-Test")));
        }

    }

}
