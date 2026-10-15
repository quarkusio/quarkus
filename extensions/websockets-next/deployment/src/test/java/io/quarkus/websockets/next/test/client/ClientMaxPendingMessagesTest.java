package io.quarkus.websockets.next.test.client;

import static io.quarkus.websockets.next.InboundProcessingMode.CONCURRENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.WebSocketConnector;

public class ClientMaxPendingMessagesTest {

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> {
                root.addClasses(Server.class, Client.class);
            }).overrideConfigKey("quarkus.websockets-next.client.max-pending-messages", "2");

    @Inject
    WebSocketConnector<Client> connector;

    @TestHTTPResource("/")
    URI uri;

    @Test
    void testInboundMessagesAreBoundedOnClient() throws InterruptedException {
        WebSocketClientConnection connection = connector.baseUri(uri).connectAndAwait();

        // The server sends four messages to the client on open - the client must not process more than the limit at once
        assertTrue(Client.twoStarted.await(5, TimeUnit.SECONDS), "Two messages should be processed concurrently");
        // Give a potential (incorrectly unbounded) third message a chance to start before we release the gate
        Thread.sleep(500);
        assertEquals(2, Client.inFlight.get(), "No more than max-pending-messages should be in flight");
        assertEquals(2, Client.peak.get(), "The peak number of in-flight messages must not exceed the limit");

        // Release the held messages - the remaining messages are fetched one at a time as the in-flight ones complete
        Client.releaseGate.countDown();
        assertTrue(Client.allProcessed.await(5, TimeUnit.SECONDS), "All messages should be eventually processed");

        connection.closeAndAwait();
    }

    @WebSocket(path = "/end")
    public static class Server {

        @Inject
        WebSocketConnection connection;

        @OnOpen
        void open() {
            for (int i = 1; i <= 4; i++) {
                connection.sendTextAndAwait("m" + i);
            }
        }

    }

    @WebSocketClient(path = "/end", inboundProcessingMode = CONCURRENT)
    public static class Client {

        static final AtomicInteger inFlight = new AtomicInteger();
        static final AtomicInteger peak = new AtomicInteger();
        static final CountDownLatch twoStarted = new CountDownLatch(2);
        static final CountDownLatch releaseGate = new CountDownLatch(1);
        static final CountDownLatch allProcessed = new CountDownLatch(4);

        @OnTextMessage
        void onMessage(String message) throws InterruptedException {
            int current = inFlight.incrementAndGet();
            peak.accumulateAndGet(current, Math::max);
            twoStarted.countDown();
            try {
                releaseGate.await(10, TimeUnit.SECONDS);
            } finally {
                inFlight.decrementAndGet();
                allProcessed.countDown();
            }
        }

    }

}
