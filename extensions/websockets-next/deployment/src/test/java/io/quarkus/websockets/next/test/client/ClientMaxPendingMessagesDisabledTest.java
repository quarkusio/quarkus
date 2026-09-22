package io.quarkus.websockets.next.test.client;

import static io.quarkus.websockets.next.InboundProcessingMode.CONCURRENT;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.WebSocketConnector;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

public class ClientMaxPendingMessagesDisabledTest {

    // Greater than the default quarkus.websockets-next.client.max-pending-messages (256)
    static final int COUNT = 300;

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Server.class, Client.class);
            }).overrideConfigKey("quarkus.websockets-next.client.max-pending-messages", "0");

    @Inject
    WebSocketConnector<Client> connector;

    @TestHTTPResource("/")
    URI uri;

    @Test
    void testBackpressureDisabledOnClient() throws InterruptedException {
        WebSocketClientConnection connection = connector.baseUri(uri).connectAndAwait();
        // The server sends more messages than the default limit; with back-pressure disabled the client receives and
        // parks all of them at once - if any bound was applied, in-flight would cap below COUNT and this would time out
        Awaitility.await().until(() -> Client.inFlight.get() == COUNT);
        // Release all parked messages and make sure they are all processed
        Client.releaseAll();
        assertTrue(Client.processed.await(5, TimeUnit.SECONDS), "All messages should be eventually processed");
        connection.closeAndAwait();
    }

    @WebSocket(path = "/end")
    public static class Server {

        @Inject
        WebSocketConnection connection;

        @OnOpen
        void open() {
            for (int i = 0; i < COUNT; i++) {
                connection.sendTextAndAwait("m" + i);
            }
        }

    }

    @WebSocketClient(path = "/end", inboundProcessingMode = CONCURRENT)
    public static class Client {

        static final AtomicInteger inFlight = new AtomicInteger();
        static final List<UniEmitter<? super Void>> emitters = new CopyOnWriteArrayList<>();
        static final CountDownLatch processed = new CountDownLatch(COUNT);

        static void releaseAll() {
            for (UniEmitter<? super Void> emitter : emitters) {
                emitter.complete(null);
            }
        }

        // Non-blocking handler - each message is parked via an emitter (no thread consumed) so the event loop keeps
        // reading and unprocessed messages accumulate when back-pressure is disabled
        @OnTextMessage
        Uni<Void> onMessage(String message) {
            return Uni.createFrom().<Void> emitter(emitter -> {
                emitters.add(emitter);
                inFlight.incrementAndGet();
            }).onTermination().invoke(processed::countDown);
        }

    }

}
