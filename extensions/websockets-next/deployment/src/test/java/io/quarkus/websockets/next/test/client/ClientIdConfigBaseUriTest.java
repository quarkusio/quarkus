package io.quarkus.websockets.next.test.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketConnector;

public class ClientIdConfigBaseUriTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ServerEndpoint.class, ClientEndpoint.class);
            });

    @TestHTTPResource("/")
    URI uri;

    @Inject
    WebSocketConnector<ClientEndpoint> connector;

    @Test
    public void testConfiguredBaseUri() throws InterruptedException, ExecutionException {
        String key = "c1.base-uri";
        String prev = System.getProperty(key);
        System.setProperty(key, uri.toString());
        try {
            // No need to pass baseUri
            connector.connectAndAwait();
            assertTrue(ServerEndpoint.OPEN_LATCH.await(5, TimeUnit.SECONDS));
            assertTrue(ClientEndpoint.OPEN_LATCH.await(5, TimeUnit.SECONDS));
        } finally {
            if (prev != null) {
                System.setProperty(key, prev);
            }
        }
    }

    @WebSocket(path = "/end")
    public static class ServerEndpoint {

        static final CountDownLatch OPEN_LATCH = new CountDownLatch(1);

        @OnOpen
        void open() {
            OPEN_LATCH.countDown();
        }

    }

    @WebSocketClient(path = "/end", clientId = "c1")
    public static class ClientEndpoint {

        static final CountDownLatch OPEN_LATCH = new CountDownLatch(1);

        @OnOpen
        void open() {
            OPEN_LATCH.countDown();
        }

    }

}
