package io.quarkus.websockets.next.test.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OpenClientConnections;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnector;

public class OpenClientConnectionsTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ServerEndpoint.class, ClientEndpoint.class);
            });

    @Inject
    OpenClientConnections connections;

    @Inject
    WebSocketConnector<ClientEndpoint> connector;

    @Inject
    BasicWebSocketConnector basicConnector;

    @TestHTTPResource("/")
    URI uri;

    @Test
    void testClient() throws InterruptedException {
        for (WebSocketClientConnection c : connections) {
            fail("No connection should be found: " + c);
        }

        WebSocketClientConnection connection1 = connector
                .baseUri(uri)
                .addHeader("X-Test", "foo")
                .connectAndAwait();

        WebSocketClientConnection connection2 = connector
                .baseUri(uri)
                .addHeader("X-Test", "bar")
                .connectAndAwait();

        CountDownLatch CONN3_OPEN_LATCH = new CountDownLatch(1);
        WebSocketClientConnection connection3 = basicConnector
                .baseUri(uri)
                .onOpen(c -> CONN3_OPEN_LATCH.countDown())
                .path("end")
                .connectAndAwait();

        assertTrue(ServerEndpoint.OPEN_LATCH.await(5, TimeUnit.SECONDS));
        assertTrue(ClientEndpoint.OPEN_LATCH.await(5, TimeUnit.SECONDS));

        assertNotNull(connections.findByConnectionId(connection1.id()));
        assertNotNull(connections.findByConnectionId(connection2.id()));
        assertNotNull(connections.findByConnectionId(connection3.id()));
        assertEquals(3, connections.listAll().size());
        assertEquals(2, connections.findByClientId("client").size());
        assertEquals(1, connections.findByClientId(BasicWebSocketConnector.class.getName()).size());

        connection2.closeAndAwait();
        assertTrue(ClientEndpoint.CLOSED_LATCH.await(5, TimeUnit.SECONDS));
        assertEquals(2, connections.stream().toList().size());
    }

    @WebSocket(path = "/end")
    public static class ServerEndpoint {

        static final CountDownLatch OPEN_LATCH = new CountDownLatch(2);

        @OnOpen
        void open(HandshakeRequest handshakeRequest) {
            if (handshakeRequest.header("X-Test") != null) {
                OPEN_LATCH.countDown();
            }
        }

    }

    @WebSocketClient(path = "/end", clientId = "client")
    public static class ClientEndpoint {

        static final CountDownLatch OPEN_LATCH = new CountDownLatch(2);
        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @OnOpen
        void open(HandshakeRequest handshakeRequest) {
            if (handshakeRequest.header("X-Test") != null) {
                OPEN_LATCH.countDown();
            }
        }

        @OnClose
        void close() {
            CLOSED_LATCH.countDown();
        }

    }

}
