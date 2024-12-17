package io.quarkus.websockets.next.test.pingpong;

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
import io.quarkus.websockets.next.OnPongMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketConnector;
import io.vertx.core.buffer.Buffer;

public class ClientAutoPingIntervalTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(ServerEndpoint.class, ClientEndpoint.class);
            }).overrideConfigKey("quarkus.websockets-next.client.auto-ping-interval", "200ms");

    @TestHTTPResource("/")
    URI uri;

    @Inject
    WebSocketConnector<ClientEndpoint> connector;

    @Test
    public void testPingPong() throws InterruptedException, ExecutionException {
        connector.baseUri(uri.toString()).connectAndAwait();
        // Ping messages are sent automatically
        assertTrue(ClientEndpoint.PONG.await(5, TimeUnit.SECONDS));
    }

    @WebSocket(path = "/end")
    public static class ServerEndpoint {

        @OnOpen
        void open() {
        }

    }

    @WebSocketClient(path = "/end")
    public static class ClientEndpoint {

        static final CountDownLatch PONG = new CountDownLatch(3);

        @OnPongMessage
        void pong(Buffer data) {
            PONG.countDown();
        }

    }

}
