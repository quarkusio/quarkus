package io.quarkus.websockets.next.test.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.Connection;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnPingMessage;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.WebSocketConnector;
import io.vertx.core.buffer.Buffer;

public class ClientEndpointTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ServerEndpoint.class, ClientEndpoint.class);
            });

    @Inject
    WebSocketConnector<ClientEndpoint> connector;

    @TestHTTPResource("/")
    URI uri;

    @Test
    void testClient() throws InterruptedException {
        Buffer ping = Buffer.buffer("ping");
        WebSocketClientConnection connection = connector
                .baseUri(uri)
                // The value will be encoded automatically
                .pathParam("name", "Lu=")
                .connectAndAwait();
        assertTrue(ClientEndpoint.OPEN_LATCH.await(5, TimeUnit.SECONDS));
        assertInstanceOf(WebSocketClientConnection.class, ClientEndpoint.CONNECTION.get());
        assertEquals("Lu=", connection.pathParam("name"));
        connection.sendPingAndAwait(ping);
        connection.sendTextAndAwait("Hi!");

        assertTrue(ServerEndpoint.PING_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(ClientEndpoint.PING_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(ClientEndpoint.MESSAGE_LATCH.await(5, TimeUnit.SECONDS));
        assertEquals("Lu=:Hello Lu=!", ClientEndpoint.MESSAGES.get(0));
        assertEquals("Lu=:Hi!", ClientEndpoint.MESSAGES.get(1));

        connection.closeAndAwait();
        assertTrue(ClientEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(ServerEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
    }

    @WebSocket(path = "/endpoint/{name}")
    public static class ServerEndpoint {

        private final Buffer ping = Buffer.buffer("ping");

        static final CountDownLatch PING_LATCH = new CountDownLatch(1);

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @Inject
        WebSocketConnection connection;

        @OnOpen
        String open(@PathParam String name) {
            connection.sendPingAndAwait(ping);
            return "Hello " + name + "!";
        }

        @OnPingMessage
        void onPing(Buffer message) {
            PING_LATCH.countDown();
        }

        @OnTextMessage
        String echo(String message) {
            return message;
        }

        @OnClose
        void close() {
            CLOSED_LATCH.countDown();
        }

    }

    @WebSocketClient(path = "/endpoint/{name}")
    public static class ClientEndpoint {

        static final CountDownLatch OPEN_LATCH = new CountDownLatch(1);

        static final AtomicReference<Connection> CONNECTION = new AtomicReference<>();

        static final CountDownLatch PING_LATCH = new CountDownLatch(1);

        static final CountDownLatch MESSAGE_LATCH = new CountDownLatch(2);

        static final List<String> MESSAGES = new CopyOnWriteArrayList<>();

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @OnOpen
        void onOpen(Connection connection) {
            CONNECTION.set(connection);
            OPEN_LATCH.countDown();
        }

        @OnTextMessage
        void onMessage(@PathParam String name, String message, WebSocketClientConnection connection) {
            if (!name.equals(connection.pathParam("name"))) {
                throw new IllegalArgumentException();
            }
            MESSAGES.add(name + ":" + message);
            MESSAGE_LATCH.countDown();
        }

        @OnPingMessage
        void onPing(Buffer message) {
            PING_LATCH.countDown();
        }

        @OnClose
        void close() {
            CLOSED_LATCH.countDown();
        }

    }

}
