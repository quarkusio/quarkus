package io.quarkus.websockets.next.test.subprotocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.WebSocketConnector;

public class SubprotocolFromConnectionTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class, Client.class);
            }).overrideConfigKey("quarkus.websockets-next.server.supported-subprotocols", "oak,larch");

    @TestHTTPResource("/")
    URI endpointUri;

    @Inject
    WebSocketConnector<Client> connector;

    @Test
    void testSubprotocolFromConnection() throws InterruptedException, ExecutionException {
        var connection = connector.baseUri(endpointUri).addSubprotocol("larch").connectAndAwait();
        assertEquals("larch", connection.subprotocol());
        assertTrue(Client.OPEN_LATCH.await(5, TimeUnit.SECONDS));
        assertEquals("larch", Client.SUB_PROTOCOL.get());
        assertTrue(Endpoint.OPEN_LATCH.await(5, TimeUnit.SECONDS));
        assertEquals("larch", Endpoint.SUB_PROTOCOL.get());
        connection.closeAndAwait();
    }

    @WebSocket(path = "/endpoint")
    public static class Endpoint {

        static final CountDownLatch OPEN_LATCH = new CountDownLatch(1);

        static final AtomicReference<String> SUB_PROTOCOL = new AtomicReference<>();

        @OnOpen
        void connected(WebSocketConnection connection) {
            SUB_PROTOCOL.set(connection.subprotocol());
            OPEN_LATCH.countDown();
        }
    }

    @WebSocketClient(path = "/endpoint")
    public static class Client {

        static final CountDownLatch OPEN_LATCH = new CountDownLatch(1);

        static final AtomicReference<String> SUB_PROTOCOL = new AtomicReference<>();

        @OnOpen
        void connected(WebSocketClientConnection connection) {
            SUB_PROTOCOL.set(connection.subprotocol());
            OPEN_LATCH.countDown();
        }
    }

}
