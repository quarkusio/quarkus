package io.quarkus.websockets.next.test.client.programmatic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;

public class ClientEndpointProgrammaticTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ServerEndpoint.class, ClientEndpoint.class);
            });

    @Inject
    Instance<WebSocketConnector<ClientEndpoint>> connector;

    @TestHTTPResource("/")
    URI uri;

    @Test
    void testClient() throws InterruptedException {
        WebSocketClientConnection connection1 = connector
                .get()
                .baseUri(uri)
                .addHeader("Foo", "Lu")
                .connectAndAwait();
        connection1.sendTextAndAwait("Hi!");

        WebSocketClientConnection connection2 = connector
                .get()
                .baseUri(uri)
                .addHeader("Foo", "Ma")
                .connectAndAwait();
        connection2.sendTextAndAwait("Hi!");

        assertTrue(ClientEndpoint.MESSAGE_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(ClientEndpoint.MESSAGES.contains("Lu:Hello Lu!"));
        assertTrue(ClientEndpoint.MESSAGES.contains("Lu:Hi!"));
        assertTrue(ClientEndpoint.MESSAGES.contains("Ma:Hello Ma!"));
        assertTrue(ClientEndpoint.MESSAGES.contains("Ma:Hi!"), ClientEndpoint.MESSAGES.toString());

        connection1.closeAndAwait();
        connection2.closeAndAwait();
        assertTrue(ClientEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(ServerEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
    }

    @WebSocket(path = "/endpoint")
    public static class ServerEndpoint {

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(2);

        @OnOpen
        String open(HandshakeRequest handshakeRequest) {
            return "Hello " + handshakeRequest.header("Foo") + "!";
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

    @WebSocketClient(path = "/endpoint")
    public static class ClientEndpoint {

        static final CountDownLatch MESSAGE_LATCH = new CountDownLatch(4);

        static final List<String> MESSAGES = new CopyOnWriteArrayList<>();

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(2);

        @OnTextMessage
        void onMessage(String message, HandshakeRequest handshakeRequest) {
            MESSAGES.add(handshakeRequest.header("Foo") + ":" + message);
            MESSAGE_LATCH.countDown();
        }

        @OnClose
        void close() {
            CLOSED_LATCH.countDown();
        }

    }

}
